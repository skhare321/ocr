
FROM paddlepaddle/paddle:3.1.0

USER root
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
       libgomp1 \
       libgl1-mesa-glx \
       libglib2.0-0 \
       libsm6 \
       libxext6 \
       libxrender1 \
       libfontconfig1 && \
    rm -rf /var/lib/apt/lists/* && \
    pip install --no-cache-dir \
       paddleocr \
       flask \
       pillow \
       requests \
       paddlepaddle

WORKDIR /app
COPY server.py /app/

EXPOSE 8866
CMD ["python", "server.py"]

