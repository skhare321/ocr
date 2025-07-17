package com.example.argus.ocr.controller;


import com.example.argus.ocr.dto.OcrResponse;
import com.example.argus.ocr.service.OcrService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    private final OcrService tesseractService;
    private final OcrService googleVisionService;
    private final OcrService ocrSpaceService;
    private final OcrService paddleOcrService;
    private final OcrService nanonetsService;


    public OcrController(@Qualifier("tesseract") OcrService tesseractService,
                         @Qualifier("googleVision") OcrService googleVisionService,
                         @Qualifier("ocrSpace") OcrService ocrSpaceService,
                         @Qualifier("paddleOCR") OcrService paddleOcrService,
                         @Qualifier("nanonetsOCR") OcrService nanonetsService){
        this.tesseractService = tesseractService;
        this.googleVisionService = googleVisionService;
        this.ocrSpaceService = ocrSpaceService;
        this.paddleOcrService = paddleOcrService;
        this.nanonetsService = nanonetsService;
    }

    @PostMapping("/tesseract")
    public ResponseEntity<OcrResponse> tesseractOCR(@RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(tesseractService.extractText(image));
    }

    @PostMapping("/google-vision")
    public ResponseEntity<OcrResponse> googleVisionOCR(@RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(googleVisionService.extractText(image));
    }

    @PostMapping("/ocrspace")
    public ResponseEntity<OcrResponse> extractViaOcrSpace(@RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(ocrSpaceService.extractText(image));
    }

    @PostMapping("/paddleocr")
    public ResponseEntity<OcrResponse> paddleOcr(@RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(paddleOcrService.extractText(image));
    }

    @PostMapping("/nanonets")
    public ResponseEntity<OcrResponse> nanonetsOCR(@RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(nanonetsService.extractText(image));
    }
}

