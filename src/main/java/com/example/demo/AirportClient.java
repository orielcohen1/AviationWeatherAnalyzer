package com.example.demo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AirportClient {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Returns [lat, lon] for an ICAO code, or null if not found.
    public static double[] getCoordinates(String icaoCode) {
        try {
            String url = "https://aviationweather.gov/api/data/airport?ids=" + icaoCode + "&format=json";
            HttpResponse<String> resp = CLIENT.send(
                    HttpRequest.newBuilder().uri(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return extractLatLon(resp.body(), 0);
            }
        } catch (Exception e) {
            System.out.println("AirportClient coords error for " + icaoCode + ": " + e.getMessage());
        }
        return null;
    }

    // Returns list of {icaoId, lat, lon} for airports inside the bounding box.
    public static List<String[]> getAirportsInBbox(double minLat, double minLon,
                                                    double maxLat, double maxLon) {
        List<String[]> result = new ArrayList<>();
        try {
            String url = String.format(
                    "https://aviationweather.gov/api/data/airport?bbox=%.3f,%.3f,%.3f,%.3f&format=json",
                    minLat, minLon, maxLat, maxLon);
            HttpResponse<String> resp = CLIENT.send(
                    HttpRequest.newBuilder().uri(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) return result;

            String body = resp.body();
            Matcher icaoM = Pattern.compile("\"icaoId\":\"([A-Z][A-Z0-9]{2,4})\"").matcher(body);

            while (icaoM.find()) {
                String icao = icaoM.group(1);
                int pos = icaoM.end();
                // lat and lon appear within the same JSON object, up to ~600 chars ahead
                String segment = body.substring(pos, Math.min(pos + 600, body.length()));
                double[] ll = extractLatLon(segment, 0);
                if (ll != null) result.add(new String[]{icao, String.valueOf(ll[0]), String.valueOf(ll[1])});
            }
        } catch (Exception e) {
            System.out.println("AirportClient bbox error: " + e.getMessage());
        }
        return result;
    }

    private static double[] extractLatLon(String text, int fromIndex) {
        Matcher latM = Pattern.compile("\"lat\":([-\\d.]+)").matcher(text);
        Matcher lonM = Pattern.compile("\"lon\":([-\\d.]+)").matcher(text);
        if (latM.find(fromIndex) && lonM.find(fromIndex)) {
            return new double[]{Double.parseDouble(latM.group(1)), Double.parseDouble(lonM.group(1))};
        }
        return null;
    }
}
