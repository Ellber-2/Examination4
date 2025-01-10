package org.example.Examination4.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class ApiRequest {

	private static String canvasApiUrl;
	private static String accessToken;

	public static void main(String[] args) {
		// Get the access token from the JSON file
		accessToken = getAccessTokenFromFile();
		if (accessToken != null) {
			System.out.println("Access Token: " + accessToken); // Print the access token (for testing)

			// Set the Canvas API URL
			setCanvasApiUrl();

			// Now use the access token in the API call to create an event
			if (canvasApiUrl != null) {
				createEvent();
				//createTimeeditCalendarEvent("https://cloud.timeedit.net/ltu/web/schedule1/ri10985QX28Z04Q6ZW6gc565y90Z6Y58704gxY7Qb57aY050X39Q5757Y637Q8.html");
			} else {
				System.err.println("Canvas API URL is not set.");
			}
		} else {
			System.err.println("Access token not found.");
		}
	}

	private static void createTimeeditCalendarEvent(String timeeditLink) {
		// Step 1: Validate the input link
		if (timeeditLink == null || !timeeditLink.endsWith(".html")) {
			System.err.println("Invalid TimeEdit link. Please provide a valid link ending with '.html'.");
			return;
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
					// Example: Parse the JSON using Jackson
					ObjectMapper objectMapper = new ObjectMapper();
					JsonNode rootNode = objectMapper.readTree(jsonResponse);

					// Example of accessing specific parts of the JSON
					JsonNode eventsNode = rootNode.path("reservations");
					if (eventsNode.isArray()) {
						for (JsonNode event : eventsNode) {
							System.out.println("Event: " + event.toString());
						}
					}
				} else {
					System.err.println("Failed to fetch JSON. Status code: " + statusCode);
				}
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private static void createEvent() {
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			// Create a POST request for creating a calendar event
			HttpPost request = new HttpPost(canvasApiUrl + "/calendar_events.json");

			// Create the form data payload
			String formData = "calendar_event[context_code]=user_146018" +
					"&calendar_event[title]=Test" +
					"&calendar_event[start_at]=2025-01-04T14:00:00" +
					"&calendar_event[end_at]=2025-01-04T14:45:00" +
					"&calendar_event[description]=testing";

			// Set the body of the request using StringEntity
			StringEntity entity = new StringEntity(formData, ContentType.APPLICATION_FORM_URLENCODED);
			request.setEntity(entity);

			// Add the Authorization header with Bearer token
			request.addHeader("Authorization", "Bearer " + accessToken);

			try (CloseableHttpResponse response = httpClient.execute(request)) {
				// Get the status code of the response
				int statusCode = response.getCode();

				// Convert the response to a string
				String jsonResponse = EntityUtils.toString(response.getEntity());

				// Check if the response is successful
				if (statusCode == 201) {
					System.out.println("Request was created!");
				} else {
					System.out.println("Request failed with status code: " + statusCode);
				}
			}
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
	}

	private static void setCanvasApiUrl() {
		canvasApiUrl = "https://canvas.ltu.se/api/v1"; // Replace with your Canvas domain
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