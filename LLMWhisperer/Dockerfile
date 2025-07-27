FROM python:3.12-slim

WORKDIR /app


RUN apt-get update && apt-get install -y \
    git \
    libjpeg-dev \
    libpng-dev \
    && rm -rf /var/lib/apt/lists/*
    
COPY requirements.txt .
RUN pip install --upgrade pip && pip install -r requirements.txt

# clone the LLMWhisperer handwritten‑PDF extraction pipeline
RUN git clone --depth=1 https://github.com/Zipstack/llmwhisperer-handwritten-pdf-extraction.git

# install its Python deps
RUN pip install --upgrade pip \
 && pip install -r llmwhisperer-handwritten-pdf-extraction/requirements.txt

# bring in our server code
COPY server.py .

# expose port and start
EXPOSE 8118
CMD ["python", "server.py"]
