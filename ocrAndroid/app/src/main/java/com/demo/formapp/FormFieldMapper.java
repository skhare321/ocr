package com.demo.formapp;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FormFieldMapper {
    private static final String[] STOP_WORDS = {
            "of"
    };
    private static final double MATCH_THRESHOLD = 0.6;

    public static void fill(Context context, View[] fields, JSONObject json) {
        Map<String,String> data = jsonToMap(json);

        for (Map.Entry<String,String> entry : data.entrySet()) {
            String rawKey = entry.getKey();
            String value  = entry.getValue();
            if (value == null || value.isEmpty()) continue;

            String normKey = stripStopWords(normalize(rawKey));

            View bestMatch   = null;
            double bestScore = 0.0;

            for (View field : fields) {
                String entryName = context.getResources()
                        .getResourceEntryName(field.getId());
                String normField = normalize(entryName);

                double score = similarity(normKey, normField);
                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = field;
                }
            }

            if (bestScore >= MATCH_THRESHOLD && bestMatch != null) {
                applyValue(bestMatch, value);
            }
        }
    }

    private static Map<String,String> jsonToMap(JSONObject json) {
        Map<String,String> map = new HashMap<>();
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String val = json.optString(key, null);
            map.put(key, val);
        }
        return map;
    }

    private static String normalize(String s) {
        return s.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    private static String stripStopWords(String s) {
        for (String sw : STOP_WORDS) {
            s = s.replaceAll("(?<=^|[A-Za-z0-9])" + sw + "(?=$|[A-Za-z0-9])", "");
        }
        return s;
    }

    private static void applyValue(View field, String value) {
        if (field instanceof EditText) {
            ((EditText) field).setText(value);
        } else if (field instanceof Spinner) {
            Spinner sp = (Spinner) field;
            @SuppressWarnings("unchecked")
            ArrayAdapter<String> ad = (ArrayAdapter<String>) sp.getAdapter();
            for (int i = 0; i < ad.getCount(); i++) {
                if (ad.getItem(i).equalsIgnoreCase(value)) {
                    sp.setSelection(i);
                    return;
                }
            }
        } else if (field instanceof RadioGroup) {
            RadioGroup rg = (RadioGroup) field;
            for (int i = 0; i < rg.getChildCount(); i++) {
                RadioButton rb = (RadioButton) rg.getChildAt(i);
                if (rb.getText().toString().equalsIgnoreCase(value)) {
                    rb.setChecked(true);
                    return;
                }
            }
        }
    }


    private static double similarity(String a, String b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        int dist = levenshteinDistance(a, b);
        int maxLen = Math.max(a.length(), b.length());
        return 1.0 - ((double) dist / maxLen);
    }

    private static int levenshteinDistance(String a, String b) {
        int n = a.length(), m = b.length();
        int[][] dp = new int[n+1][m+1];
        for (int i = 0; i <= n; i++) dp[i][0] = i;
        for (int j = 0; j <= m; j++) dp[0][j] = j;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int cost = (a.charAt(i-1) == b.charAt(j-1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i-1][j] + 1,    // deletion
                                dp[i][j-1] + 1),   // insertion
                        dp[i-1][j-1] + cost // substitution
                );
            }
        }
        return dp[n][m];
    }
}
