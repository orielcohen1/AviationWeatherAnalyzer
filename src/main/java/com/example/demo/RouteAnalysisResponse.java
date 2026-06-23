package com.example.demo;

import java.util.List;

public class RouteAnalysisResponse {

    private String departure;
    private String destination;
    private int routeDistanceNm;
    private WeatherReport departureWeather;
    private WeatherReport destinationWeather;
    private String departureNotams;
    private String destinationNotams;
    private List<String> enRouteAirports;
    private String enRouteNotams;

    public RouteAnalysisResponse(String departure, String destination, int routeDistanceNm,
                                  WeatherReport departureWeather, WeatherReport destinationWeather,
                                  String departureNotams, String destinationNotams,
                                  List<String> enRouteAirports, String enRouteNotams) {
        this.departure = departure;
        this.destination = destination;
        this.routeDistanceNm = routeDistanceNm;
        this.departureWeather = departureWeather;
        this.destinationWeather = destinationWeather;
        this.departureNotams = departureNotams;
        this.destinationNotams = destinationNotams;
        this.enRouteAirports = enRouteAirports;
        this.enRouteNotams = enRouteNotams;
    }

    public String getDeparture()              { return departure; }
    public String getDestination()            { return destination; }
    public int getRouteDistanceNm()           { return routeDistanceNm; }
    public WeatherReport getDepartureWeather()  { return departureWeather; }
    public WeatherReport getDestinationWeather(){ return destinationWeather; }
    public String getDepartureNotams()        { return departureNotams; }
    public String getDestinationNotams()      { return destinationNotams; }
    public List<String> getEnRouteAirports()  { return enRouteAirports; }
    public String getEnRouteNotams()          { return enRouteNotams; }
}
