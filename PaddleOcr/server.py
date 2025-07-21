from flask import Flask, request, jsonify
from paddleocr import PaddleOCR
from PIL import UnidentifiedImageError
import tempfile, os, logging, json

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

ocr = PaddleOCR(
    lang='en', 
    use_doc_orientation_classify=True,
    use_doc_unwarping=True,
    use_textline_orientation=True
)

os.makedirs("output", exist_ok=True)

@app.route("/paddleocr", methods=["POST"])
def paddleocr():
    img_file = request.files.get("image")
    if not img_file:
        return jsonify({"error": "no file part; send form-field 'image'"}), 400

    ext = os.path.splitext(img_file.filename)[1] or ".png"
    tmp = tempfile.NamedTemporaryFile(suffix=ext, delete=False)
    try:
        img_file.save(tmp.name)
        tmp.close()

        results = ocr.predict(input=tmp.name)
        logger.info(f"Raw results: {results}") 

        kv = {}
        for res in results:
            # Ensure raw is a dict (some OCR engines return raw.json as string)
            raw = res.json if isinstance(res.json, dict) else json.loads(res.json)

            # Access nested 'rec_texts'
            inner_res = raw.get("res", {})
            texts = inner_res.get("rec_texts", [])

            if not texts:
                logger.warning("No rec_texts found in result")
                continue

            for text in texts:
                if ':' in text:
                    parts = text.split(':', 1)
                    key = parts[0].strip()
                    value = parts[1].strip() if len(parts) > 1 else ""
                    kv[key] = value

            # Save output files
            res.save_to_json("output")
            res.save_to_img("output")  

        if not kv:
            logger.warning("No key-value pairs extracted")
            raw_all = [res.json if isinstance(res.json, dict) else json.loads(res.json) for res in results]
            return jsonify({"error": "No text extracted", "raw_results": raw_all}), 400

        return jsonify(kv)

    except UnidentifiedImageError as uie:
        logger.error("Invalid image format", exc_info=True)
        return jsonify({"error": f"Invalid image: {uie}"}), 400
    except Exception as e:
        logger.exception("OCR failed")
        return jsonify({"error": str(e)}), 500
    finally:
        try:
            os.remove(tmp.name)
        except OSError:
            pass

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8866)