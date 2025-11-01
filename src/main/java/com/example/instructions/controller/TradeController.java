package com.example.instructions.controller;

import com.example.instructions.service.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Trade Instruction Ingestion", description = "Endpoints for uploading and ingesting trade instruction files.")
@RestController
@RequestMapping("/instructions/v1/api")
public class TradeController {

    private static final Logger log = LoggerFactory.getLogger(TradeController.class);
    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @Operation(
            summary = "Upload Trade Instructions File",
            description = "Accepts a single .csv or .json file containing bulk trade instructions for asynchronous processing.",
            responses = {
                @ApiResponse(responseCode = "202", description = "File accepted and processing initiated asynchronously."),
                @ApiResponse(responseCode = "400", description = "Invalid file type or empty file."),
                @ApiResponse(responseCode = "500", description = "Internal server error during file processing.")
            }
        )
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@Parameter(description = "The trade instruction file (.csv or .json) to upload.")
    										 @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new ResponseEntity<>("Please select a file to upload.", HttpStatus.BAD_REQUEST);
        }

        String filename = file.getOriginalFilename();  
        try {
            long startTime = System.currentTimeMillis();
            int tradeCount = tradeService.processFileUpload(file);
            long duration = System.currentTimeMillis() - startTime;
            
            String responseMessage = String.format(
                "Successfully queued %d trade instructions from file: %s. Processing initiated asynchronously. Time: %d ms",
                tradeCount, filename, duration
            );
            
            log.info(responseMessage);
            // HttpStatus.ACCEPTED (202) is appropriate as processing is asynchronous
            return new ResponseEntity<>(responseMessage, HttpStatus.ACCEPTED);
            
        } catch (IllegalArgumentException e) {
            log.error("File upload error: {}", e.getMessage());
            return new ResponseEntity<>("File upload failed: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            log.error("File I/O error during upload: {}", e.getMessage());
            return new ResponseEntity<>("Failed to process file.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}