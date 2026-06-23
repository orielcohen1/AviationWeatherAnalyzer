package com.example.demo;

public class Notam {
    private String id;              // מזהה הנוטאם הרשמי (למשל: A0123/26)
    private String rawText;         // הטקסט המקורי והמלא שהתקבל מהשרת
    private NotamSeverity severity; // רמת החומרה שקבענו לו

    // בנאי (Constructor)
    public Notam(String id, String rawText, NotamSeverity severity) {
        this.id = id;
        this.rawText = rawText;
        this.severity = severity;
    }

    // פונקציות גישה (Getters)
    public String getId() {
        return id;
    }

    public String getRawText() {
        return rawText;
    }

    public NotamSeverity getSeverity() {
        return severity;
    }

    // פונקציית הדפסה מסודרת
    @Override
    public String toString() {
        // מדפיס בפורמט: [WARNING] A0123/26: NAV VOR UNUSABLE...
        return String.format("[%s] %s: %s", severity.name(), id, rawText);
    }
}