package org.example.Examination4.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ApiService {

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
                    System.out.println("Fetched JSON from TimeEdit:");
                    System.out.println(jsonResponse);

                    // Process the JSON data here (e.g., create calendar events)
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode rootNode = objectMapper.readTree(jsonResponse);

                    JsonNode eventsNode = rootNode.path("reservations");
                    if (eventsNode.isArray()) {
                        for (JsonNode event : eventsNode) {
                            System.out.println("Event: " + event.toString());
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
}
