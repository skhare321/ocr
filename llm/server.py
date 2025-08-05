
from flask import Flask, request, jsonify
import logging, base64, re, json, requests
from openai import OpenAI

API_KEY = "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"
BASE_URL = "https://openrouter.ai/api/v1"
OCR_MODEL = "meta-llama/llama-3.2-11b-vision-instruct:free"
TEXT_MODEL = "cognitivecomputations/dolphin-mistral-24b-venice-edition:free"
REQUEST_TIMEOUT = 120

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)

client = OpenAI(
    api_key=API_KEY,
    base_url=BASE_URL
)


SYSTEM_OCR = (
    "You are a JSON‐only extraction assistant."
    "Process the attached image and return exactly one JSON object of all key/value pairs—no extra text."
    "Remember that the form is related to health care so if you find any anomaly in spelling then correct it."
    "If you find the key to be present and nothing after ':' then make it empty string (""), otherwise it distorts remaining correct json data"
)

SYSTEM_FORMATTER = (
    "You are a JSON repair assistant.  I will give you a string that is intended to be a JSON object but may have syntax errors "
    "(missing quotes, commas, backslashes, etc.).  Output ONLY the corrected, valid JSON object with keys and string values in double quotes, "
    "proper commas, and no extra commentary or markdown."
    "Remember that the form is related to health care so if you find any anomaly in spelling then correct it."
    "If you find the key to be present and nothing after ':' then make it empty string (""), otherwise it distorts remaining correct json data"
)

def format_json(raw_str: str):
    """
    Send raw_str to the TEXT_MODEL to repair into valid JSON.
    Returns the repaired JSON-parsable string.
    """
    user_content = f"```json\n{raw_str}\n```"
    messages = [
        {"role": "system", "content": SYSTEM_FORMATTER},
        {"role": "user",   "content": user_content}
    ]
    resp = client.chat.completions.create(
        model=TEXT_MODEL,
        messages=messages,
        timeout=REQUEST_TIMEOUT
    )
    fixed = resp.choices[0].message.content.strip()
    return re.sub(r"```(?:json)?\s*|\s*```", "", fixed).strip()


@app.route("/ocr", methods=["POST"])
def vision_ocr():
    image = request.files.get("image")
    if not image:
        return jsonify({"error": "No image provided"}), 400

    raw = image.read()
    data_url = "data:image/jpeg;base64," + base64.b64encode(raw).decode()

    messages = [
        {"role": "system", "content": SYSTEM_OCR},
        {"role": "user", "content": [{"type": "image_url", "image_url": {"url": data_url}}]}
    ]

    last_exc = None
    for _ in range(2):
        try:
            resp = client.chat.completions.create(
                model=OCR_MODEL,
                messages=messages,
                timeout=REQUEST_TIMEOUT
            )
            break
        except Exception as e:
            last_exc = e
            logging.warning("OCR attempt failed: %s", e)
            time.sleep(1)
    else:
        return jsonify({"error": "OCR service unavailable"}), 503

    raw_out = resp.choices[0].message.content
    cleaned = re.sub(r"```(?:json)?\s*|\s*```", "", raw_out).strip()

    try:
        return jsonify(json.loads(cleaned))
    except json.JSONDecodeError:
        logging.warning("Malformed JSON from OCR, raw='%s'", cleaned)

        try:
            fixed = format_json(cleaned)
            return jsonify(json.loads(fixed))
        except Exception:
            logging.exception("Formatter failed on OCR fallback")
            return jsonify({
                "error": "Invalid JSON and formatter failed",
                "raw_ocr": cleaned
            }), 502


@app.route("/text", methods=["POST"])
def vision_text():
    raw = request.form.get("raw")
    if not raw:
        return jsonify({"error": "No raw data provided"}), 400

    try:
        fixed = format_json(raw)
        return jsonify(json.loads(fixed))
    except json.JSONDecodeError:
        logging.exception("Formatter produced invalid JSON")
        return jsonify({
            "error": "Still invalid JSON after formatting",
            "fixed_raw": fixed
        }), 502
    except Exception as e:
        logging.exception("Formatter service unavailable")
        return jsonify({"error": "Service unavailable", "detail": str(e)}), 503
    except Exception:
        logging.exception("Unexpected error in /text")
        return jsonify({"error": "Internal error"}), 500
@app.route("/models", methods=["GET"])
def list_models():
    try:
        headers = {"Authorization": f"Bearer {API_KEY}"}
        r = requests.get(f"{BASE_URL}/models", headers=headers, timeout=10)
        r.raise_for_status()
        return jsonify(r.json())
    except Exception:
        app.logger.exception("Failed to fetch models")
        return jsonify({"error": "Could not fetch model list"}), 502


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
