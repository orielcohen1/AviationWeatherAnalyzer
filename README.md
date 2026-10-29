# Aviation Weather Analyzer

A Spring Boot web app for pre-flight briefings: pulls live METAR weather and NOTAMs for an airport (or an entire route), and uses an LLM to turn the raw NOTAM text into a plain-English safety summary.

## Features

- **Airport lookup** — enter an ICAO code to get the current METAR (parsed into flight category, wind, visibility, ceiling, etc.) and raw NOTAMs.
- **AI NOTAM interpretation** — NOTAMs are summarized by an LLM into CRITICAL / WARNING / INFO, one sentence per NOTAM.
- **Route analysis** — enter departure and destination ICAO codes to get weather for both ends, the great-circle distance, en-route airports along the path, and an AI-generated en-route NOTAM briefing grouped by airport.
- Simple static HTML/JS front end served by the same Spring Boot app.

## Tech stack

- Java 22, Spring Boot 3.3 (`spring-boot-starter-web`)
- Maven
- [Jsoup](https://jsoup.org/) for HTML parsing
- Jackson for JSON (AI API calls)
- Data sources: [aviationweather.gov](https://aviationweather.gov/) (METAR + airport coordinates) and the [FAA NOTAM Search](https://notams.aim.faa.gov/) API
- AI: [Groq](https://groq.com/) (`llama-3.1-8b-instant`) for NOTAM interpretation

## Running locally

Requires Java 22 and Maven.

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080` (or `$PORT` if set).

### AI interpretation (optional)

NOTAM interpretation requires a free [Groq API key](https://console.groq.com/):

```bash
export GROQ_API_KEY=your_key_here
```

Without it, the app still works — AI endpoints just return a message saying the key isn't set.

## API endpoints

| Endpoint | Description |
|---|---|
| `GET /api/analyze?airport=ICAO` | METAR + raw NOTAMs for one airport |
| `GET /api/notams/interpret?airport=ICAO` | AI summary of that airport's NOTAMs |
| `GET /api/route?from=ICAO&to=ICAO` | Weather, distance, and en-route airports for a route |
| `GET /api/route/interpret?from=ICAO&to=ICAO&airports=ICAO,ICAO` | AI summary of en-route NOTAMs |

## Deployment

Live at [aviationweatheranalyzer.onrender.com](https://aviationweatheranalyzer.onrender.com) (Render free tier — spins down after inactivity, first request can take ~50s to wake up).

Deployed via the included `Dockerfile` (Render has no native Java buildpack). Also includes a `Procfile` and `system.properties` for Heroku/Railway-style platforms (Java 22 buildpack, `$PORT` support).

## Project structure

```
src/main/java/com/example/demo/
  DemoApplication.java        Spring Boot entry point
  FlightController.java       REST endpoints
  WeatherAPIClient.java       METAR fetch (aviationweather.gov)
  MetarParser.java            Raw METAR -> WeatherReport
  NotamAPIClient.java         NOTAM fetch (FAA NOTAM Search)
  NotamAIAgent.java           Groq LLM calls for NOTAM interpretation
  AirportInfoClient.java      Airport coordinate lookup
  RouteAnalyzer.java          Distance + en-route airport detection
src/main/resources/static/index.html   Front end
```
