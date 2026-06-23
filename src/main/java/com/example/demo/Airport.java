package com.example.demo;

import java.util.ArrayList;
import java.util.List;

public class Airport {
    private String icaoCode;
    private String name;

    // הרשימה החדשה שתחזיק את הנוטאמים של השדה
    private List<Notam> activeNotams;

    public Airport(String icaoCode, String name) {
        this.icaoCode = icaoCode;
        this.name = name;
        // חשוב לאתחל את הרשימה כדי שלא נקבל NullPointerException
        this.activeNotams = new ArrayList<>();
    }

    public String getIcaoCode() {
        return icaoCode;
    }

    public String getName() {
        return name;
    }

    public List<Notam> getActiveNotams() {
        return activeNotams;
    }

    // הפונקציה שהייתה חסרה לקומפיילר!
    public void addNotam(Notam notam) {
        this.activeNotams.add(notam);
    }

    // פונקציית עזר שבודקת האם יש נוטאמים פעילים בכלל
    public boolean hasActiveNotams() {
        return !activeNotams.isEmpty();
    }
}