FROM python:3.9-slim

# 1) system libs for torch & pillow
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
      libgomp1 libgl1-mesa-glx libglib2.0-0 \
      libsm6 libxext6 libxrender1 libfontconfig1 && \
    rm -rf /var/lib/apt/lists/*

# 2) python deps
RUN pip install --no-cache-dir \
      torch torchvision \
      transformers==4.44.2 \
      fastapi uvicorn \
      pillow python-multipart

WORKDIR /app
COPY server_nanonets.py /app/

EXPOSE 8000
CMD ["uvicorn", "server_nanonets:app", "--host", "0.0.0.0", "--port", "8000"]

