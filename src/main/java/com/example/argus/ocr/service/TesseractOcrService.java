package com.example.argus.ocr.service;


import com.example.argus.ocr.dto.OcrResponse;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("tesseract")
@Lazy
public class TesseractOcrService implements OcrService {

    private final RestTemplate rest;
    OCRLanguageCleaner cleaner = new OCRLanguageCleaner();

    public TesseractOcrService(RestTemplateBuilder builder) {
        this.rest = builder.build();
    }

    @Override
    public OcrResponse extractText(MultipartFile file) {
        OcrResponse resp = new OcrResponse("Tesseract-HTTP", null, null);
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("options", "{\"languages\":[\"eng\"]}");
            body.add("file", new MultipartInputStreamFileResource(
                    file.getInputStream(), file.getOriginalFilename()
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String,Object>> req = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String,Object> json = rest.postForObject(
                    "http://localhost:8884/tesseract", req, Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String,Object> data = (Map<String,Object>)json.get("data");
            String text = (String)data.get("stdout");
            String corrected = cleaner.cleanText(text);

            resp.setExtractedText(corrected);

        } catch (Exception e) {
            resp.setError("OCR-server failed: " + e.getMessage());
        }
        return resp;
    }
}


