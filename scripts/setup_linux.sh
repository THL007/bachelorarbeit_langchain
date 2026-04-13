#!/usr/bin/env bash
#
# One-shot Linux setup for bachelorarbeit_langchain:
#   - Python 3.12 + venv + pip dependencies
#   - OpenJDK 17
#   - GnuCOBOL 3.2.0 (from source; set SKIP_GNUCOBOL_SOURCE=1 to use distro package only)
#   - Docker + Neo4j (docker compose) started in the background
#   - Build tools + tree-sitter CLI + compile tree-sitter-cobol → cobol.so
#
# Usage (from anywhere):
#   curl -fsSL ... > setup_linux.sh   # or clone the repo first
#   chmod +x scripts/setup_linux.sh
#   cd /path/to/bachelorarbeit_langchain
#   ./scripts/setup_linux.sh
#
# Environment:
#   SKIP_GNUCOBOL_SOURCE=1   Skip building GnuCOBOL 3.2 from tarball; install distro gnucobol only
#   SKIP_DOCKER=1            Do not install/start Docker or Neo4j
#   SKIP_NEO4J_START=1       Install Docker but do not run docker compose up
#

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

log() { printf '\n[%s] %s\n' "$(date -Iseconds)" "$*"; }
die() { echo "ERROR: $*" >&2; exit 1; }

need_cmd() { command -v "$1" >/dev/null 2>&1; }

if [[ "$(uname -s)" != "Linux" ]]; then
  die "This script is intended for Linux. On macOS use Homebrew equivalents (see README)."
fi

if [[ "${EUID:-}" -eq 0 ]]; then
  die "Do not run this script as root. It will use sudo where needed."
fi

if ! need_cmd sudo; then
  die "sudo is required."
fi

# --- detect distro (Debian/Ubuntu family) ---
if [[ -f /etc/os-release ]]; then
  # shellcheck source=/dev/null
  . /etc/os-release
  DISTRO_ID="${ID:-}"
  DISTRO_VERSION_ID="${VERSION_ID:-}"
else
  die "Cannot detect distribution (missing /etc/os-release)."
fi

is_debian_like() {
  case "${DISTRO_ID}" in
    debian|ubuntu|linuxmint|pop) return 0 ;;
    *) return 1 ;;
  esac
}

if ! is_debian_like; then
  die "Only Debian/Ubuntu-based distros are automated here. Adapt package names for ${DISTRO_ID:-unknown}."
fi

APT_UPDATE_RAN=0
apt_update() {
  if [[ "$APT_UPDATE_RAN" -eq 0 ]]; then
    sudo apt-get update -y
    APT_UPDATE_RAN=1
  fi
}

apt_install() {
  apt_update
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends "$@"
}

log "Installing base packages (build tools, SSL, VCS, ripgrep)…"
apt_install \
  ca-certificates \
  curl \
  git \
  gnupg \
  software-properties-common \
  build-essential \
  pkg-config \
  libssl-dev \
  libffi-dev \
  zlib1g-dev \
  libbz2-dev \
  libreadline-dev \
  libsqlite3-dev \
  xz-utils \
  wget \
  ripgrep

# --- Python 3.12 ---
install_python312() {
  if need_cmd python3.12; then
    log "python3.12 already on PATH."
    return
  fi
  log "Installing Python 3.12…"
  if apt-cache show python3.12 &>/dev/null; then
    apt_install python3.12 python3.12-venv python3.12-dev
  else
    log "Adding deadsnakes PPA for Python 3.12…"
    sudo add-apt-repository -y ppa:deadsnakes/ppa
    APT_UPDATE_RAN=0
    apt_install python3.12 python3.12-venv python3.12-dev
  fi
}

install_python312
python3.12 --version

