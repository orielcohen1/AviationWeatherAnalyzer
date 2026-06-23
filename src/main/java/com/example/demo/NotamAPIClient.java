package com.example.demo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotamAPIClient {

    public static String fetchNotamsForAirports(List<String> icaoCodes) {
        if (icaoCodes == null || icaoCodes.isEmpty()) return "No airports specified.";
        return fetchFromFaa(String.join(",", icaoCodes));
    }

    public static String fetchRawNotams(String icaoCode) {
        return fetchFromFaa(icaoCode);
    }

    private static String fetchFromFaa(String designators) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            String formBody = "searchType=0&designatorsForLocation=" + designators
                    + "&actionType=notamRetrievalByICAOs&reportType=RAW&formatType=DOMESTIC";
            String icaoCode = designators; // keep for error messages

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://notams.aim.faa.gov/notamSearch/search"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body().contains("notamList")) {
                return parseNotamJson(response.body(), icaoCode);
            }

            return "No NOTAM data available for " + icaoCode + " from this source.";

        } catch (Exception e) {
            return "Error fetching NOTAMs: " + e.getMessage();
        }
    }

    private static String parseNotamJson(String json, String icaoCode) {
        if (json.contains("\"notamList\":[]")) {
            return "No active NOTAMs for " + icaoCode + ".";
        }

        Matcher matcher = Pattern.compile("\"icaoMessage\":\"(.*?)\"", Pattern.DOTALL).matcher(json);
        StringBuilder sb = new StringBuilder();
        int count = 0;

        while (matcher.find()) {
            String msg = matcher.group(1)
                    .replace("\\r\\n", "\n")
                    .replace("\\n", "\n")
                    .replace("\\r", "\n")
                    .trim();
            if (!msg.isEmpty()) {
                sb.append(msg).append("\n\n");
                count++;
            }
        }

        return count == 0
                ? "No active NOTAMs for " + icaoCode + "."
                : sb.toString().trim();
    }
}
