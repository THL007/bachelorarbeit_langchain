# Bachelorarbeit LangChain – COBOL migration app
FROM python:3.12-slim

WORKDIR /app

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Application code
COPY main.py .
COPY workspace ./workspace

# Chroma and workspace data are mounted at runtime
ENV PYTHONUNBUFFERED=1

CMD ["python", "main.py"]
