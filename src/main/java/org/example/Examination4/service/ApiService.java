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
import java.util.Iterator;

@Service
public class ApiService {

    private static String canvasApiUrl = "https://canvas.ltu.se/api/v1"; // Replace with your Canvas domain
    private static String accessToken = getAccessTokenFromFile();

    public ResponseEntity<?> createTimeeditCalendarEvent(String timeeditLink) {
        // Step 1: Validate the input link
        if (timeeditLink == null || !timeeditLink.endsWith(".html")) {
            // Return error response with status code 400 (Bad Request)
            return ResponseEntity.badRequest().body("{\"error\": \"Invalid TimeEdit link. Please provide a valid link ending with '.html'\"}");
        }

        // Step 2: Convert the .html link to .json
        String jsonLink = timeeditLink.replace(".html", ".json");

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Step 3: Send GET request to fetch the JSON data
            HttpGet request = new HttpGet(jsonLink);


            try (CloseableHttpResponse response = httpClient.execute(request)) {
                // Get the response status code
                int statusCode = response.getCode();

                if (statusCode == 200) { // HTTP 200 OK
                    // Step 4: Parse the JSON response
                    String jsonResponse = EntityUtils.toString(response.getEntity());
                    /*System.out.println("Fetched JSON from TimeEdit:");
                    System.out.println(jsonResponse);

                     */

                    // Process the JSON data here (e.g., create calendar events)
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode rootNode = objectMapper.readTree(jsonResponse);

                    JsonNode eventsNode = rootNode.path("reservations");
                    if (eventsNode.isArray()) {
                        for (JsonNode event : eventsNode) {
                            //System.out.println("Event: " + event.toString());
                        }
                    }

                    // Return the fetched JSON data in the response
                    return ResponseEntity.ok(rootNode);
                } else {
                    // Return error response with the status code if fetching failed
                    return ResponseEntity.status(statusCode).body("{\"error\": \"Failed to fetch JSON. Status code: " + statusCode + "\"}");
                }
            } catch (ParseException | IOException e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Error during HTTP request: " + e.getMessage() + "\"}");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Error during HTTP request: " + e.getMessage() + "\"}");
        }
    }

    public ResponseEntity<?> createCalendarEvent(String jsonPayload) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            System.out.println("Trying to create calendar event");

            // Parse the JSON payload
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonPayload);

            // Extract reservations
            JsonNode reservations = rootNode.path("reservations");
            if (reservations.isMissingNode() || !reservations.isObject()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\": \"Invalid reservations data\"}");
            }

            // Iterate over the reservations
            for (Iterator<String> it = reservations.fieldNames(); it.hasNext(); ) {
                String reservationKey = it.next();
                JsonNode reservation = reservations.path(reservationKey);

                // Extract the necessary fields
                String contextCode = "user_146018"; // Static or dynamically retrieved
                String title = reservation.path("columns").path("0").asText("Untitled Event");
                String location = reservation.path("columns").path("1").asText("");
                String startDate = reservation.path("startdate").asText("");
                String startTime = reservation.path("starttime").asText("");
                String endDate = reservation.path("enddate").asText("");
                String endTime = reservation.path("endtime").asText("");
                String meetingLink = reservation.path("columns").path("5").asText("");
                String additionalComment = reservation.path("columns").path("4").asText("");
                String text = reservation.path("columns").path("11").asText("");


// Combine the description content
                String description = "Zoom: " + meetingLink +
                        "<br>" + additionalComment +
                        "<br>" + text;

// Combine date and time fields to match the required format
                String startAt = startDate + "T" + startTime;
                String endAt = endDate + "T" + endTime;

// Create the form data payload
                String formData = "calendar_event[context_code]=" + URLEncoder.encode(contextCode, StandardCharsets.UTF_8) +
                        "&calendar_event[title]=" + URLEncoder.encode(title, StandardCharsets.UTF_8) +
                        "&calendar_event[location_name]=" + URLEncoder.encode(location, StandardCharsets.UTF_8) +
                        "&calendar_event[start_at]=" + URLEncoder.encode(startAt, StandardCharsets.UTF_8) +
                        "&calendar_event[end_at]=" + URLEncoder.encode(endAt, StandardCharsets.UTF_8) +
                        "&calendar_event[description]=" + URLEncoder.encode(description, StandardCharsets.UTF_8);

                System.out.println("Processing Event: " + title);
                System.out.println("Form Data: " + formData);

                // Create a POST request for creating a calendar event
                HttpPost request = new HttpPost(canvasApiUrl + "/calendar_events.json");

                // Set the body of the request using StringEntity
                StringEntity entity = new StringEntity(formData, ContentType.APPLICATION_FORM_URLENCODED);
                request.setEntity(entity);

                // Add the Authorization header with Bearer token
                request.addHeader("Authorization", "Bearer " + accessToken);

                // Execute the request and handle the response
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getCode();
                    String jsonResponse = EntityUtils.toString(response.getEntity());

                    if (statusCode != 201) {
                        System.err.println("Failed to create event: " + jsonResponse);
                        return ResponseEntity.status(statusCode).body(jsonResponse);
                    }

                    System.out.println("Successfully created event: " + title);
                }
            }

            return ResponseEntity.status(HttpStatus.OK).body("{\"message\": \"All events processed successfully\"}");

        } catch (IOException | ParseException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Error during HTTP request: " + e.getMessage() + "\"}");
        }
    }


    // Method to read the access token from the JSON file
    private static String getAccessTokenFromFile() {
        try {
            // Path to the JSON file in Documents folder
            String filePath = Paths.get(System.getProperty("user.home"), "Documents", "config.json").toString();

            // Create a Jackson ObjectMapper to parse the JSON file
            ObjectMapper objectMapper = new ObjectMapper();

            // Read the file and parse it into a JsonNode
            JsonNode rootNode = objectMapper.readTree(new File(filePath));

            // Extract the access token from the JSON
            JsonNode accessTokenNode = rootNode.path("access_token");

            // Return the access token as a string
            if (accessTokenNode.isTextual()) {
                return accessTokenNode.asText();
            } else {
                return null; // Token not found or not a string
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null; // In case of error (e.g., file not found)
        }
    }
}