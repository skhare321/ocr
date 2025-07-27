#!/usr/bin/env bash
set -e

IMAGE=llmwhisperer-srv
CONTAINER=llmwhisperer-srv

# rebuild
docker stop $CONTAINER 2>/dev/null || true
docker rm   $CONTAINER 2>/dev/null || true
docker build -t $IMAGE .

# run (pass your keys via env‑vars)
docker run -d \
  --name $CONTAINER \
  -p 8118:8118 \
  -e LLMWHISPERER_API_KEY="$LLMWHISPERER_API_KEY" \
  -e OPENAI_API_KEY="$OPENAI_API_KEY" \
  $IMAGE

