#!/bin/bash

CONTAINER_NAME=trocr-api
IMAGE_NAME=trocr-api-img

echo "🔧 Building Docker image..."
docker stop $CONTAINER_NAME 2>/dev/null && docker rm $CONTAINER_NAME 2>/dev/null
docker build -t $IMAGE_NAME .

echo "🚀 Running container on port 8555..."
docker run -d --name $CONTAINER_NAME -p 8555:8555 $IMAGE_NAME
