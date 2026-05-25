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
 * Service for local LLM operations using Ollama
 * Integrates with Ollama REST API for local model inference
 * Models: llama3, mistral, qwen2.5, neural-chat, etc.
 * 
 * No API keys required - runs completely locally
 * Privacy-focused - all data stays on your machine
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OllamaLLMService {

    private final RestTemplate restTemplate;
    private final SummaryRepository summaryRepository;
    private final ActionItemRepository actionItemRepository;
    private final VideoRepository videoRepository;
    private final TranscriptChunkRepository transcriptChunkRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.ollama.enabled:true}")
    private boolean ollamaEnabled;

    @Value("${app.ollama.api-url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${app.ollama.model:llama3}")
    private String model;

    @Value("${app.ollama.max-tokens:2000}")
    private int maxTokens;

    @Value("${app.ollama.temperature:0.7}")
    private double temperature;

    @Value("${app.ollama.timeout:120}")
    private long timeout;

    /**
     * Generate comprehensive summary from transcript chunks
     * Uses local LLM model (llama3, mistral, qwen2.5, etc.)
     *
     * @param videoId the video ID
     * @param summaryType GENERAL, DETAILED, or BRIEF
     * @return created SummaryDTO
     */
    @Transactional
    public SummaryDTO generateSummary(Long videoId, String summaryType) {
        log.info("Generating {} summary for video: {} using model: {}", summaryType, videoId, model);

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));

        List<TranscriptChunk> chunks = transcriptChunkRepository.findByVideoIdOrderByStartTime(videoId);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No transcript chunks found for video");
        }

        String fullTranscript = chunks.stream()
                .map(TranscriptChunk::getChunkText)
                .collect(Collectors.joining(" "));

        SummaryResponse summaryResponse = generateSummaryResponse(fullTranscript, summaryType, chunks);

        summaryRepository.findByVideoId(videoId).ifPresent(s -> summaryRepository.delete(s));

        Summary summary = Summary.builder()
                .video(video)
                .summaryText(summaryResponse.getSummaryText())
                .keyPoints(summaryResponse.getKeyPointsJson())
                .summaryType(Summary.SummaryType.valueOf(summaryType))
                .build();

        Summary saved = summaryRepository.save(summary);
        log.info("Summary generated successfully for video: {} using {}", videoId, model);

        return mapSummaryToDTO(saved);
    }

    /**
     * Extract action items from transcript using local LLM
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

        List<ActionItemExtraction> extractions = extractActionItemsResponse(fullTranscript, chunks);

        actionItemRepository.deleteByVideoId(videoId);

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
     * Generate chunk-level summaries
     *
     * @param videoId the video ID
     * @return list of chunk summaries
     */
    @Transactional
    public List<Map<String, Object>> generateChunkSummaries(Long videoId) {
        log.info("Generating chunk-level summaries for video: {}", videoId);

        if (!videoRepository.existsById(videoId)) {
            throw new ResourceNotFoundException("Video not found");
        }

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
     * Extract entities from transcript
     *
     * @param videoId the video ID
     * @return map of extracted entities
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
     *
     * @param videoId the video ID
     * @param numQuestions number of pairs to generate
     * @return list of Q&A pairs
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
     * Generate highlights from transcript
     *
     * @param videoId the video ID
     * @return list of highlights
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
     * Analyze sentiment of transcript
     *
     * @param videoId the video ID
     * @return sentiment analysis results
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

    /**
     * Get list of available models installed in Ollama
     *
     * @return list of available models
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAvailableModels() {
        log.debug("Fetching available Ollama models from: {}", ollamaUrl);
        
        try {
            String url = ollamaUrl + "/api/tags";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("models")) {
                return (List<Map<String, Object>>) response.get("models");
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error fetching available models", e);
            return new ArrayList<>();
        }
    }

    /**
     * Check Ollama service health
     *
     * @return true if service is running, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean checkServiceHealth() {
        try {
            String url = ollamaUrl + "/api/tags";
            restTemplate.getForObject(url, Map.class);
            log.info("Ollama service is healthy at: {}", ollamaUrl);
            return true;
        } catch (Exception e) {
            log.error("Ollama service is not responding at: {}", ollamaUrl);
            return false;
        }
    }

    // ============= Private Helper Methods =============

    private SummaryResponse generateSummaryResponse(String transcript, String summaryType,
                                                     List<TranscriptChunk> chunks) {
        String prompt = buildSummaryPrompt(transcript, summaryType);
        String response = callOllamaAPI(prompt);
        return parseSummaryResponse(response, chunks);
    }

    private List<ActionItemExtraction> extractActionItemsResponse(String transcript,
                                                                  List<TranscriptChunk> chunks) {
        String prompt = buildActionItemsPrompt(transcript);
        String response = callOllamaAPI(prompt);
        return parseActionItemsResponse(response, chunks);
    }

    private String generateChunkSummaryResponse(String chunkText) {
        String prompt = "Summarize the following text in 2-3 sentences:\n\n" + chunkText;
        return callOllamaAPI(prompt);
    }

    private Map<String, List<String>> extractEntitiesResponse(String transcript) {
        String prompt = buildEntityExtractionPrompt(transcript);
        String response = callOllamaAPI(prompt);
        return parseEntitiesResponse(response);
    }

    private List<Map<String, String>> generateQuestionsAnswersResponse(String transcript, int numQuestions) {
        String prompt = buildQAPrompt(transcript, numQuestions);
        String response = callOllamaAPI(prompt);
        return parseQAResponse(response);
    }

    private List<Map<String, Object>> generateHighlightsResponse(List<TranscriptChunk> chunks) {
        String prompt = buildHighlightsPrompt(chunks);
        String response = callOllamaAPI(prompt);
        return parseHighlightsResponse(response, chunks);
    }

    private Map<String, Object> analyzeSentimentResponse(String transcript) {
        String prompt = buildSentimentPrompt(transcript);
        String response = callOllamaAPI(prompt);
        return parseSentimentResponse(response);
    }

    /**
     * Call Ollama API for local LLM inference
     * Compatible with: llama3, mistral, qwen2.5, neural-chat, dolphin-mixtral, etc.
     */
    private String callOllamaAPI(String prompt) {
        if (!ollamaEnabled) {
            log.warn("Ollama service is disabled");
            return "Ollama service is disabled. Please enable it in configuration.";
        }

        try {
            log.debug("Calling Ollama API with model: {} at {}", model, ollamaUrl);

            String url = ollamaUrl + "/api/generate";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            requestBody.put("temperature", temperature);
            requestBody.put("num_predict", maxTokens);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            if (response != null && response.containsKey("response")) {
                String content = (String) response.get("response");
                log.debug("Ollama response received: {} characters", content.length());
                return content.trim();
            }

            log.error("Invalid response from Ollama API");
            return "Error: Invalid response from Ollama API";

        } catch (Exception e) {
            log.error("Error calling Ollama API", e);
            return "Error: " + e.getMessage();
        }
    }

    // ============= Prompt Building Methods =============

    private String buildSummaryPrompt(String transcript, String summaryType) {
        return switch (summaryType) {
            case "BRIEF" -> String.format(
                    "Provide a very brief 2-3 sentence summary of the following transcript:\n\n%s\n\n" +
                    "Summary:",
                    limitTranscriptLength(transcript, 2000)
            );
            case "DETAILED" -> String.format(
                    "Provide a detailed multi-paragraph summary of the following transcript, including all major points:\n\n%s\n\n" +
                    "Detailed Summary:",
                    limitTranscriptLength(transcript, 3000)
            );
            default -> String.format(
                    "Provide a comprehensive summary (4-6 paragraphs) of the following transcript:\n\n%s\n\n" +
                    "Summary:",
                    limitTranscriptLength(transcript, 3000)
            );
        };
    }

    private String buildActionItemsPrompt(String transcript) {
        return String.format(
                "Extract all action items, tasks, and deliverables from the following transcript.\n" +
                "For each item, identify: title, description, responsible person (if mentioned), priority (HIGH/MEDIUM/LOW).\n\n" +
                "Format: For each action item, write it on a new line as: TITLE | DESCRIPTION | PERSON | PRIORITY\n\n%s\n\n" +
                "Action Items:",
                limitTranscriptLength(transcript, 3000)
        );
    }

    private String buildEntityExtractionPrompt(String transcript) {
        return String.format(
                "Extract important entities from the following transcript.\n" +
                "Identify: people names, organizations, locations, important concepts, and numbers/statistics.\n\n" +
                "Format the response as:\n" +
                "PEOPLE: [comma-separated names]\n" +
                "ORGANIZATIONS: [comma-separated org names]\n" +
                "LOCATIONS: [comma-separated locations]\n" +
                "CONCEPTS: [comma-separated key concepts]\n" +
                "NUMBERS: [comma-separated statistics]\n\n%s\n\nExtracted Entities:",
                limitTranscriptLength(transcript, 3000)
        );
    }

    private String buildQAPrompt(String transcript, int numQuestions) {
        return String.format(
                "Generate %d important questions and answers based on the following transcript.\n" +
                "Questions should test understanding of key concepts and facts.\n\n" +
                "Format each Q&A as: Q: [question]\nA: [answer]\n\n%s\n\n" +
                "Questions and Answers:",
                numQuestions,
                limitTranscriptLength(transcript, 3000)
        );
    }

    private String buildHighlightsPrompt(List<TranscriptChunk> chunks) {
        StringBuilder chunkInfo = new StringBuilder();
        for (TranscriptChunk chunk : chunks) {
            chunkInfo.append(String.format("[%d-%ds] %s\n",
                    chunk.getStartTime(), chunk.getEndTime(), 
                    chunk.getChunkText().substring(0, Math.min(100, chunk.getChunkText().length()))));
        }

        return String.format(
                "Identify the most important moments from the following timestamped transcript excerpts.\n" +
                "For each highlight, provide: timestamp (in MM:SS format), brief description, and importance (HIGH/MEDIUM/LOW).\n\n" +
                "Format: HH:MM | Description | Importance\n\n%s\n\nHighlights:",
                limitTranscriptLength(chunkInfo.toString(), 2000)
        );
    }

    private String buildSentimentPrompt(String transcript) {
        return String.format(
                "Analyze the sentiment and emotional tone of the following transcript.\n" +
                "Provide: overall sentiment (POSITIVE/NEUTRAL/NEGATIVE), confidence (0-100), tone description, and emotional keywords.\n\n" +
                "Format:\n" +
                "SENTIMENT: [POSITIVE/NEUTRAL/NEGATIVE]\n" +
                "CONFIDENCE: [0-100]\n" +
                "TONE: [description]\n" +
                "KEYWORDS: [comma-separated words]\n\n%s\n\nSentiment Analysis:",
                limitTranscriptLength(transcript, 2000)
        );
    }

    // ============= Response Parsing Methods =============

    private SummaryResponse parseSummaryResponse(String response, List<TranscriptChunk> chunks) {
        try {
            List<String> keyPoints = extractKeyPoints(response);
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
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.contains("|")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 4) {
                        items.add(ActionItemExtraction.builder()
                                .title(parts[0].trim())
                                .description(parts[1].trim())
                                .assignedTo(parts[2].trim())
                                .priority(parts[3].trim())
                                .timeReference(0)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing action items response", e);
        }
        return items;
    }

    private Map<String, List<String>> parseEntitiesResponse(String response) {
        Map<String, List<String>> entities = new HashMap<>();
        try {
            String[] sections = response.split("\n");
            List<String> currentList = new ArrayList<>();
            String currentKey = "";

            for (String line : sections) {
                if (line.startsWith("PEOPLE:")) {
                    parseEntityLine(line, "PEOPLE:", entities);
                } else if (line.startsWith("ORGANIZATIONS:")) {
                    parseEntityLine(line, "ORGANIZATIONS:", entities);
                } else if (line.startsWith("LOCATIONS:")) {
                    parseEntityLine(line, "LOCATIONS:", entities);
                } else if (line.startsWith("CONCEPTS:")) {
                    parseEntityLine(line, "CONCEPTS:", entities);
                } else if (line.startsWith("NUMBERS:")) {
                    parseEntityLine(line, "NUMBERS:", entities);
                }
            }

            // Ensure all keys exist
            entities.putIfAbsent("persons", new ArrayList<>());
            entities.putIfAbsent("organizations", new ArrayList<>());
            entities.putIfAbsent("locations", new ArrayList<>());
            entities.putIfAbsent("concepts", new ArrayList<>());
            entities.putIfAbsent("numbers", new ArrayList<>());

        } catch (Exception e) {
            log.error("Error parsing entities response", e);
        }
        return entities;
    }

    private void parseEntityLine(String line, String prefix, Map<String, List<String>> entities) {
        String content = line.replace(prefix, "").trim();
        List<String> items = Arrays.asList(content.split(","));
        items.replaceAll(String::trim);

        String key = prefix.replace(":", "").toLowerCase().replace("people", "persons")
                .replace("organizations", "organizations").replace("locations", "locations")
                .replace("concepts", "concepts").replace("numbers", "numbers");
        entities.put(key, items);
    }

    private List<Map<String, String>> parseQAResponse(String response) {
        List<Map<String, String>> qaList = new ArrayList<>();
        try {
            String[] parts = response.split("Q:");
            for (String part : parts) {
                if (part.contains("A:")) {
                    String[] qaPair = part.split("A:");
                    if (qaPair.length >= 2) {
                        Map<String, String> qa = new HashMap<>();
                        qa.put("question", qaPair[0].trim());
                        qa.put("answer", qaPair[1].trim());
                        qaList.add(qa);
                    }
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
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.contains("|")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 3) {
                        Map<String, Object> highlight = new HashMap<>();
                        highlight.put("timeInSeconds", convertTimeToSeconds(parts[0].trim()));
                        highlight.put("description", parts[1].trim());
                        highlight.put("importance", parts[2].trim());
                        highlights.add(highlight);
                    }
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
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.startsWith("SENTIMENT:")) {
                    sentiment.put("overallSentiment", line.replace("SENTIMENT:", "").trim());
                } else if (line.startsWith("CONFIDENCE:")) {
                    try {
                        sentiment.put("confidence", Integer.parseInt(line.replace("CONFIDENCE:", "").trim()));
                    } catch (Exception e) {
                        sentiment.put("confidence", 50);
                    }
                } else if (line.startsWith("TONE:")) {
                    sentiment.put("tone", line.replace("TONE:", "").trim());
                } else if (line.startsWith("KEYWORDS:")) {
                    String keywords = line.replace("KEYWORDS:", "").trim();
                    sentiment.put("emotionalKeywords", Arrays.asList(keywords.split(",")));
                }
            }
        } catch (Exception e) {
            log.error("Error parsing sentiment response", e);
        }
        return sentiment;
    }

    // ============= Utility Methods =============

    private List<String> extractKeyPoints(String summaryText) {
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

    private Integer convertTimeToSeconds(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            if (parts.length >= 2) {
                int minutes = Integer.parseInt(parts[0]);
                int seconds = Integer.parseInt(parts[1]);
                return minutes * 60 + seconds;
            }
        } catch (Exception e) {
            log.debug("Could not parse time: {}", timeStr);
        }
        return 0;
    }

    private String limitTranscriptLength(String transcript, int maxLength) {
        if (transcript.length() > maxLength) {
            return transcript.substring(0, maxLength) + "...";
        }
        return transcript;
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
