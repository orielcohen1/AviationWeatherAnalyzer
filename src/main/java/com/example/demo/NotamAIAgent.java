package com.example.demo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotamAIAgent {

    private static final String API_KEY = System.getenv("ANTHROPIC_API_KEY");
    private static final String API_URL  = "https://api.anthropic.com/v1/messages";
    private static final String MODEL    = "claude-haiku-4-5-20251001";

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

    private static String callClaude(String prompt) {
        if (API_KEY == null || API_KEY.isBlank()) {
            return "AI interpretation unavailable — ANTHROPIC_API_KEY environment variable is not set.";
        }

        String body = """
                {
                  "model": "%s",
                  "max_tokens": 1024,
                  "messages": [{"role": "user", "content": "%s"}]
                }
                """.formatted(MODEL, jsonEscape(prompt));

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type",     "application/json")
                    .header("Authorization",    "Bearer " + API_KEY)
                    .header("anthropic-version","2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Matcher m = Pattern.compile("\"text\":\\s*\"(.*?)\"(?=\\s*[\\},])", Pattern.DOTALL)
                        .matcher(response.body());
                if (m.find()) {
                    return m.group(1)
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\")
                            .trim();
                }
            }

            if (response.statusCode() == 401)
                return "AI unavailable — API key invalid or not authorized.";
            if (response.statusCode() == 429)
                return "AI unavailable — rate limit reached. Try again in a moment.";

            return "AI unavailable (HTTP " + response.statusCode() + ").";

        } catch (Exception e) {
            return "Error contacting AI: " + e.getMessage();
        }
    }

    public static String interpretAll(String airport, String rawNotams) {
        if (rawNotams == null || rawNotams.isBlank() || rawNotams.startsWith("No NOTAM")) {
            return "No NOTAMs are currently available for " + airport + ".";
        }

        String prompt = "You are an aviation safety expert providing a pre-flight NOTAM briefing for "
                + airport + ". Summarize the NOTAMs in plain English for the pilot. "
                + "Lead with CRITICAL items (closed runways, airspace closures, ILS/nav outages), "
                + "then WARNINGS, then INFO. One clear sentence per NOTAM. "
                + "If there are no significant impacts, say so.\n\nRAW NOTAMS:\n" + rawNotams;

        return callClaude(prompt);
    }

    public static String interpretRoute(String from, String to,
                                        List<String> enRoute, String rawNotams) {
        if (rawNotams == null || rawNotams.isBlank() || rawNotams.startsWith("No active")) {
            return "No en-route NOTAMs found for the route from " + from + " to " + to + ".";
        }

        String airports = enRoute.isEmpty() ? "none identified" : String.join(", ", enRoute);

        String prompt = "You are an aviation safety expert providing an en-route NOTAM briefing "
                + "for a flight from " + from + " to " + to + ". "
                + "Route airports: " + airports + ". "
                + "Group by airport. Lead with CRITICAL items, then WARNINGS, then INFO. "
                + "One sentence per NOTAM. If an airport has no issues, say so briefly.\n\n"
                + "RAW NOTAMS:\n" + rawNotams;

        return callClaude(prompt);
    }

    public static Notam analyzeNotam(String notamId, String rawNotam) {
        String prompt = "You are an aviation expert. Read the NOTAM and reply ONLY in this exact format: "
                + "SUMMARY|SEVERITY  where SEVERITY is INFO, WARNING, or CRITICAL (RWY CLSD = CRITICAL). "
                + "RAW NOTAM: " + rawNotam;

        try {
            String result = callClaude(prompt);
            if (result == null || !result.contains("|")) return null;
            String[] parts = result.trim().split("\\|");
            if (parts.length < 2) return null;
            NotamSeverity severity = NotamSeverity.valueOf(parts[1].trim());
            return new Notam(notamId, parts[0].trim(), severity);
        } catch (Exception e) {
            System.out.println("analyzeNotam error: " + e.getMessage());
            return null;
        }
    }
}
