package org.example.Examination4.controller;

import org.example.Examination4.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:63342")  // Allow requests from this origin
public class ApiController {

    @Autowired
    private ApiService apiService;

    // The method to handle the POST request with the TimeEdit link
    @PostMapping("/fetch-schedule")
    public ResponseEntity<?> fetchSchedule(@RequestBody TimeEditRequest timeEditRequest) {
        // Pass the timeeditLink to the service
        return apiService.createTimeeditCalendarEvent(timeEditRequest.getTimeeditLink());
    }

    // DTO class to hold the timeeditLink
    public static class TimeEditRequest {
        private String timeeditLink;

        public String getTimeeditLink() {
            return timeeditLink;
        }

        public void setTimeeditLink(String timeeditLink) {
            this.timeeditLink = timeeditLink;
        }
    }
}