# --- OpenJDK 17 ---
install_java17() {
  if need_cmd java && java -version 2>&1 | grep -q '17\.'; then
    log "Java 17 already default or available."
    return
  fi
  log "Installing OpenJDK 17…"
  if apt-cache show openjdk-17-jdk &>/dev/null; then
    apt_install openjdk-17-jdk
  else
    die "openjdk-17-jdk not found in APT. Install JDK 17 manually."
  fi
  if need_cmd update-alternatives; then
    sudo update-alternatives --set java /usr/lib/jvm/java-17-openjdk-*/bin/java 2>/dev/null || true
  fi
}

install_java17
java -version

# --- GnuCOBOL 3.2.0 ---
GNUCOBOL_PREFIX="${GNUCOBOL_PREFIX:-/usr/local}"
GNUCOBOL_TARBALL_URL="https://ftp.gnu.org/gnu/gnucobol/gnucobol-3.2.tar.xz"

install_gnucobol_from_source() {
  log "Building GnuCOBOL 3.2.0 from source (installs under ${GNUCOBOL_PREFIX})…"
  apt_install \
    libgmp-dev \
    libncurses-dev \
    libdb-dev \
    gettext \
    flex \
    bison \
    help2man \
    texinfo

  local build_dir
  build_dir="$(mktemp -d)"
  trap 'rm -rf "$build_dir"' RETURN
  cd "$build_dir"
  wget -q "$GNUCOBOL_TARBALL_URL" -O gnucobol-3.2.tar.xz
  tar xf gnucobol-3.2.tar.xz
  cd gnucobol-3.2
  ./configure --prefix="$GNUCOBOL_PREFIX"
  make -j"$(nproc)"
  sudo make install
  sudo ldconfig
  cd "$REPO_ROOT"
  hash -r
  "${GNUCOBOL_PREFIX}/bin/cobc" --version
}

install_gnucobol_distro() {
  log "Installing distribution GnuCOBOL package (version may differ from 3.2.0)…"
  apt_install gnucobol || apt_install open-cobol
  cobc --version
}

if [[ "${SKIP_GNUCOBOL_SOURCE:-}" == "1" ]]; then
  install_gnucobol_distro
else
  if need_cmd cobc && cobc --version 2>&1 | grep -qE '3\.2(\.|$)'; then
    log "GnuCOBOL 3.2.x already detected."
    cobc --version
  else
    install_gnucobol_from_source
    case ":${PATH:-}:" in
      *":${GNUCOBOL_PREFIX}/bin:"*) ;;
      *)
        log "Add to your shell profile: export PATH=\"${GNUCOBOL_PREFIX}/bin:\$PATH\""
        export PATH="${GNUCOBOL_PREFIX}/bin:${PATH}"
        ;;
    esac
  fi
fi

# --- Docker + Neo4j ---
# Prefer Docker Compose v2 (`docker compose`); many Debian images lack docker-compose-plugin in default APT.
compose_up_neo4j() {
  local dcfile="$REPO_ROOT/docker-compose.yml"
  if sudo docker compose version &>/dev/null; then
    sudo docker compose -f "$dcfile" up -d neo4j \
      || docker compose -f "$dcfile" up -d neo4j
  elif need_cmd docker-compose; then
    sudo docker-compose -f "$dcfile" up -d neo4j \
      || docker-compose -f "$dcfile" up -d neo4j
  else
    die "Docker is installed but neither 'docker compose' nor docker-compose is available. Install docker-compose-plugin or docker-compose from your distro."
  fi
}

install_docker() {
  if need_cmd docker; then
    log "docker already installed."
    return
  fi
  log "Installing Docker Engine (docker.io)…"
  apt_install docker.io

  log "Installing Compose (plugin preferred, else standalone docker-compose)…"
  if apt-cache show docker-compose-plugin &>/dev/null; then
    apt_install docker-compose-plugin
  elif apt-cache show docker-compose-v2 &>/dev/null; then
    apt_install docker-compose-v2
  elif apt-cache show docker-compose &>/dev/null; then
    log "docker-compose-plugin not in APT; using standalone docker-compose."
    apt_install docker-compose
  else
    die "No compose package found (tried docker-compose-plugin, docker-compose-v2, docker-compose). Add Docker's APT repo or install compose manually."
  fi

  sudo systemctl enable docker --now 2>/dev/null || sudo service docker start 2>/dev/null || true
  if ! groups "$USER" | grep -q '\bdocker\b'; then
    log "Adding user $USER to group docker (log out/in to apply)…"
    sudo usermod -aG docker "$USER" || true
  fi
}

