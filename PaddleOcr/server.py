from flask import Flask, request, jsonify
from paddleocr import PaddleOCR
from PIL import UnidentifiedImageError
import tempfile, os, logging, json
import requests

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

LT_API_URL = "https://api.languagetool.org/v2/check"

def clean_text(text: str) -> str:
    """
    Clean up whitespace and run grammar/spell check
    against the public LanguageTool API.
    """
    text = text.replace("\\r\\n", "\n") \
               .replace("\\n", "\n") \
               .replace("\\r", "\n") \
               .replace("\\", "") \
               .strip()
    text = " ".join(text.split())

    resp = requests.post(
        LT_API_URL,
        data={
            "text": text,
            "language": "en-US"
        },
        timeout=10
    )
    if resp.status_code != 200:
        logger.warning("LanguageTool API error: %s %s", resp.status_code, resp.text)
        return text

    data = resp.json()
    corrected = text
    for match in sorted(data.get("matches", []), key=lambda m: m["offset"], reverse=True):
        replacements = match.get("replacements")
        if replacements:
            start = match["offset"]
            length = match["length"]
            corrected = (
                corrected[:start]
                + replacements[0]["value"]
                + corrected[start + length :]
            )
    return corrected

@app.route("/paddleocr", methods=["POST"])
def paddleocr_endpoint():
    img_file = request.files.get("image")
    if not img_file:
        return jsonify({"error": "no file part; send form-field 'image'"}), 400

    ext = os.path.splitext(img_file.filename)[1] or ".png"
    tmp = tempfile.NamedTemporaryFile(suffix=ext, delete=False)

    try:
        img_file.save(tmp.name)
        tmp.close()

        results = ocr.predict(input=tmp.name)
        logger.info("Raw OCR results: %s", results)

        kv = {}
        for res in results:
            raw = res.json if isinstance(res.json, dict) else json.loads(res.json)
            inner = raw.get("res", {})
            texts = inner.get("rec_texts", [])

            if not texts:
                logger.warning("No rec_texts found in result")
                continue

            for line in texts:
                if ':' in line:
                    key_part, val_part = line.split(":", 1)
                    key = clean_text(key_part.strip())
                    val = clean_text(val_part.strip())
                    kv[key] = val

            res.save_to_json("output")
            res.save_to_img("output")

        if not kv:
            logger.warning("No key-value pairs extracted")
            raw_all = [
                (res.json if isinstance(res.json, dict) else json.loads(res.json))
                for res in results
            ]
            return jsonify({
                "error": "No text extracted",
                "raw_results": raw_all
            }), 400

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
