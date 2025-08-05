package com.demo.formapp;

public class OcrConfig {
    private static String ocrType = "llm";

    public static String getOcrType() {
        return ocrType;
    }

    public static void setOcrType(String type) {
        ocrType = type;
    }
}