start_neo4j() {
  log "Starting Neo4j via docker compose (project: neo4j service)…"
  if [[ ! -f "$REPO_ROOT/docker-compose.yml" ]]; then
    die "docker-compose.yml not found in $REPO_ROOT"
  fi
  if ! need_cmd docker; then
    die "docker not found after install."
  fi
  compose_up_neo4j
  log "Neo4j Bolt: neo4j://localhost:7687  Browser: http://localhost:7474"
}

if [[ "${SKIP_DOCKER:-}" == "1" ]]; then
  log "SKIP_DOCKER=1: skipping Docker and Neo4j."
else
  install_docker
  if [[ "${SKIP_NEO4J_START:-}" == "1" ]]; then
    log "SKIP_NEO4J_START=1: Docker installed but Neo4j not started."
  else
    start_neo4j
  fi
fi

# --- Node + tree-sitter CLI (for cobol.so) ---
install_tree_sitter_cli() {
  if need_cmd tree-sitter; then
    log "tree-sitter CLI already on PATH."
    tree-sitter --version
    return
  fi
  log "Installing Node.js (for tree-sitter CLI; needs Node 18+)…"
  local need_node_repo=0
  if need_cmd node; then
    major="$(node -p 'process.versions.node.split(".")[0]' 2>/dev/null || echo 0)"
    if [[ "${major:-0}" -lt 18 ]]; then
      need_node_repo=1
    fi
  else
    need_node_repo=1
  fi
  if [[ "$need_node_repo" -eq 1 ]]; then
    curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
    APT_UPDATE_RAN=0
    apt_install nodejs
  fi
  node --version
  log "Installing tree-sitter-cli globally…"
  sudo npm install -g tree-sitter-cli
  tree-sitter --version
}

build_cobol_grammar() {
  local gdir="$REPO_ROOT/tree-sitter-cobol"
  [[ -d "$gdir" ]] || die "Missing $gdir"
  log "Generating and building tree-sitter COBOL parser → cobol.so …"
  (
    cd "$gdir"
    tree-sitter generate
    tree-sitter build -o cobol.so
  )
  [[ -f "$gdir/cobol.so" ]] || die "cobol.so not produced in $gdir"
  log "Parser library: $gdir/cobol.so"
}

install_tree_sitter_cli
build_cobol_grammar

# --- Python venv + requirements ---
VENV_DIR="${VENV_DIR:-$REPO_ROOT/.venv}"
log "Creating virtualenv at $VENV_DIR …"
python3.12 -m venv "$VENV_DIR"
# shellcheck source=/dev/null
source "$VENV_DIR/bin/activate"
python -m pip install --upgrade pip wheel
pip install -r "$REPO_ROOT/requirements.txt"

if [[ ! -f "$REPO_ROOT/.env" && -f "$REPO_ROOT/.env.example" ]]; then
  log "Creating .env from .env.example — set OPENAI_API_KEY."
  cp "$REPO_ROOT/.env.example" "$REPO_ROOT/.env"
fi

log "Done.

Next steps:
  1. Edit $REPO_ROOT/.env — set OPENAI_API_KEY (and Neo4j password if you changed it).
  2. Activate venv:  source $VENV_DIR/bin/activate
  3. Run migration:    python main.py
  4. Neo4j UI:         http://localhost:7474  (Bolt neo4j://localhost:7687)

If Docker required a new group membership, log out and back in, then:
  docker compose -f $REPO_ROOT/docker-compose.yml up -d neo4j
"
