package com.trustdesk.trustdesk.evaluation;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.trustdesk.trustdesk.evaluation.EvaluationReportService;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EvaluationReportServiceTest {
    @TempDir
    Path tempDirectory;

    private final JsonMapper objectMapper = new JsonMapper();

    @Test
    void readsLatestEvaluationJson() throws Exception {
        Path reportPath = tempDirectory.resolve("evaluation-summary.json");
        Files.writeString(reportPath, """
                {"total_cases":8,"triage_accuracy_percent":100.0}
                """);

        EvaluationReportService service =
                new EvaluationReportService(reportPath.toString(), objectMapper);

        var report = service.readLatestReport();

        assertEquals(8, report.get("total_cases").asInt());
        assertEquals(
                100.0,
                report.get("triage_accuracy_percent").asDouble());
    }

    @Test
    void reportsMissingEvaluationFile() {
        Path reportPath = tempDirectory.resolve("missing-report.json");
        EvaluationReportService service =
                new EvaluationReportService(reportPath.toString(), objectMapper);

        assertThrows(FileNotFoundException.class, service::readLatestReport);
    }
}