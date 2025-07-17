package com.example.argus.ocr.service;

import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OCRLanguageCleaner {

    private final JLanguageTool langTool;

    public OCRLanguageCleaner() {
        langTool = new JLanguageTool(new AmericanEnglish());
    }

    public String cleanText(String inputText) throws IOException {

        String cleanText = inputText
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\\r", "\n")
                .replaceAll("\\\\", "")
                .replaceAll("\\s+", " ").trim();

        List<RuleMatch> matches = langTool.check(cleanText);

        Collections.sort(matches, Comparator.comparingInt(RuleMatch::getFromPos).reversed());

        StringBuilder correctedText = new StringBuilder(cleanText);

        for (RuleMatch match : matches) {
            List<String> suggestions = match.getSuggestedReplacements();
            if (!suggestions.isEmpty()) {
                correctedText.replace(
                        match.getFromPos(),
                        match.getToPos(),
                        suggestions.get(0)
                );
            }
        }

        return correctedText.toString();
    }
}
