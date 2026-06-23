package com.example.demo;

public class AnalysisResponse {
    private WeatherReport weather;
    private String notams;

    public AnalysisResponse(WeatherReport weather, String notams) {
        this.weather = weather;
        this.notams = notams;
    }

    public WeatherReport getWeather() { return weather; }
    public String getNotams() { return notams; }
}
