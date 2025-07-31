from flask import Flask, request, jsonify
import os
import base64
import re
import json
import requests
from openai import OpenAI

app = Flask(__name__)

client = OpenAI(
    base_url="https://openrouter.ai/api/v1",
    api_key="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"
)

@app.route('/ocr', methods=['POST'])
def vision_ocr():
    
    prompt_input = """Please examine the attached image and extract every key–value pair it contains.  "
    "• Keys may be separated from values by a colon (“key: value”), by whitespace (“key value”), or by uppercase‐only values.  "
    "• Ignore any text that isn’t part of a key–value pair.  "
    "• Output **only** a single JSON object string, with each key and its value in quotes, e.g.:\n\n"
    "{\"key1\": \"value1\", \"key2\": \"value2\", \"anotherKey\": \"Some BLOCK VALUE\"}\n\n"
    "Do not include any extra commentary or formatting."""
    
    """OCR endpoint using a free OpenRouter-supported vision model."""
    prompt = request.form.get('prompt', prompt_input)
    # image_url = request.form.get('file:///home/shivansh/Downloads/my_handwritten.jpg')
    # if image_url:
    #     image_input = {"type": "image_url", "image_url": {"url": image_url}}
    # else:
    image_file = request.files.get('image')
    if not image_file:
        return jsonify({'error': 'No image provided.'}), 400
    encoded = base64.b64encode(image_file.read()).decode('utf-8')
    data_url = f"data:image/jpeg;base64,{encoded}"
    image_input = {"type": "image_url", "image_url": {"url": data_url}}


    text_input = {"type": "text", "text": prompt}

    messages = [{
        "role": "user",
        "content": [text_input, image_input]
    }]

    # print(messages)

    response = client.chat.completions.create(
        model="mistralai/mistral-small-3.2-24b-instruct:free",
        messages=messages
    )
    extracted = response.choices[0].message.content

    cleaned = re.sub(r"```json|```", "", extracted).strip()
    try:
        data = json.loads(cleaned)
    except json.JSONDecodeError:
        return jsonify({'error': 'Failed to parse JSON', 'raw': extracted}), 500

    return jsonify(data)

@app.route('/text', methods=['POST'])
def text():
    """OCR endpoint using a free OpenRouter-supported vision model."""
    prompt = request.form.get('prompt', '')
    # image_url = request.form.get('file:///home/shivansh/Downloads/my_handwritten.jpg')
    # if image_url:
    #     image_input = {"type": "image_url", "image_url": {"url": image_url}}
    # else:
    image_file = request.files.get('image')
    if not image_file:
        return jsonify({'error': 'No image provided.'}), 400
    encoded = base64.b64encode(image_file.read()).decode('utf-8')
    image_input = {"type": "image_base64", "image": {"data": encoded}}

    text_input = {"type": "text", "text": prompt}

    messages = [{
        "role": "user",
        "content": [text_input, image_input]
    }]

    response = client.chat.completions.create(
        model="cognitivecomputations/dolphin-mistral-24b-venice-edition:free",
        messages=messages
    )
    extracted = response.choices[0].message.content
    return jsonify({'text': extracted})

@app.route('/models', methods=['GET'])
def list_models():
    """List available vision models from OpenRouter."""
    api_key="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"
    headers = {"Authorization": f"Bearer {api_key}"}
    resp = requests.get("https://openrouter.ai/api/v1/models", headers=headers)
    return jsonify(resp.json())

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)