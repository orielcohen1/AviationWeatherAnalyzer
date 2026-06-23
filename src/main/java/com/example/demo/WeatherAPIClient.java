package com.example.demo;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service

public class WeatherAPIClient {

    // הפונקציה המרכזית: מקבלת קוד שדה (למשל "LLHA") ומחזירה את המטאר כטקסט
    public static String fetchRawMetar(String airportCode) {

        // הכתובת של השרת, משורשרת עם קוד שדה התעופה שביקשנו
        String url = "https://aviationweather.gov/api/data/metar?ids=" + airportCode + "&format=raw";

        // 1. יצירת ה"לקוח" (Client) עם הגדרת זמן קצוב (Timeout) של 10 שניות
        // למקרה שהאינטרנט איטי או השרת לא מגיב.
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // 2. יצירת ה"בקשה" (Request) אל הכתובת שהגדרנו
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET() // אנחנו רק מבקשים מידע, לכן זו בקשת GET
                .build();

        try {
            // 3. שליחת הבקשה ושמירת התשובה (Response) מהשרת
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // אם הכל תקין (קוד 200 באינטרנט אומר OK), נחזיר את גוף התשובה (הטקסט)
            if (response.statusCode() == 200) {
                return response.body().trim(); // trim מנקה רווחים מיותרים בהתחלה ובסוף
            } else {
                System.out.println("Error pulling data. Status code: " + response.statusCode());
                return null;
            }

        } catch (Exception e) {
            // אם הייתה תקלת רשת (אין אינטרנט למשל), הקוד לא יקרוס אלא יתפוס את השגיאה
            System.out.println("Network Error: " + e.getMessage());
            return null;
        }
    }
}