package com.example.demo;

public class MetarParser {

    // הפונקציה המרכזית שמקבלת את מחרוזת המטאר ומחזירה אובייקט מסודר
    public static WeatherReport parse(String rawMetar) {

        // הפקודה split חותכת את המחרוזת למערך של מילים.
        // הביטוי הרגולרי "\\s+" אומר לחתוך לפי כל רווח (או רצף של רווחים),
        // כך שכל בלוק נתונים (כמו הראות או הרוח) יישב בתא נפרד במערך.
        String[] tokens = rawMetar.split("\\s+");

        // 1. הגדרת משתנים מקומיים עם ערכי ברירת מחדל מחוץ ללולאה.
        // אנחנו מגדירים אותם כאן כדי שהם לא יימחקו מהזיכרון בכל סיבוב של הלולאה.
        int temperatureC = 0;
        int dewPointC = 0;
        int windDirection = 0;
        int windSpeed = 0;
        int windGust = 0;
        int pressureHpa = 0;
        int visibilityMeters = 9999; // ברירת מחדל לראות טובה (CAVOK)
        int cloudBaseFeet = 99999;   // ברירת מחדל לשמיים נקיים

        // 2. לולאת for-each: רצה על כל איבר (token) בתוך המערך (tokens)
        for (String token : tokens) {

            // --- טמפרטורה ונקודת טל ---
            // Regex matches only real temp/dew tokens like "22/15", "M05/M10", "00/M03".
            // This avoids misreading fractional visibility ("1/2SM") or RVR ("R04R/P6000FT").
            if (token.matches("M?\\d{1,2}/M?\\d{1,2}")) {
                String[] tempAndDew = token.split("/");
                String tempStr = tempAndDew[0].replace("M", "-");
                String dewStr  = tempAndDew[1].replace("M", "-");
                temperatureC = Integer.parseInt(tempStr);
                dewPointC    = Integer.parseInt(dewStr);
            }

            // --- רוח ---
            // הפקודה endsWith בודקת אם המילה מסתיימת באותיות "KT" (קשרים)
            else if (token.endsWith("KT")) {
                // הפקודה substring(0, 3) שולפת רק את התווים ממיקום 0 עד 3 (לא כולל 3) - כלומר, הכיוון.
                String directionStr = token.substring(0, 3);

                // מוודאים שהרוח לא משתנית (VRB) לפני שמנסים להמיר למספר, כדי למנוע קריסה
                if (!directionStr.equals("VRB")) {
                    windDirection = Integer.parseInt(directionStr);
                }

                // הפקודה substring(3) לוקחת את כל השאר (ממיקום 3 והלאה),
                // ואז replace מוחקת את האותיות KT כדי להשאיר רק מספרים
                String speedStr = token.substring(3).replace("KT", "");

                // בדיקה אם יש משבים (Gusts) בעזרת האות G
                if (speedStr.contains("G")) {
                    // מפצלים לשני מספרים: לפני ה-G ואחריו
                    String[] speedParts = speedStr.split("G");
                    windSpeed = Integer.parseInt(speedParts[0]); // מהירות רגילה
                    windGust = Integer.parseInt(speedParts[1]);  // משב
                } else {
                    // אם אין משבים, כל המחרוזת שנותרה היא המהירות הרגילה
                    windSpeed = Integer.parseInt(speedStr);
                }
            }

            // --- לחץ ברומטרי (תקן בינלאומי) ---
            // הפקודה startsWith בודקת אם המילה מתחילה באות מסוימת (Q)
            else if (token.matches("Q\\d{4}")) {
                // חותכים החוצה את ה-Q (ממיקום 1 עד הסוף) וממירים למספר
                String pressureHpaStr = token.substring(1);
                pressureHpa = Integer.parseInt(pressureHpaStr);
            }

            // --- לחץ ברומטרי (תקן אמריקאי) ---
            else if (token.matches("A\\d{4}")) {
                // חותכים את ה-A
                String pressureStr = token.substring(1);
                int altimeterInt = Integer.parseInt(pressureStr);

                // ממירים מאינצ'ים של כספית להקטו-פסקל
                double inchesOfMercury = altimeterInt / 100.0;
                // הפקודה Math.round מעגלת את התוצאה העשרונית למספר שלם
                pressureHpa = (int) Math.round(inchesOfMercury * 33.8639);
            }

            // --- ראות ---
            // הפקודה matches בודקת בעזרת ביטוי רגולרי (Regex):
            // "\\d{4}" פירושו "בדיוק 4 ספרות (Digits) רצופות, ללא אותיות"
            else if (token.matches("\\d{4}")) {
                visibilityMeters = Integer.parseInt(token);
            }
            // Statute Miles visibility (US format). Handles whole ("2SM"), fractional ("1/2SM"),
            // and below-minimum ("M1/4SM") values.
            else if (token.endsWith("SM")) {
                String visStr = token.replace("SM", "");
                if (visStr.startsWith("M")) visStr = visStr.substring(1); // strip "less-than" M prefix
                try {
                    double miles;
                    if (visStr.contains("/")) {
                        String[] parts = visStr.split("/");
                        miles = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
                    } else {
                        miles = Double.parseDouble(visStr);
                    }
                    visibilityMeters = (int) (miles * 1609.34);
                } catch (NumberFormatException ignored) { }
            }

            // --- עננים ---
            // שרשור תנאים באמצעות || (משמעותו "או"): אם מתחיל ב-FEW או ב-SCT...
            else if (token.startsWith("FEW") || token.startsWith("SCT") ||
                    token.startsWith("BKN") || token.startsWith("OVC")) {

                // מוודאים שמעדכנים רק את בסיס הענן הנמוך ביותר (שנפגש ראשון)
                if (cloudBaseFeet == 99999 && token.length() >= 6) {
                    String cloudStr = token.substring(3, 6);
                    if (cloudStr.matches("\\d{3}")) {
                        cloudBaseFeet = Integer.parseInt(cloudStr) * 100;
                    }
                }
            }
        }

        // 3. יצירת האובייקט הסופי מהמשתנים שחילצנו
        // מילת המפתח new מקצה זיכרון ויוצרת מופע חדש של מחלקת WeatherReport
        return new WeatherReport(
                rawMetar,
                visibilityMeters,
                cloudBaseFeet,
                false,
                temperatureC,
                dewPointC,
                pressureHpa,
                FlightAnalyzer.determineCategory(visibilityMeters, cloudBaseFeet), // החישוב החכם מופעל כאן!
                windDirection,
                windSpeed,
                windGust
        );
    }
}