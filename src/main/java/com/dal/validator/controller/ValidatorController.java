package com.dal.validator.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
public class ValidatorController {

    @Value("${processor.service.url}")
    private String processorServiceUrl;

    @PostMapping("/store-file")
    public ResponseEntity<?> storeRequest(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        // Validate input
        if (!request.containsKey("file") || request.get("file") == null || request.get("file").trim().isEmpty() ||
                !request.containsKey("data") || request.get("data") == null) {
            response.put("file", null);
            response.put("error", "Invalid JSON input.");
            return ResponseEntity.badRequest().body(response);
        }

        String fileName = request.get("file");
        String fileData = request.get("data");
//        String storagePath = "C:/Users/kirta/IdeaProjects/vaja/vaja/A1/";
        String storagePath = "/kirtan_PV_dir/"; // mapped to GKE persistent storage
        File file = new File(storagePath + fileName);

        try {
            // Write data to file
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs(); // Ensure directory exists
            }
            java.nio.file.Files.write(file.toPath(), fileData.getBytes());

            response.put("file", fileName);
            response.put("message", "Success.");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            response.put("file", fileName);
            response.put("error", "Error while storing the file to the storage.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/calculate")
    public ResponseEntity<?> processRequest(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        // Validate JSON input
        if (!request.containsKey("file") || request.get("file") == null) {
            response.put("file", null);
            response.put("error", "Invalid JSON input.");
            return ResponseEntity.badRequest().body(response);
        }

        String fileName = request.get("file");
        String product = request.get("product");

        File file = new File("/kirtan_PV_dir/" + fileName);
        System.out.println(file.getAbsolutePath());
        if (!file.exists()) {
            response.put("file", fileName);
            response.put("error", "File not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // Forward request to Container 2
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<?> processorResponse = restTemplate.postForEntity(processorServiceUrl, request, Map.class);
            return ResponseEntity.status(processorResponse.getStatusCode()).body(processorResponse.getBody());

        }catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected error occurred in Service 1"));
        }
    }
}
