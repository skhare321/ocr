package com.example.argus.ocr.dto;


import lombok.*;
@NoArgsConstructor
@Getter
@Setter
public class OcrResponse {
    private String provider;
    private String extractedText;
    private String error;

    public OcrResponse(String provider, String extractedText, String error) {
        this.provider = provider;
        this.extractedText = extractedText;
        this.error = error;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public void setError(String error) {
        this.error = error;
    }
}
