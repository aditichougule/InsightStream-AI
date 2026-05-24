package com.aivideoip.service;

import com.aivideoip.dto.SummaryDTO;
import com.aivideoip.dto.ActionItemDTO;
import com.aivideoip.entity.Summary;
import com.aivideoip.entity.ActionItem;
import com.aivideoip.entity.Video;
import com.aivideoip.entity.TranscriptChunk;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.SummaryRepository;
import com.aivideoip.repository.ActionItemRepository;
import com.aivideoip.repository.VideoRepository;
import com.aivideoip.repository.TranscriptChunkRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for LLM-based operations: summaries, key points, action items, and structured extraction
 * Integrates with OpenAI API or compatible LLM services
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LLMService {

    private final RestTemplate restTemplate;
    private final SummaryRepository summaryRepository;
    private final ActionItemRepository actionItemRepository;
    private final VideoRepository videoRepository;
    private final TranscriptChunkRepository transcriptChunkRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.llm.enabled:true}")
    private boolean llmEnabled;

    @Value("${app.llm.api-key}")
    private String apiKey;

    @Value("${app.llm.api-url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${app.llm.model:gpt-3.5-turbo}")
    private String model;

    @Value("${app.llm.max-tokens:2000}")
    private int maxTokens;

    @Value("${app.llm.temperature:0.7}")
    private double temperature;

    @Value("${app.llm.timeout:30}")
    private long timeout;

    /**
     * Generate comprehensive summary from transcript chunks
     * Creates GENERAL type summary with all key information
     *
     * @param videoId the video ID
     * @param summaryType GENERAL, DETAILED, or BRIEF
     * @return created SummaryDTO
     */
    @Transactional
    public SummaryDTO generateSummary(Long videoId, String summaryType) {
        log.info("Generating {} summary for video: {}", summaryType, videoId);

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));

        // Get all transcript chunks
        List<TranscriptChunk> chunks = transcriptChunkRepository.findByVideoIdOrderByStartTime(videoId);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No transcript chunks found for video");
        }

        // Combine all chunk text
        String fullTranscript = chunks.stream()
                .map(TranscriptChunk::getChunkText)
                .collect(Collectors.joining(" "));

        // Generate summary based on type
        SummaryResponse summaryResponse = generateSummaryResponse(fullTranscript, summaryType, chunks);

        // Delete existing summary if present
        summaryRepository.findByVideoId(videoId).ifPresent(s -> summaryRepository.delete(s));

        // Save new summary
        Summary summary = Summary.builder()
                .video(video)
                .summaryText(summaryResponse.getSummaryText())
                .keyPoints(summaryResponse.getKeyPointsJson())
                .summaryType(Summary.SummaryType.valueOf(summaryType))
                .build();

        Summary saved = summaryRepository.save(summary);
        log.info("Summary generated and saved for video: {}", videoId);

        return mapSummaryToDTO(saved);
    }

    /**
     * Extract action items from transcript
     * Identifies tasks, deadlines, and responsibilities mentioned
     *
     * @param videoId the video ID
     * @return list of created ActionItemDTOs
     */
    @Transactional
    public List<ActionItemDTO> extractActionItems(Long videoId) {
        log.info("Extracting action items from video: {}", videoId);

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));

        List<TranscriptChunk> chunks = transcriptChunkRepository.findByVideoIdOrderByStartTime(videoId);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No transcript chunks found for video");
        }

        String fullTranscript = chunks.stream()
                .map(TranscriptChunk::getChunkText)
                .collect(Collectors.joining(" "));

        // Extract action items using LLM
        List<ActionItemExtraction> extractions = extractActionItemsResponse(fullTranscript, chunks);

        // Delete existing action items for this video
        actionItemRepository.deleteByVideoId(videoId);

        // Save new action items
        List<ActionItemDTO> savedItems = new ArrayList<>();
        for (ActionItemExtraction extraction : extractions) {
            ActionItem item = ActionItem.builder()
                    .video(video)
                    .title(extraction.getTitle())
                    .description(extraction.getDescription())
                    .assignedTo(extraction.getAssignedTo())
                    .status(ActionItem.ActionStatus.PENDING)
                    .timeReference(extraction.getTimeReference())
                    .priority(extraction.getPriority())
                    .build();

            ActionItem saved = actionItemRepository.save(item);
            savedItems.add(mapActionItemToDTO(saved));
        }

        log.info("Extracted {} action items for video: {}", savedItems.size(), videoId);
        return savedItems;
    }

    /**
     * Generate summaries for individual chunks
     * Creates BRIEF summaries for each chunk
     *
     * @param videoId the video ID
     * @return list of chunk IDs and their summaries
     */
    @Transactional
    public List<Map<String, Object>> generateChunkSummaries(Long videoId) {
        log.info("Generating chunk-level summaries for video: {}", videoId);

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));

        List<TranscriptChunk> chunks = transcriptChunkRepository.findByVideoIdOrderByStartTime(videoId);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No transcript chunks found for video");
        }

        List<Map<String, Object>> chunkSummaries = new ArrayList<>();

        for (TranscriptChunk chunk : chunks) {
            String chunkSummary = generateChunkSummaryResponse(chunk.getChunkText());

            Map<String, Object> summaryData = new HashMap<>();
            summaryData.put("chunkId", chunk.getId());
            summaryData.put("startTime", chunk.getStartTime());
            summaryData.put("endTime", chunk.getEndTime());
            summaryData.put("topic", chunk.getTopic());
            summaryData.put("summary", chunkSummary);

            chunkSummaries.add(summaryData);
        }

        log.info("Generated {} chunk summaries for video: {}", chunkSummaries.size(), videoId);
        return chunkSummaries;
    }

    /**
     * Extract key entities and concepts from transcript
     * Identifies important topics, names, dates, etc.
     *
     * @param videoId the video ID
     * @return map with extracted entities by type
     */
    @Transactional(readOnly = true)
    public Map<String, List<String>> extractEntities(Long videoId) {
        log.info("Extracting entities from video: {}", videoId);

        if (!videoRepository.existsById(videoId)) {
            throw new ResourceNotFoundException("Video not found");
        }

        List<TranscriptChunk> chunks = transcriptChunkRepository.findByVideoIdOrderByStartTime(videoId);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No transcript chunks found for video");
        }

        String fullTranscript = chunks.stream()
                .map(TranscriptChunk::getChunkText)
                .collect(Collectors.joining(" "));

        return extractEntitiesResponse(fullTranscript);
    }

    /**
     * Generate Q&A pairs from transcript
     * Useful for learning and comprehension
     *
     * @param videoId the video ID
     * @param numQuestions number of Q&A pairs to generate
     * @return list of question-answer pairs
     */
    @Transactional(readOnly = true)
    public List<Map<String, String>> generateQuestionsAnswers(Long videoId, int numQuestions) {
        log.info("Generating {} Q&A pairs for video: {}", numQuestions, videoId);

        if (!videoRepository.existsById(videoId)) {
            throw new ResourceNotFoundException("Video not found");
        }

        List<TranscriptChunk> chunks = transcriptChunkRepository.findByVideoIdOrderByStartTime(videoId);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No transcript chunks found for video");
        }

        String fullTranscript = chunks.stream()
                .map(TranscriptChunk::getChunkText)
                .collect(Collectors.joining(" "));

        return generateQuestionsAnswersResponse(fullTranscript, numQuestions);
    }

    /**
     * Generate timestamps/highlights for important moments
     * Identifies key moments that should be bookmarked
     *
     * @param videoId the video ID
     * @return list of important timestamps with descriptions
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> generateHighlights(Long videoId) {
        log.info("Generating highlights for video: {}", videoId);

        if (!videoRepository.existsById(videoId)) {
            throw new ResourceNotFoundException("Video not found");
        }

        List<TranscriptChunk> chunks = transcriptChunkRepository.findByVideoIdOrderByStartTime(videoId);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No transcript chunks found for video");
        }

        return generateHighlightsResponse(chunks);
    }

    /**
     * Perform sentiment analysis on transcript
     *
     * @param videoId the video ID
     * @return sentiment metrics and breakdown by segment
     */
    @Transactional(readOnly = true)
    public Map<String, Object> analyzeSentiment(Long videoId) {
        log.info("Analyzing sentiment for video: {}", videoId);

        if (!videoRepository.existsById(videoId)) {
            throw new ResourceNotFoundException("Video not found");
        }

        List<TranscriptChunk> chunks = transcriptChunkRepository.findByVideoIdOrderByStartTime(videoId);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No transcript chunks found for video");
        }

        String fullTranscript = chunks.stream()
                .map(TranscriptChunk::getChunkText)
                .collect(Collectors.joining(" "));

        return analyzeSentimentResponse(fullTranscript);
    }

    // ============= Private Helper Methods =============

    private SummaryResponse generateSummaryResponse(String transcript, String summaryType, 
                                                     List<TranscriptChunk> chunks) {
        String prompt = buildSummaryPrompt(transcript, summaryType);
        String response = callLLMAPI(prompt);
        return parseSummaryResponse(response, chunks);
    }

    private List<ActionItemExtraction> extractActionItemsResponse(String transcript, 
                                                                  List<TranscriptChunk> chunks) {
        String prompt = buildActionItemsPrompt(transcript);
        String response = callLLMAPI(prompt);
        return parseActionItemsResponse(response, chunks);
    }

    private String generateChunkSummaryResponse(String chunkText) {
        String prompt = "Summarize the following text in 2-3 sentences:\n\n" + chunkText;
        return callLLMAPI(prompt);
    }

    private Map<String, List<String>> extractEntitiesResponse(String transcript) {
        String prompt = buildEntityExtractionPrompt(transcript);
        String response = callLLMAPI(prompt);
        return parseEntitiesResponse(response);
    }

    private List<Map<String, String>> generateQuestionsAnswersResponse(String transcript, int numQuestions) {
        String prompt = buildQAPrompt(transcript, numQuestions);
        String response = callLLMAPI(prompt);
        return parseQAResponse(response);
    }

    private List<Map<String, Object>> generateHighlightsResponse(List<TranscriptChunk> chunks) {
        String prompt = buildHighlightsPrompt(chunks);
        String response = callLLMAPI(prompt);
        return parseHighlightsResponse(response, chunks);
    }

    private Map<String, Object> analyzeSentimentResponse(String transcript) {
        String prompt = buildSentimentPrompt(transcript);
        String response = callLLMAPI(prompt);
        return parseSentimentResponse(response);
    }

    /**
     * Call OpenAI API or compatible LLM service
     */
    private String callLLMAPI(String prompt) {
        if (!llmEnabled) {
            log.warn("LLM service is disabled");
            return "LLM service is disabled. Please enable it in configuration.";
        }

        try {
            log.debug("Calling LLM API with prompt of {} characters", prompt.length());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", "You are a helpful assistant that analyzes and summarizes transcripts."),
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            Map<String, Object> response = restTemplate.postForObject(apiUrl, entity, Map.class);

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    String content = (String) message.get("content");
                    log.debug("LLM API response received: {} characters", content.length());
                    return content;
                }
            }

            log.error("Invalid response from LLM API");
            return "Error: Invalid response from LLM API";

        } catch (Exception e) {
            log.error("Error calling LLM API", e);
            return "Error: " + e.getMessage();
        }
    }

    // ============= Prompt Building Methods =============

    private String buildSummaryPrompt(String transcript, String summaryType) {
        return switch (summaryType) {
            case "BRIEF" -> String.format(
                    "Provide a very brief 2-3 sentence summary of the following transcript:\n\n%s\n\n" +
                    "Provide only the summary, no explanations.",
                    transcript
            );
            case "DETAILED" -> String.format(
                    "Provide a detailed multi-paragraph summary of the following transcript, including all major points:\n\n%s\n\n" +
                    "Format: Clear paragraphs with main topics highlighted. Include key statistics and examples.",
                    transcript
            );
            default -> String.format(
                    "Provide a comprehensive summary (4-6 paragraphs) of the following transcript:\n\n%s\n\n" +
                    "Include main topics, key points, and important conclusions.",
                    transcript
            );
        };
    }

    private String buildActionItemsPrompt(String transcript) {
        return String.format(
                "Extract all action items, tasks, and deliverables from the following transcript.\n" +
                "For each item, identify: title, description, responsible person (if mentioned), priority (HIGH/MEDIUM/LOW), and timestamp (approximate time in format MM:SS).\n\n" +
                "Format your response as a JSON array with objects containing: title, description, assignedTo, priority, timeReference.\n\n%s\n\n" +
                "Return ONLY valid JSON array, no other text.",
                transcript
        );
    }

    private String buildEntityExtractionPrompt(String transcript) {
        return String.format(
                "Extract important entities from the following transcript.\n" +
                "Categorize them as: PERSON, ORGANIZATION, LOCATION, CONCEPT, DATE, NUMBER.\n\n" +
                "Format your response as a JSON object with these keys: persons, organizations, locations, concepts, dates, numbers.\n" +
                "Each value should be an array of unique strings.\n\n%s\n\n" +
                "Return ONLY valid JSON, no other text.",
                transcript
        );
    }

    private String buildQAPrompt(String transcript, int numQuestions) {
        return String.format(
                "Generate %d important questions and answers based on the following transcript.\n" +
                "Questions should test understanding of key concepts and facts.\n\n" +
                "Format your response as a JSON array with objects containing: question, answer.\n\n%s\n\n" +
                "Return ONLY valid JSON array, no other text.",
                numQuestions, transcript
        );
    }

    private String buildHighlightsPrompt(List<TranscriptChunk> chunks) {
        StringBuilder chunkInfo = new StringBuilder();
        for (TranscriptChunk chunk : chunks) {
            chunkInfo.append(String.format("[%d-%ds] %s\n",
                    chunk.getStartTime(), chunk.getEndTime(), chunk.getChunkText()));
        }

        return String.format(
                "Identify the most important moments and highlights from the following timestamped transcript.\n" +
                "For each highlight, provide: timestamp (in MM:SS format), description, and importance level (HIGH/MEDIUM/LOW).\n\n" +
                "Format as JSON array with objects: timestamp, description, importance.\n\n%s\n\n" +
                "Return ONLY valid JSON array, no other text.",
                chunkInfo.toString()
        );
    }

    private String buildSentimentPrompt(String transcript) {
        return String.format(
                "Analyze the sentiment of the following transcript.\n" +
                "Provide: overall_sentiment (POSITIVE/NEUTRAL/NEGATIVE), confidence (0-100), tone, and emotional_keywords.\n\n" +
                "Format as JSON object with these keys: overall_sentiment, confidence, tone, emotional_keywords (array).\n\n%s\n\n" +
                "Return ONLY valid JSON, no other text.",
                transcript
        );
    }

    // ============= Response Parsing Methods =============

    private SummaryResponse parseSummaryResponse(String response, List<TranscriptChunk> chunks) {
        try {
            List<String> keyPoints = extractKeyPoints(response, chunks);
            return new SummaryResponse(response, keyPoints);
        } catch (Exception e) {
            log.error("Error parsing summary response", e);
            return new SummaryResponse(response, List.of());
        }
    }

    private List<ActionItemExtraction> parseActionItemsResponse(String response, 
                                                                List<TranscriptChunk> chunks) {
        List<ActionItemExtraction> items = new ArrayList<>();
        try {
            // Try to parse as JSON
            if (response.trim().startsWith("[")) {
                JsonNode jsonArray = objectMapper.readTree(response);
                for (JsonNode node : jsonArray) {
                    items.add(ActionItemExtraction.builder()
                            .title(node.get("title").asText())
                            .description(node.get("description").asText(""))
                            .assignedTo(node.get("assignedTo").asText(""))
                            .priority(node.get("priority").asText("MEDIUM"))
                            .timeReference(findTimeReference(node.get("timeReference").asText("0:00"), chunks))
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Error parsing action items response", e);
            // Return empty list on parse error
        }
        return items;
    }

    private Map<String, List<String>> parseEntitiesResponse(String response) {
        Map<String, List<String>> entities = new HashMap<>();
        try {
            if (response.trim().startsWith("{")) {
                JsonNode jsonObject = objectMapper.readTree(response);
                entities.put("persons", parseJsonArray(jsonObject.get("persons")));
                entities.put("organizations", parseJsonArray(jsonObject.get("organizations")));
                entities.put("locations", parseJsonArray(jsonObject.get("locations")));
                entities.put("concepts", parseJsonArray(jsonObject.get("concepts")));
                entities.put("dates", parseJsonArray(jsonObject.get("dates")));
                entities.put("numbers", parseJsonArray(jsonObject.get("numbers")));
            }
        } catch (Exception e) {
            log.error("Error parsing entities response", e);
        }
        return entities;
    }

    private List<Map<String, String>> parseQAResponse(String response) {
        List<Map<String, String>> qaList = new ArrayList<>();
        try {
            if (response.trim().startsWith("[")) {
                JsonNode jsonArray = objectMapper.readTree(response);
                for (JsonNode node : jsonArray) {
                    Map<String, String> qa = new HashMap<>();
                    qa.put("question", node.get("question").asText());
                    qa.put("answer", node.get("answer").asText());
                    qaList.add(qa);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Q&A response", e);
        }
        return qaList;
    }

    private List<Map<String, Object>> parseHighlightsResponse(String response, List<TranscriptChunk> chunks) {
        List<Map<String, Object>> highlights = new ArrayList<>();
        try {
            if (response.trim().startsWith("[")) {
                JsonNode jsonArray = objectMapper.readTree(response);
                for (JsonNode node : jsonArray) {
                    Map<String, Object> highlight = new HashMap<>();
                    String timeStr = node.get("timestamp").asText("0:00");
                    highlight.put("timeInSeconds", convertTimeToSeconds(timeStr));
                    highlight.put("description", node.get("description").asText());
                    highlight.put("importance", node.get("importance").asText("MEDIUM"));
                    highlights.add(highlight);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing highlights response", e);
        }
        return highlights;
    }

    private Map<String, Object> parseSentimentResponse(String response) {
        Map<String, Object> sentiment = new HashMap<>();
        try {
            if (response.trim().startsWith("{")) {
                JsonNode jsonObject = objectMapper.readTree(response);
                sentiment.put("overallSentiment", jsonObject.get("overall_sentiment").asText("NEUTRAL"));
                sentiment.put("confidence", jsonObject.get("confidence").asInt(50));
                sentiment.put("tone", jsonObject.get("tone").asText(""));
                sentiment.put("emotionalKeywords", parseJsonArray(jsonObject.get("emotional_keywords")));
            }
        } catch (Exception e) {
            log.error("Error parsing sentiment response", e);
        }
        return sentiment;
    }

    // ============= Utility Methods =============

    private List<String> extractKeyPoints(String summaryText, List<TranscriptChunk> chunks) {
        List<String> keyPoints = new ArrayList<>();
        String[] sentences = summaryText.split("\\. ");
        for (int i = 0; i < Math.min(5, sentences.length); i++) {
            String point = sentences[i].trim();
            if (point.length() > 10) {
                keyPoints.add(point);
            }
        }
        return keyPoints;
    }

    private List<String> parseJsonArray(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> list.add(item.asText()));
        }
        return list;
    }

    private Integer findTimeReference(String timeStr, List<TranscriptChunk> chunks) {
        try {
            return convertTimeToSeconds(timeStr);
        } catch (Exception e) {
            return chunks.isEmpty() ? 0 : chunks.get(0).getStartTime();
        }
    }

    private Integer convertTimeToSeconds(String timeStr) {
        String[] parts = timeStr.split(":");
        if (parts.length >= 2) {
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            return minutes * 60 + seconds;
        }
        return 0;
    }

    private SummaryDTO mapSummaryToDTO(Summary summary) {
        return SummaryDTO.builder()
                .id(summary.getId())
                .videoId(summary.getVideo().getId())
                .summaryText(summary.getSummaryText())
                .keyPoints(summary.getKeyPoints())
                .summaryType(summary.getSummaryType().toString())
                .build();
    }

    private ActionItemDTO mapActionItemToDTO(ActionItem item) {
        return ActionItemDTO.builder()
                .id(item.getId())
                .videoId(item.getVideo().getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .assignedTo(item.getAssignedTo())
                .status(item.getStatus().toString())
                .timeReference(item.getTimeReference())
                .priority(item.getPriority())
                .build();
    }

    // ============= Helper Classes =============

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class SummaryResponse {
        private String summaryText;
        private List<String> keyPoints;

        public String getKeyPointsJson() {
            try {
                return new ObjectMapper().writeValueAsString(keyPoints);
            } catch (Exception e) {
                return "[]";
            }
        }
    }

    @lombok.Data
    @lombok.Builder
    private static class ActionItemExtraction {
        private String title;
        private String description;
        private String assignedTo;
        private String priority;
        private Integer timeReference;
    }
}
