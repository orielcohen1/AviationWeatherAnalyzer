package com.example.demo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteAnalyzer {

    private static final double EARTH_RADIUS_NM = 3440.065;

    public static double distanceNm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                  * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_NM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static double bearing(double lat1, double lon1, double lat2, double lon2) {
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    // Perpendicular distance in NM from point 3 to the great circle route from 1 to 2.
    private static double crossTrackNm(double lat1, double lon1,
                                        double lat2, double lon2,
                                        double lat3, double lon3) {
        double d13 = distanceNm(lat1, lon1, lat3, lon3) / EARTH_RADIUS_NM;
        double brng13 = Math.toRadians(bearing(lat1, lon1, lat3, lon3));
        double brng12 = Math.toRadians(bearing(lat1, lon1, lat2, lon2));
        return Math.abs(Math.asin(Math.sin(d13) * Math.sin(brng13 - brng12))) * EARTH_RADIUS_NM;
    }

    // Distance in NM from departure to the point on the route closest to point 3.
    private static double alongTrackNm(double lat1, double lon1,
                                        double lat2, double lon2,
                                        double lat3, double lon3) {
        double d13 = distanceNm(lat1, lon1, lat3, lon3) / EARTH_RADIUS_NM;
        double brng13 = Math.toRadians(bearing(lat1, lon1, lat3, lon3));
        double brng12 = Math.toRadians(bearing(lat1, lon1, lat2, lon2));
        double dxt = Math.asin(Math.sin(d13) * Math.sin(brng13 - brng12));
        double dat = Math.acos(Math.max(-1, Math.min(1, Math.cos(d13) / Math.cos(dxt))));
        return dat * EARTH_RADIUS_NM;
    }

    public static List<String> findEnRouteAirports(double depLat, double depLon,
                                                    double destLat, double destLon) {
        double totalDist = distanceNm(depLat, depLon, destLat, destLon);

        double minLat = Math.min(depLat, destLat) - 2.5;
        double maxLat = Math.max(depLat, destLat) + 2.5;
        double minLon = Math.min(depLon, destLon) - 2.5;
        double maxLon = Math.max(depLon, destLon) + 2.5;

        try {
            String url = String.format(
                    "https://aviationweather.gov/api/data/airport?bbox=%.2f,%.2f,%.2f,%.2f&format=json",
                    minLat, minLon, maxLat, maxLon);

            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200 || !resp.body().trim().startsWith("[")) {
                return Collections.emptyList();
            }

            // Parse each airport object from the JSON array
            String body = resp.body().trim();
            String inner = body.substring(1, body.length() - 1);
            String[] objects = inner.split("\\},\\s*\\{");

            List<double[]> candidates = new ArrayList<>(); // [lat, lon, alongTrack, icaoIndex]
            List<String> icaos = new ArrayList<>();

            double maxCrossTrack = Math.max(100.0, totalDist * 0.08);
            double minAlong = Math.max(30.0, totalDist * 0.05);
            double maxAlong = totalDist - minAlong;

            for (String obj : objects) {
                try {
                    Matcher icaoM = Pattern.compile("\"icaoId\":\"([A-Z][A-Z0-9]{2,3})\"").matcher(obj);
                    Matcher latM  = Pattern.compile("\"lat\":\\s*([-\\d.]+)").matcher(obj);
                    Matcher lonM  = Pattern.compile("\"lon\":\\s*([-\\d.]+)").matcher(obj);
                    if (!icaoM.find() || !latM.find() || !lonM.find()) continue;

                    String icao = icaoM.group(1);
                    double lat  = Double.parseDouble(latM.group(1));
                    double lon  = Double.parseDouble(lonM.group(1));

                    double cross = crossTrackNm(depLat, depLon, destLat, destLon, lat, lon);
                    double along = alongTrackNm(depLat, depLon, destLat, destLon, lat, lon);

                    if (cross < maxCrossTrack && along > minAlong && along < maxAlong) {
                        candidates.add(new double[]{lat, lon, along, icaos.size()});
                        icaos.add(icao);
                    }
                } catch (Exception ignored) { }
            }

            // Sort by along-track distance
            candidates.sort(Comparator.comparingDouble(e -> e[2]));

            // Evenly space up to 6 picks across the route
            List<String> result = new ArrayList<>();
            int max = 6;
            if (candidates.size() <= max) {
                for (double[] e : candidates) result.add(icaos.get((int) e[3]));
            } else {
                double step = (double) (candidates.size() - 1) / (max - 1);
                for (int i = 0; i < max; i++) {
                    int idx = (int) Math.round(i * step);
                    result.add(icaos.get((int) candidates.get(idx)[3]));
                }
            }
            return result;

        } catch (Exception e) {
            System.out.println("Error finding en-route airports: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
