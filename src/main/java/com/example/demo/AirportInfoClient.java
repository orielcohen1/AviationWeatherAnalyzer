package com.example.demo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AirportInfoClient {

    public static double[] fetchCoordinates(String icaoCode) {
        try {
            String url = "https://aviationweather.gov/api/data/airport?ids=" + icaoCode + "&format=json";
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200) {
                String body = resp.body();
                Matcher lat = Pattern.compile("\"lat\":\\s*([-\\d.]+)").matcher(body);
                Matcher lon = Pattern.compile("\"lon\":\\s*([-\\d.]+)").matcher(body);
                if (lat.find() && lon.find()) {
                    return new double[]{
                        Double.parseDouble(lat.group(1)),
                        Double.parseDouble(lon.group(1))
                    };
                }
            }
        } catch (Exception e) {
            System.out.println("Could not fetch coordinates for " + icaoCode + ": " + e.getMessage());
        }
        return null;
    }
}
