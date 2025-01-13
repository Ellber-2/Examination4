package org.example.Examination4.controller;

import org.example.Examination4.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private ApiService apiService;

    @PostMapping("/create-calendar-event")
    public ResponseEntity<?> createCalendarEvent(@RequestBody String jsonPayload) {
        return apiService.createCalendarEvent(jsonPayload);
    }
    @PostMapping("/fetch-schedule")
    public ResponseEntity<?> fetchSchedule(@RequestBody TimeEditRequest timeEditRequest) {
        return apiService.createTimeeditCalendarEvent(timeEditRequest.getTimeeditLink());
    }


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
