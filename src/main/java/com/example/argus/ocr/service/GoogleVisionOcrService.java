package com.example.argus.ocr.service;


import com.example.argus.ocr.dto.OcrResponse;
import com.google.cloud.vision.v1.*;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.vision.v1.Image;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.util.Collections;

@Service("googleVision")
public class GoogleVisionOcrService implements OcrService {

    @Override
    public OcrResponse extractText(MultipartFile file) {
        OcrResponse response = new OcrResponse("Google Vision", null, null);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create(
                ImageAnnotatorSettings.newBuilder()
                        .setCredentialsProvider(() ->
                                ServiceAccountCredentials.fromStream(new FileInputStream("src/main/resources/vision-key.json")))
                        .build())) {

            ByteString byteString = ByteString.readFrom(file.getInputStream());

            Image image = Image.newBuilder().setContent(byteString).build();
            Feature feature = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(image)
                    .build();

            AnnotateImageResponse imageResponse = client.batchAnnotateImages(Collections.singletonList(request))
                    .getResponses(0);

            if (imageResponse.hasError()) {
                response.setError("Google Vision Error: " + imageResponse.getError().getMessage());
            } else {
                response.setExtractedText(imageResponse.getFullTextAnnotation().getText());
            }

        } catch (Exception e) {
            response.setError("Google Vision OCR failed: " + e.getMessage());
        }

        return response;
    }
}

