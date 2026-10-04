package com.trustdesk.trustdesk;

import java.io.IOException;

import tools.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {
    private final EvaluationReportService reportService;

    public EvaluationController(EvaluationReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/latest")
    public JsonNode getLatestReport() {
        try {
            return reportService.readLatestReport();
        } catch (java.io.FileNotFoundException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No evaluation report is available yet",
                    exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not read the evaluation report",
                    exception);
        }
    }
}