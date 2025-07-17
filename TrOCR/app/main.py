from transformers import TrOCRProcessor, VisionEncoderDecoderModel
from PIL import Image
import torch
import sys

# Load model
processor = TrOCRProcessor.from_pretrained("microsoft/trocr-base-printed")
model = VisionEncoderDecoderModel.from_pretrained("microsoft/trocr-base-printed")

def run_ocr(image_path):
    image = Image.open(image_path).convert("RGB")
    image = image.crop(image.getbbox())
    image = image.resize((1024, 1024))

    pixel_values = processor(images=image, return_tensors="pt").pixel_values

    generated_ids = model.generate(
        pixel_values,
        num_beams=5,
        max_length=512,
        early_stopping=True
    )
    text = processor.batch_decode(generated_ids, skip_special_tokens=True)[0]
    return text

if __name__ == "__main__":
    print(run_ocr(sys.argv[1]))
