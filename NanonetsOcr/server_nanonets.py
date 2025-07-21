import os
from fastapi import FastAPI, UploadFile, File
import requests
from tempfile import NamedTemporaryFile

app = FastAPI()

@app.post("/ocr")
async def ocr_image(image: UploadFile = File(...)):
    # Get API key and model ID from environment variables
    api_key = os.getenv("NANONETS_API_KEY")
    model_id = os.getenv("NANONETS_MODEL_ID")
    
    if not api_key or not model_id:
        return {"error": "API key or model ID not set"}
    
    with NamedTemporaryFile(delete=False) as temp_file:
        contents = await image.read()
        temp_file.write(contents)
        temp_path = temp_file.name
    
    url = f"https://app.nanonets.com/api/v2/OCR/Model/{model_id}/LabelFile/"
    try:
        with open(temp_path, "rb") as f:
            response = requests.post(
                url,
                auth=requests.auth.HTTPBasicAuth(api_key, ""),
                files={"file": f}
            )
        os.unlink(temp_path)  # Clean up temp file
        
        if response.status_code != 200:
            return {"error": f"API error: {response.text}"}
        
        data = response.json()
        result = data.get("result", [{}])[0]
        predictions = result.get("prediction", [])
        if predictions:
            key_value = {pred.get("label", "unknown"): pred.get("ocr_text", "") for pred in predictions}
            return {"extracted_data": key_value}
        return {"text": result.get("ocr_text", "")}
    
    except Exception as e:
        if os.path.exists(temp_path):
            os.unlink(temp_path)
        return {"error": str(e)}

# To run locally: uvicorn app:app --reload
