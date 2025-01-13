package org.example.Examination4.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

@Service
public class ApiService {

    // API-bas-URL och åtkomsttoken. Bör förvaras säkert och vara konfigurerbar.
    private static final String CANVAS_API_URL = "https://canvas.ltu.se/api/v1";
    private static final String ACCESS_TOKEN = getAccessTokenFromFile();

    public ResponseEntity<?> createTimeeditCalendarEvent(String timeeditLink) {
        // Kontrollerar och omvandlar TimeEdit-länken, hämtar data, och validerar "reservations".
        if (!isValidTimeEditLink(timeeditLink)) {
            return badRequest("Invalid TimeEdit link. Please provide a valid link ending with '.html'");
        }

        String jsonLink = timeeditLink.replace(".html", ".json");

        try {
            String jsonResponse = fetchJsonFromUrl(jsonLink);
            if (jsonResponse == null) {
                return errorResponse(HttpStatus.BAD_GATEWAY, "Failed to fetch JSON from TimeEdit");
            }

            JsonNode reservations = parseJson(jsonResponse).path("reservations");

            return reservations.isArray() ? ResponseEntity.ok(reservations) :
                    badRequest("Invalid JSON structure: 'reservations' not found");
        } catch (Exception e) {
            return internalServerError("Error processing TimeEdit link: " + e.getMessage());
        }
    }

    public ResponseEntity<?> createCalendarEvent(String jsonPayload) {
        // Hanterar JSON-innehåll och skapar kalenderhändelser baserat på reservationer.
        try {
            JsonNode parsedJson = parseJson(jsonPayload);

            JsonNode reservations = parsedJson.has("reservations") ? parsedJson.path("reservations") : parsedJson;

            if (reservations.isObject()) {
                for (String key : iterableFieldNames(reservations)) {
                    createCanvasEvent(reservations.path(key));
                }
            } else if (reservations.isArray()) {
                for (JsonNode reservation : reservations) {
                    createCanvasEvent(reservation);
                }
            } else {
                return badRequest("Invalid reservations data: 'reservations' must be an object or array.");
            }

            return success("All events processed successfully.");
        } catch (Exception e) {
            return internalServerError("Error creating calendar event: " + e.getMessage());
        }
    }

    private void createCanvasEvent(JsonNode reservation) throws IOException, ParseException {
        // Skickar en POST-förfrågan till Canvas API för att skapa en kalenderhändelse.
        String formData = buildEventFormData(reservation);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(CANVAS_API_URL + "/calendar_events.json");
            request.setEntity(new StringEntity(formData, ContentType.APPLICATION_FORM_URLENCODED));
            request.addHeader("Authorization", "Bearer " + ACCESS_TOKEN);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getCode();
                if (statusCode != HttpStatus.CREATED.value()) {
                    throw new IOException("Failed to create event: " + EntityUtils.toString(response.getEntity()));
                }
            }
        }
    }

    private String buildEventFormData(JsonNode reservation) throws IOException {
        String contextCode = "user_146018"; // Static or dynamically retrieved
        String title = reservation.path("columns").path("0").asText("Untitled Event");
        String location = reservation.path("columns").path("1").asText("");
        String startAt = reservation.path("startdate").asText("") + "T" + reservation.path("starttime").asText("");
        String endAt = reservation.path("enddate").asText("") + "T" + reservation.path("endtime").asText("");
        String description = String.join("<br>",
                "Zoom: " + reservation.path("columns").path("5").asText(""),
                reservation.path("columns").path("4").asText(""),
                reservation.path("columns").path("11").asText(""));

        return "calendar_event[context_code]=" + urlEncode(contextCode) +
                "&calendar_event[title]=" + urlEncode(title) +
                "&calendar_event[location_name]=" + urlEncode(location) +
                "&calendar_event[start_at]=" + urlEncode(startAt) +
                "&calendar_event[end_at]=" + urlEncode(endAt) +
                "&calendar_event[description]=" + urlEncode(description);
    }

    private static String getAccessTokenFromFile() {
        // Hämtar åtkomsttoken från en lokal konfigurationsfil. Kan göras säkrare med ett hemlighetsvalv.
        try {
            File file = Paths.get(System.getProperty("user.home"), "Documents", "config.json").toFile();
            JsonNode rootNode = new ObjectMapper().readTree(file);
            return rootNode.path("access_token").asText(null);
        } catch (IOException e) {
            return null;
        }
    }

    private String fetchJsonFromUrl(String url) throws IOException, ParseException {
        // Gör en GET-förfrågan till en URL och returnerar JSON-innehåll om framgångsrikt.
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getCode();
                return statusCode == HttpStatus.OK.value() ?
                        EntityUtils.toString(response.getEntity()) : null;
            }
        }
    }

    private JsonNode parseJson(String json) throws IOException {
        // Tolkar en JSON-sträng till ett JsonNode-objekt.
        return new ObjectMapper().readTree(json);
    }

    private Iterable<String> iterableFieldNames(JsonNode node) {
        // Gör att JsonNode:s fältnamn kan itereras.
        return node::fieldNames;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isValidTimeEditLink(String link) {
        // Kontrollerar om länken slutar med ".html".
        return link != null && link.endsWith(".html");
    }

    private ResponseEntity<String> success(String message) {
        // Returnerar en framgångssvar med ett meddelande.
        return ResponseEntity.ok("{\"message\": \"" + message + "\"}");
    }

    private ResponseEntity<String> badRequest(String message) {
        // Returnerar ett felsvar för ogiltig begäran.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\": \"" + message + "\"}");
    }

    private ResponseEntity<String> internalServerError(String message) {
        // Returnerar ett felsvar för internt serverfel.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"" + message + "\"}");
    }

    private ResponseEntity<String> errorResponse(HttpStatus status, String message) {
        // Returnerar ett felsvar med specifik HTTP-status.
        return ResponseEntity.status(status).body("{\"error\": \"" + message + "\"}");
    }
}