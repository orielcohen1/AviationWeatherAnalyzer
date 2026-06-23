package com.example.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
