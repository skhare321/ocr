#!/usr/bin/env bash
set -euo pipefail

# make sure an output folder exists on the host
mkdir -p ./output

# Stop & remove any old container
docker stop ocr-server        2>/dev/null || true
docker rm   ocr-server        2>/dev/null || true

# Remove old image
docker rmi paddleocr-server   2>/dev/null || true

# Build the new image
docker build -t paddleocr-server .

# Run the container, mounting host ./output → container /app/output
docker run -d \
  --name ocr-server \
  -p 8866:8866 \
  -v "$(pwd)/output:/app/output" \
  -v "/home/shivansh/Desktop/ocr/ocr_media:/app/ocr_media" \
  paddleocr-server


echo "✅ PaddleOCR server is up on http://localhost:8866"
echo "   Outputs will appear in $(pwd)/output"
