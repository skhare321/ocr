FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY server_nanonets.py .

EXPOSE 80

CMD ["uvicorn", "server_nanonets:app", "--host", "0.0.0.0", "--port", "80"]
