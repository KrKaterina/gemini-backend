package com.platform.accident.submission.integration;

import com.platform.accident.enrichment.util.WeatherCodeMapper;
import com.platform.accident.submission.domain.AccidentReport;
import com.platform.integration.review.AccidentSnapshotView;
import com.platform.integration.review.AiAnalysisView; // Το Record της AI
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class AccidentSnapshotMapper {

    public AccidentSnapshotView mapToView(AccidentReport report) {
        var context = report.getContextData();
        var location = report.getLocation();

        Double rawTemp = (context != null) ? context.temperatureCelsius() : null;
        Double roundedTemp = (rawTemp != null) ? Math.round(rawTemp * 10.0) / 10.0 : null;

        Integer limit = (context != null) ? context.speedLimit() : null;
        if (limit == null || limit == 0) {
            String type = (context != null) ? context.roadType() : "residential";
            limit = switch (type.toLowerCase()) {
                case "motorway" -> 120;
                case "residential" -> 30; // Στην περίπτωσή σου ( residential) θα δείξει 30!
                default -> 50;
            };
        }

        String weatherDescription = "N/A";
        if (context != null && context.weatherCondition() != null) {
            weatherDescription = WeatherCodeMapper.translate(context.weatherCondition());
        }

        return new AccidentSnapshotView(
                report.getCaseId(),
                report.getRawDescription(),
                report.getReporterId(),
                report.getLocation() != null ? report.getLocation().lat() : 0.0,
                report.getLocation() != null ? report.getLocation().lng() : 0.0,
                weatherDescription,
                context != null ? context.roadType() : "N/A",
                report.getOccurrenceTime(),
                report.getAssetIds(),
                roundedTemp,
                location != null ? location.address() : "Unknown Location",
                limit
        );
    }

    @SuppressWarnings("unchecked")
    public AiAnalysisView mapAiInsights(AccidentReport report) {
        Map<String, Object> ai = report.getAiAnalysis();
        if (ai == null) return AiAnalysisView.empty();

        return new AiAnalysisView(
                (String) ai.getOrDefault("summary", "No summary available"),
                (String) ai.getOrDefault("severityLevel", "UNKNOWN"),
                (List<String>) ai.get("suggestedNextSteps"),
                (String) ai.getOrDefault("detailedReasoning", "")
        );
    }
}