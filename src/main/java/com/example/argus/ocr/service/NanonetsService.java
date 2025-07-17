package com.example.argus.ocr.service;

import com.example.argus.ocr.dto.OcrResponse;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@Service("nanonetsOCR")
public class NanonetsService implements OcrService {

    private final RestTemplate rest;

    public NanonetsService(RestTemplateBuilder builder) {
        // Use the JDK HttpURLConnection–based factory, but buffer the body
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setBufferRequestBody(true);

        this.rest = builder
                .requestFactory(() -> factory)
                .build();
    }

    @Override
    public OcrResponse extractText(MultipartFile file) {
        OcrResponse resp = new OcrResponse("Nanonets-OCR-s", null, null);

        try {
            byte[] data = file.getBytes();
            ByteArrayResource bar = new ByteArrayResource(data) {
                @Override public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            MultiValueMap<String,Object> body = new LinkedMultiValueMap<>();
            body.add("file", bar);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String,Object>> req =
                    new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String,String> json = rest.postForObject(
                    "http://localhost:8000/ocr",  // your FastAPI route
                    req,
                    Map.class
            );

            resp.setExtractedText(json.get("text"));
            resp.setError       (json.get("error"));

        } catch (Exception e) {
            resp.setError("Nanonets-OCR-s failed: " + e.getMessage());
        }
        return resp;
    }
}
