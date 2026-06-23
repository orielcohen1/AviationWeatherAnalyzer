package com.example.demo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotamAIAgent {

    // הכנס את המפתח שלך בתוך המרכאות
    private static final String API_KEY = "AQ.Ab8RN6Io2Hn3BEC35XOs3Ns8esnJ3MCB4FRRscwHTw0F_MhzmQ";
    private static final String[] MODELS = {
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite",
        "gemini-2.0-flash"
    };
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    private static String apiUrl(String model) {
        return "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + API_KEY;
    }

    // שים לב: הפונקציה עכשיו מחזירה אובייקט Notam אמיתי, לא סתם סטרינג!
    // Escapes a string for embedding inside a JSON double-quoted value.
    private static String jsonEscape(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> sb.append(c);
            }
        }
        return sb.toString();
    }

    // Sends all raw NOTAMs to Gemini and returns a plain-English pilot briefing.
    public static String interpretAll(String airport, String rawNotams) {
        try {
            if (rawNotams == null || rawNotams.isBlank() || rawNotams.startsWith("No NOTAM")) {
                return "No NOTAMs are currently available for " + airport + " from this data source.";
            }

            String prompt = "You are an aviation safety expert providing a pre-flight NOTAM briefing for airport "
                    + airport + ". "
                    + "Read the raw NOTAMs below and summarize them in plain English for a pilot. "
                    + "Lead with CRITICAL items (closed runways, airspace closures, ILS/nav outages), "
                    + "then WARNING items (equipment degraded, construction near taxiways), "
                    + "then brief INFO items. "
                    + "Write one clear sentence per NOTAM. "
                    + "If there are no significant operational impacts, say so clearly.\\n\\n"
                    + "RAW NOTAMS:\\n"
                    + jsonEscape(rawNotams);

            String jsonPayload = """
                    {
                      "contents": [{
                        "parts": [{
                          "text": "%s"
                        }]
                      }]
                    }
                    """.formatted(prompt);

            HttpClient client = HttpClient.newHttpClient();

            for (String model : MODELS) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl(model)))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String body = response.body();
                    Matcher matcher = Pattern.compile("\"text\":\\s*\"(.*?)\"(?:\\s*,|\\s*\\})", Pattern.DOTALL).matcher(body);
                    if (matcher.find()) {
                        return matcher.group(1)
                                .replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\")
                                .trim();
                    }
                }
                // 503 = overloaded, 429 = rate limited — try next model
                if (response.statusCode() != 503 && response.statusCode() != 429) break;
            }

            return "AI interpretation unavailable — all models are currently busy. Try again in a moment.";

        } catch (Exception e) {
            return "Error contacting AI: " + e.getMessage();
        }
    }

    public static Notam analyzeNotam(String notamId, String rawNotam) {
        try {
            // 1. הפרומפט החדש: נוקשה ומדויק
            String prompt = "You are an aviation expert. Read the NOTAM. " +
                    "1. Summarize it in one clear sentence. " +
                    "2. Classify severity as exactly: INFO, WARNING, or CRITICAL (RWY CLSD must be CRITICAL). " +
                    "Reply ONLY in this exact format: SUMMARY|SEVERITY. Do not add any other text.";

            String jsonPayload = """
                    {
                      "contents": [{
                        "parts": [{
                          "text": "%s"
                        }]
                      }]
                    }
                    """.formatted(prompt + " RAW NOTAM: " + rawNotam.replace("\"", "\\\""));

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // 2. חילוץ התשובה מתוך ה-JSON של גוגל בעזרת חיפוש תבניות (Regex)
                String body = response.body();
                Matcher matcher = Pattern.compile("\"text\":\\s*\"(.*?)\"", Pattern.DOTALL).matcher(body);

                if (matcher.find()) {
                    // מנקים קצת תווים מיותרים שה-AI עלול להוסיף
                    String cleanResponse = matcher.group(1).replace("\\n", "").trim();

                    // 3. פיצול התשובה לשני חלקים לפי הקו האנכי |
                    String[] parts = cleanResponse.split("\\|");

                    if (parts.length == 2) {
                        String summary = parts[0].trim();
                        String severityStr = parts[1].trim();

                        // ממירים את הטקסט ("CRITICAL") ל-Enum האמיתי שלנו
                        NotamSeverity severity = NotamSeverity.valueOf(severityStr);

                        // 4. בום! מחזירים אובייקט שלם למערכת
                        return new Notam(notamId, summary, severity);
                    }
                }
            }
            return null;

        } catch (Exception e) {
            System.out.println("Error talking to AI: " + e.getMessage());
            return null;
        }
    }
}