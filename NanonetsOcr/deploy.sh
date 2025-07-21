#!/bin/bash
docker stop ocr-container || true
docker rm ocr-container || true

# Remove image if exists
docker rmi ocr-service || true

# Build the Docker image
docker build -t ocr-service .


# Run the container (replace with your actual API key and model ID)
# You can also pass them via a .env file or secrets manager for production
docker run -d -p 8800:80 \
  --name ocr-container \
  --env NANONETS_API_KEY=a8fbabde-6592-11f0-8e1e-5af4c2d7f67e \
  --env NANONETS_MODEL_ID=becb98f4-e4fc-42a5-ac71-5b6cd156509d \
  ocr-service

echo "Service deployed. Access at http://localhost:8800/ocr"
