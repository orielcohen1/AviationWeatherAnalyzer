package com.example.demo;

import java.time.LocalDateTime;

public class Flight {
    // השתמשנו באובייקטים מסוג Airport שכבר בנינו! (Composition)
    private Airport departure;
    private Airport destination;

    private LocalDateTime departureTime;
    private int durationMinutes; // משך הטיסה בדקות (הכי נוח לחישובים)
    private FlightRules rules;   // כללי הטיסה המבוקשים

    // Constructor
    public Flight(Airport departure, Airport destination, LocalDateTime departureTime, int durationMinutes, FlightRules rules) {
        this.departure = departure;
        this.destination = destination;
        this.departureTime = departureTime;
        this.durationMinutes = durationMinutes;
        this.rules = rules;
    }

    // Getters
    public Airport getDeparture() {
        return departure; }

    public Airport getDestination() {
        return destination; }

    public LocalDateTime getDepartureTime() {
        return departureTime; }

    public int getDurationMinutes() {
        return durationMinutes; }

    public FlightRules getRules() {
        return rules; }

    // פונקציית עזר שתהיה שימושית מאוד למנוע החוקים שלנו בהמשך
    //   by local departure airport time היא מחשבת מתי הטיסה אמורה לנחות
    public LocalDateTime getEstimatedArrivalTime() {
        return departureTime.plusMinutes(durationMinutes);
    }
}
