package com.example.employee_managment.controller;

import com.example.employee_managment.dto.ExportRequest;
import com.example.employee_managment.dto.ExportResponse;
import com.example.employee_managment.model.ExportHistory;
import com.example.employee_managment.repository.ExportHistoryRepository;
import com.example.employee_managment.service.ExportJobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/exports")
@CrossOrigin(origins = "*")
public class ExportController {
    
    @Autowired
    private ExportJobService exportJobService;
    
    @Autowired
    private ExportHistoryRepository exportHistoryRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Submit export request
     * POST /api/exports
     */
    @PostMapping
    public ResponseEntity<ExportResponse> submitExportRequest(@RequestBody ExportRequest exportRequest) {
        try {
            // Generate unique reference ID
            String referenceId = exportJobService.generateReferenceId();
            
            // Convert filters to JSON string
            String filtersJson = objectMapper.writeValueAsString(exportRequest);
            
            // Create export history record
            ExportHistory exportHistory = new ExportHistory(
                referenceId,
                exportRequest.getExportType(),
                filtersJson,
                exportRequest.getFields()
            );
            
            if (exportRequest.getUserId() != null) {
                exportHistory.setUserId(exportRequest.getUserId());
            }
            
            // Save to database
            exportHistoryRepository.save(exportHistory);
            
            // Start background processing
            exportJobService.processExportJob(referenceId);
            
            // Return response with reference ID
            ExportResponse response = new ExportResponse(
                referenceId,
                ExportHistory.ExportStatus.PENDING,
                "Export request submitted successfully. Use reference ID to check status."
            );
            
            // Set estimated completion time (rough estimate: 1 second per 1000 records)
            if (exportRequest.getSize() != null) {
                long estimatedSeconds = Math.max(5, exportRequest.getSize() / 1000); // Minimum 5 seconds
                response.setEstimatedCompletion(LocalDateTime.now().plusSeconds(estimatedSeconds));
            }
            
            return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
            
        } catch (Exception e) {
            ExportResponse errorResponse = new ExportResponse(
                null,
                ExportHistory.ExportStatus.FAILED,
                "Failed to submit export request: " + e.getMessage()
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Check export status and download if ready
     * GET /api/exports/{referenceId}
     */
    @GetMapping("/{referenceId}")
    public ResponseEntity<?> checkExportStatus(@PathVariable String referenceId) {
        try {
            Optional<ExportHistory> exportHistory = exportHistoryRepository.findByReferenceId(referenceId);
            
            if (exportHistory.isEmpty()) {
                return new ResponseEntity<>(
                    new ExportResponse(null, ExportHistory.ExportStatus.FAILED, "Export not found"),
                    HttpStatus.NOT_FOUND
                );
            }
            
            ExportHistory export = exportHistory.get();
            
            switch (export.getStatus()) {
                case PENDING:
                    return new ResponseEntity<>(
                        new ExportResponse(referenceId, export.getStatus(), "Export is queued for processing"),
                        HttpStatus.OK
                    );
                    
                case PROCESSING:
                    ExportResponse processingResponse = new ExportResponse(
                        referenceId, 
                        export.getStatus(), 
                        "Export is currently being processed"
                    );
                    processingResponse.setCreatedAt(export.getCreatedAt());
                    // Note: startedAt is not part of the response DTO
                    return new ResponseEntity<>(processingResponse, HttpStatus.OK);
                    
                case COMPLETED:
                    // Return CSV file for download
                    if (export.getCsvData() != null && !export.getCsvData().isEmpty()) {
                        byte[] csvBytes = export.getCsvData().getBytes(StandardCharsets.UTF_8);
                        
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.parseMediaType("text/csv"));
                        headers.setContentDispositionFormData("attachment", "export_" + referenceId + ".csv");
                        headers.setContentLength(csvBytes.length);
                        
                        ExportResponse completedResponse = new ExportResponse(
                            referenceId, 
                            export.getStatus(), 
                            "Export completed successfully"
                        );
                        completedResponse.setTotalRecords(export.getTotalRecords());
                        completedResponse.setFileSize(export.getFileSize());
                        completedResponse.setCreatedAt(export.getCreatedAt());
                        // Note: completedAt is not part of the response DTO
                        
                        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
                    } else {
                        return new ResponseEntity<>(
                            new ExportResponse(referenceId, export.getStatus(), "Export completed but no data found"),
                            HttpStatus.OK
                        );
                    }
                    
                case FAILED:
                    ExportResponse failedResponse = new ExportResponse(
                        referenceId, 
                        export.getStatus(), 
                        "Export failed: " + export.getErrorMessage()
                    );
                    failedResponse.setCreatedAt(export.getCreatedAt());
                    // Note: completedAt is not part of the response DTO
                    return new ResponseEntity<>(failedResponse, HttpStatus.OK);
                    
                default:
                    return new ResponseEntity<>(
                        new ExportResponse(referenceId, export.getStatus(), "Unknown status"),
                        HttpStatus.OK
                    );
            }
            
        } catch (Exception e) {
            return new ResponseEntity<>(
                new ExportResponse(null, ExportHistory.ExportStatus.FAILED, "Error checking status: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * Get all export history for a user
     * GET /api/exports/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ExportHistory>> getUserExportHistory(@PathVariable String userId) {
        List<ExportHistory> exports = exportHistoryRepository.findByUserId(userId);
        return new ResponseEntity<>(exports, HttpStatus.OK);
    }
    
    /**
     * Get all export history
     * GET /api/exports
     */
    @GetMapping
    public ResponseEntity<List<ExportHistory>> getAllExportHistory() {
        List<ExportHistory> exports = exportHistoryRepository.findAll();
        return new ResponseEntity<>(exports, HttpStatus.OK);
    }
    
    /**
     * Cancel export request (if still pending)
     * DELETE /api/exports/{referenceId}
     */
    @DeleteMapping("/{referenceId}")
    public ResponseEntity<String> cancelExport(@PathVariable String referenceId) {
        try {
            Optional<ExportHistory> exportHistory = exportHistoryRepository.findByReferenceId(referenceId);
            
            if (exportHistory.isEmpty()) {
                return new ResponseEntity<>("Export not found", HttpStatus.NOT_FOUND);
            }
            
            ExportHistory export = exportHistory.get();
            
            if (export.getStatus() == ExportHistory.ExportStatus.PENDING) {
                export.setStatus(ExportHistory.ExportStatus.FAILED);
                export.setErrorMessage("Export cancelled by user");
                export.setCompletedAt(LocalDateTime.now());
                exportHistoryRepository.save(export);
                
                return new ResponseEntity<>("Export cancelled successfully", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Cannot cancel export in " + export.getStatus() + " status", HttpStatus.BAD_REQUEST);
            }
            
        } catch (Exception e) {
            return new ResponseEntity<>("Error cancelling export: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 