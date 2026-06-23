package com.example.demo;

public class WeatherReport {
    private String rawMetar;
    private int visibilityMeters;
    private int cloudBaseFeet;
    private boolean hasThunderstorms;
    private int temperatureC;
    private int dewPointC;
    private int pressureHpa;
    private FlightCategory category;

    // נתוני רוח
    private int windDirectionDegrees;
    private int windSpeedKnots;
    private int windGustKnots;

    // בנאי (Constructor)
    public WeatherReport(String rawMetar, int visibilityMeters, int cloudBaseFeet,
                         boolean hasThunderstorms, int temperatureC, int dewPointC,
                         int pressureHpa, FlightCategory category,
                         int windDirectionDegrees, int windSpeedKnots, int windGustKnots) {
        this.rawMetar = rawMetar;
        this.visibilityMeters = visibilityMeters;
        this.cloudBaseFeet = cloudBaseFeet;
        this.hasThunderstorms = hasThunderstorms;
        this.temperatureC = temperatureC;
        this.dewPointC = dewPointC;
        this.pressureHpa = pressureHpa;
        this.category = category;
        this.windDirectionDegrees = windDirectionDegrees;
        this.windSpeedKnots = windSpeedKnots;
        this.windGustKnots = windGustKnots;
    }

    // פונקציות גישה (Getters)
    public String getRawMetar() { return rawMetar; }
    public int getVisibilityMeters() { return visibilityMeters; }
    public int getCloudBaseFeet() { return cloudBaseFeet; }
    public boolean hasThunderstorms() { return hasThunderstorms; }
    public int getTemperatureC() { return temperatureC; }
    public int getDewPointC() { return dewPointC; }
    public int getPressureHpa() { return pressureHpa; }
    public FlightCategory getCategory() { return category; }
    public int getWindDirectionDegrees() { return windDirectionDegrees; }
    public int getWindSpeedKnots() { return windSpeedKnots; }
    public int getWindGustKnots() { return windGustKnots; }
} // <--- זה הסוגר היחיד שצריך להיות בסוף הקובץ כדי לסגור את המחלקה