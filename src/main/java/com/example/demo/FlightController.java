package com.example.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api")
public class FlightController {

    @GetMapping("/analyze")
    public ResponseEntity<?> analyze(@RequestParam String airport) {
        String icao = airport.trim().toUpperCase();

        String metar = WeatherAPIClient.fetchRawMetar(icao);
        if (metar == null || metar.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("{\"error\":\"No METAR data found for: " + icao + "\"}");
        }

        WeatherReport weather = MetarParser.parse(metar);
        String notams = NotamAPIClient.fetchRawNotams(icao);

        return ResponseEntity.ok(new AnalysisResponse(weather, notams));
    }

    @GetMapping(value = "/notams/interpret", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<String> interpretNotams(@RequestParam String airport) {
        String icao = airport.trim().toUpperCase();
        String rawNotams = NotamAPIClient.fetchRawNotams(icao);
        String interpretation = NotamAIAgent.interpretAll(icao, rawNotams);
        return ResponseEntity.ok(interpretation);
    }

    @GetMapping("/route")
    public ResponseEntity<?> analyzeRoute(@RequestParam String from, @RequestParam String to) {
        String depIcao  = from.trim().toUpperCase();
        String destIcao = to.trim().toUpperCase();

        // Fetch weather for both airports
        String depMetar  = WeatherAPIClient.fetchRawMetar(depIcao);
        String destMetar = WeatherAPIClient.fetchRawMetar(destIcao);

        if (depMetar == null || depMetar.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("{\"error\":\"No METAR found for departure airport: " + depIcao + "\"}");
        }
        if (destMetar == null || destMetar.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("{\"error\":\"No METAR found for destination airport: " + destIcao + "\"}");
        }

        WeatherReport depWeather  = MetarParser.parse(depMetar);
        WeatherReport destWeather = MetarParser.parse(destMetar);

        // Fetch NOTAMs for departure and destination
        String depNotams  = NotamAPIClient.fetchRawNotams(depIcao);
        String destNotams = NotamAPIClient.fetchRawNotams(destIcao);

        // Find en-route airports and their NOTAMs
        double[] depCoords  = AirportInfoClient.fetchCoordinates(depIcao);
        double[] destCoords = AirportInfoClient.fetchCoordinates(destIcao);

        int routeDistanceNm = 0;
        List<String> enRouteAirports = Collections.emptyList();
        String enRouteNotams = "Route coordinates unavailable — en-route NOTAMs could not be fetched.";

        if (depCoords != null && destCoords != null) {
            routeDistanceNm = (int) RouteAnalyzer.distanceNm(
                    depCoords[0], depCoords[1], destCoords[0], destCoords[1]);
            enRouteAirports = RouteAnalyzer.findEnRouteAirports(
                    depCoords[0], depCoords[1], destCoords[0], destCoords[1]);
            enRouteNotams = enRouteAirports.isEmpty()
                    ? "No significant airports found along this route."
                    : NotamAPIClient.fetchNotamsForAirports(enRouteAirports);
        }

        return ResponseEntity.ok(new RouteAnalysisResponse(
                depIcao, destIcao, routeDistanceNm,
                depWeather, destWeather,
                depNotams, destNotams,
                enRouteAirports, enRouteNotams));
    }

    @GetMapping(value = "/route/interpret", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<String> interpretRouteNotams(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String airports) {

        String depIcao  = from.trim().toUpperCase();
        String destIcao = to.trim().toUpperCase();

        List<String> enRoute = (airports != null && !airports.isBlank())
                ? Arrays.asList(airports.split(","))
                : Collections.emptyList();

        String rawNotams = enRoute.isEmpty()
                ? "No en-route airports specified."
                : NotamAPIClient.fetchNotamsForAirports(enRoute);

        String interpretation = NotamAIAgent.interpretRoute(depIcao, destIcao, enRoute, rawNotams);
        return ResponseEntity.ok(interpretation);
    }
}
