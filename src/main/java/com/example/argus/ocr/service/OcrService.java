package com.example.argus.ocr.service;


import org.springframework.web.multipart.MultipartFile;
import com.example.argus.ocr.dto.OcrResponse;

public interface OcrService {
    OcrResponse extractText(MultipartFile file);
}
