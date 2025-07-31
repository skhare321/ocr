set -e

docker build -t ocr-openrouter-server .

docker rm -f ocr-openrouter-server || true

docker run -d --name ocr-openrouter-server -p 5000:5000 \
    -e OPENROUTER_API_KEY \
    ocr-openrouter-server