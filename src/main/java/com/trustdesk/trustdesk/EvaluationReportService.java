package com.trustdesk.trustdesk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EvaluationReportService {
    private final Path reportPath;
    private final JsonMapper objectMapper;

    public EvaluationReportService(
            @Value("${trustdesk.evaluation.report-path:"
                    + "ai-service/reports/evaluation-summary.json}")
            String reportPath,
            JsonMapper objectMapper) {
        this.reportPath = Path.of(reportPath).toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
    }

    public JsonNode readLatestReport() throws IOException {
        if (!Files.isRegularFile(reportPath)) {
            throw new java.io.FileNotFoundException(
                    "Evaluation report not found: " + reportPath);
        }

        JsonNode report = objectMapper.readTree(reportPath.toFile());
        if (report == null || !report.isObject()) {
            throw new IOException("Evaluation report must contain a JSON object");
        }

        return report;
    }
}