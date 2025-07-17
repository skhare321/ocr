package com.example.argus.ocr.service;

import com.example.argus.ocr.dto.OcrResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service("paddleOCR")
public class PaddleOcrService implements OcrService {
    private static final Logger logger = LoggerFactory.getLogger(PaddleOcrService.class);
    private final RestTemplate rest;
    private final ObjectMapper mapper = new ObjectMapper();

    public PaddleOcrService(RestTemplateBuilder builder) {
        this.rest = builder.build();
    }

    @Override
    public OcrResponse extractText(MultipartFile file) {
        OcrResponse resp = new OcrResponse("PaddleOCR", null, null);

        try {
            logger.info("Processing file: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());
            byte[] bytes = file.getBytes();
            if (bytes.length == 0) {
                logger.error("Empty file received");
                resp.setError("Empty file uploaded");
                return resp;
            }

            ByteArrayResource bar = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", bar);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(body, headers);

            logger.info("Sending request to http://172.17.0.1:8866/ocr");
            ResponseEntity<String> response = rest.exchange(
                    "http://172.17.0.1:8866/ocr",
                    HttpMethod.POST,
                    req,
                    String.class
            );

            logger.info("Response received: {}", response.getBody());
            Map<String, Object> json = mapper.readValue(response.getBody(), Map.class);

            if (json.containsKey("error")) {
                resp.setError("PaddleOCR failed: " + json.get("error"));
            } else {
                resp.setExtractedText((String) json.get("text"));
                resp.setError(null);
            }

        } catch (Exception e) {
            logger.error("PaddleOCR processing failed", e);
            resp.setError("PaddleOCR failed: " + e.getMessage());
        }

        return resp;
    }
}