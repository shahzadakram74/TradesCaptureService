package com.example.instructions.controller;

import com.example.instructions.service.TradeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    // Inject the TradeService
    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @Operation(
        summary = "Upload Trade Instructions File",
        description = "Accepts a single .csv or .json file for asynchronous processing.",
        responses = {
            @ApiResponse(responseCode = "202", description = "File accepted and processing initiated asynchronously."),
            @ApiResponse(responseCode = "400", description = "Invalid file type or empty file."),
            @ApiResponse(responseCode = "500", description = "Internal server error during file processing.")
        }
    )
    @PostMapping("/upload")    
    public ResponseEntity<String> uploadTradeInstructions(
        @Parameter(description = "The trade instruction file (.csv or .json) to upload.")
        @RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File cannot be empty.");
        }

        try {            
            int recordCount = tradeService.processFileUpload(file);            
            return ResponseEntity.accepted().body("File " + file.getOriginalFilename() + 
                                                 " accepted. Processed " + recordCount + " records successfully.");

        } catch (IllegalArgumentException e) {            
            log.warn("Bad Request during file upload: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (RuntimeException e) {            
            log.error("Error processing file upload: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to process file: " + e.getMessage());
        }
    }
}