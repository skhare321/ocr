package com.example.argus.ocr.service;

import com.example.argus.ocr.dto.OcrResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service("ocrSpace")
public class OcrSpaceOcrService implements OcrService {

    @Value("K83041659988957")
    private String apiKey;
    OCRLanguageCleaner cleaner = new OCRLanguageCleaner();
    private static final String OCR_SPACE_URL = "https://api.ocr.space/parse/image";

    @Override
    public OcrResponse extractText(MultipartFile file) {
        OcrResponse response = new OcrResponse("OCR.Space", null, null);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("apikey", apiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("language", "eng");
            body.add("isOverlayRequired", "false");
            body.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> apiResponse = restTemplate.postForEntity(OCR_SPACE_URL, requestEntity, String.class);

            if (apiResponse.getStatusCode().is2xxSuccessful() && apiResponse.getBody() != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(apiResponse.getBody());

                String parsedText = root.path("ParsedResults").get(0).path("ParsedText").asText();
                String cleanText = parsedText
                        .replace("\\r\\n", "\n")
                        .replace("\\n", "\n")
                        .replace("\\r", "\n")
                        .replaceAll("\\\\", "")
                        .replaceAll("\\s+", " ").trim();

                String corrected = cleaner.cleanText(cleanText);
                response.setExtractedText(corrected);
            } else {
                response.setError("OCR.Space failed: " + apiResponse.getStatusCode());
            }

        } catch (IOException e) {
            response.setError("OCR.Space error: " + e.getMessage());
        }

        return response;
    }

    private String extractTextFromJson(String json) {
        try {
            com.fasterxml.jackson.databind.JsonNode node =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            return node.path("ParsedResults").get(0).path("ParsedText").asText();
        } catch (Exception e) {
            return "Error parsing OCR response";
        }
    }
}

