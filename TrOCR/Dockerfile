FROM python:3.10-slim

WORKDIR /app

# Install system packages
RUN apt-get update && apt-get install -y \
    libglib2.0-0 libsm6 libxext6 libxrender-dev git && \
    rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --upgrade pip && pip install -r requirements.txt

COPY app/ ./app/
WORKDIR /app/app
CMD ["python", "api.py"]


EXPOSE 8555
