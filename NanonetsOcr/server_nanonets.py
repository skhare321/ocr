import io, logging
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import JSONResponse
from PIL import Image
from transformers import AutoProcessor, AutoModelForImageTextToText

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("nanonets-ocr-server")

app = FastAPI()
MODEL_ID = "nanonets/Nanonets-OCR-s"

logger.info(f"Loading processor + model for {MODEL_ID}…")
# we trust the remote code & opt in to `use_fast=True` here
processor = AutoProcessor.from_pretrained(
    MODEL_ID,
    trust_remote_code=True,
    use_fast=True
)
model = AutoModelForImageTextToText.from_pretrained(
    MODEL_ID,
    trust_remote_code=True
)
model.eval()
logger.info("Model loaded.")

@app.get("/ping")
async def ping():
    return {"ping": "pong"}

@app.post("/ocr")
async def ocr_endpoint(
    file: UploadFile = File(...),
    prompt: str = File(
        ...,
        description=(
            "Instruction to the model. "
            "Defaults to: "
            "\"Extract the text from the above document as if you were reading it naturally. "
            "Return the tables in html format. Return the equations in LaTeX…\""
        )
    )
):

    try:
        raw = await file.read()
        img = Image.open(io.BytesIO(raw)).convert("RGB")
    except Exception as e:
        raise HTTPException(400, f"Invalid image: {e}")

    logger.info(f"Received {file.filename!r} size={img.size} mode={img.mode}")

    try:
        inputs = processor(
            images=img,
            text=prompt,
            return_tensors="pt",
        )
        logger.debug(f"Processor inputs: {inputs.keys()}")
    except Exception as e:
        logger.exception("Preprocessing failed")
        return JSONResponse(500, {"text": None, "error": f"Preprocess error: {e}"})

    try:
        outputs = model.generate(**inputs, max_new_tokens=4096)
        text = processor.batch_decode(outputs, skip_special_tokens=True)[0]
        return {"text": text, "error": None}
    except Exception as e:
        logger.exception("OCR failure")
        return JSONResponse(500, {"text": None, "error": str(e)})

