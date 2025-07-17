from flask import Flask, request, jsonify
import os
from werkzeug.utils import secure_filename
from main import run_ocr

app = Flask(__name__)
UPLOAD_FOLDER = "/tmp/uploads"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

@app.route('/trocr', methods=['POST'])
def trocr_endpoint():
    if 'image' not in request.files:
        return jsonify({'error': 'No image file provided'}), 400

    file = request.files['image']
    filename = secure_filename(file.filename)
    image_path = os.path.join(UPLOAD_FOLDER, filename)
    file.save(image_path)

    try:
        text = run_ocr(image_path)
        return jsonify({'text': text})
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8555)
