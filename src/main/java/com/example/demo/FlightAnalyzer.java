package com.example.demo;

public class FlightAnalyzer {

    public static FlightCategory determineCategory(int visMeters, int ceilingFeet) {

        // 1. שולפים את הנתונים מתוך הדוח המפוענח


        if(visMeters < 1600 || ceilingFeet < 500){
            return FlightCategory.LIFR;
        }
        else if (visMeters < 4800 || ceilingFeet < 1000) {
            return FlightCategory.IFR;
        }
        else if (visMeters < 8000 || ceilingFeet < 3000) {
            return FlightCategory.MVFR;
        }
        return FlightCategory.VFR;


    }
}