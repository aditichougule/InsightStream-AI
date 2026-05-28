package com.aivideoip.service;

import com.aivideoip.dto.SummaryDTO;
import com.aivideoip.entity.Summary;
import com.aivideoip.entity.TranscriptChunk;
import com.aivideoip.entity.Video;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.SummaryRepository;
import com.aivideoip.repository.TranscriptChunkRepository;
import com.aivideoip.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Step 13 - Enhanced Summary Generation Service
 * 
 * Generates comprehensive summaries from transcript chunks using Ollama
 * Produces structured output including:
 * - Concise notes
 * - Key concepts
 * - Action items with timestamps
 * - Important timestamps
 * 
 * Uses WebClient for non-blocking async processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SummaryGenerationService {

    private final OllamaClient ollamaClient;
    private final SummaryRepository summaryRepository;
    private final VideoRepository videoRepository;
    private final TranscriptChunkRepository transcriptChunkRepository;

    /**
     * Generate comprehensive summary from video transcript
     * Includes notes, concepts, action items, and timestamps
     *
     * @param videoId the video ID
     * @return SummaryDTO with comprehensive analysis
     */
    public SummaryDTO generateComprehensiveSummary(Long videoId) {
        log.info("Generating comprehensive summary for video: {}", videoId);

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));

        List<TranscriptChunk> chunks = transcriptChunkRepository.findByVideoIdOrderByStartTime(videoId);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No transcript chunks found for video");
        }

        String fullTranscript = buildTranscriptWithTimestamps(chunks);

        // Generate summary synchronously (blocking for database save)
        String summaryResponse = ollamaClient.generateTextSync(buildComprehensivePrompt(fullTranscript));

        // Parse structured response
        ComprehensiveSummaryResult result = parseComprehensiveSummary(summaryResponse, chunks);

        // Save to database
        summaryRepository.findByVideoId(videoId).ifPresent(s -> summaryRepository.delete(s));

        Summary summary = Summary.builder()
                .video(video)
                .summaryText(result.getConciseNotes())
                .keyPoints(String.join(",", result.getKeyConcepts()))
                .summaryType(Summary.SummaryType.DETAILED)
                .metadata(result.toJson())
                .build();

        Summary saved = summaryRepository.save(summary);
        log.info("Comprehensive summary generated for video: {}", videoId);

        return mapSummaryToDTO(saved, result);
    }

    /**
     * Generate summary asynchronously using WebClient
     * Useful for non-blocking request handling
     *
     * @param videoId the video ID
     * @return Mono<SummaryDTO> for async handling
     */
    public Mono<SummaryDTO> generateComprehensiveSummaryAsync(Long videoId) {
        log.info("Generating comprehensive summary asynchronously for video: {}", videoId);

        return Mono.fromCallable(() -> {
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Video not found"));

            List<TranscriptChunk> chunks = transcriptChunkRepository.findByVideoIdOrderByStartTime(videoId);
            if (chunks.isEmpty()) {
                throw new IllegalStateException("No transcript chunks found for video");
            }

            return buildTranscriptWithTimestamps(chunks);
        })
        .flatMap(fullTranscript -> ollamaClient.generateText(buildComprehensivePrompt(fullTranscript)))
        .map(response -> {
            List<TranscriptChunk> chunks = transcriptChunkRepository.findByVideoIdOrderByStartTime(videoId);
            return parseComprehensiveSummary(response, chunks);
        })
        .flatMap(result -> Mono.fromCallable(() -> {
            summaryRepository.findByVideoId(videoId).ifPresent(s -> summaryRepository.delete(s));

            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Video not found"));

            Summary summary = Summary.builder()
                    .video(video)
                    .summaryText(result.getConciseNotes())
                    .keyPoints(String.join(",", result.getKeyConcepts()))
                    .summaryType(Summary.SummaryType.DETAILED)
                    .metadata(result.toJson())
                    .build();

            Summary saved = summaryRepository.save(summary);
            return mapSummaryToDTO(saved, result);
        }))
        .doOnError(error -> log.error("Error generating summary asynchronously", error));
    }

    /**
     * Generate summary by type: BRIEF, GENERAL, DETAILED, COMPREHENSIVE
     *
     * @param videoId the video ID
     * @param summaryType the type of summary
     * @return SummaryDTO
     */
    public SummaryDTO generateSummaryByType(Long videoId, String summaryType) {
        log.info("Generating {} summary for video: {}", summaryType, videoId);

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));

        List<TranscriptChunk> chunks = transcriptChunkRepository.findByVideoIdOrderByStartTime(videoId);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No transcript chunks found for video");
        }

        String transcript = buildTranscriptWithTimestamps(chunks);
        String prompt = buildTypeSpecificPrompt(transcript, summaryType);
        String response = ollamaClient.generateTextSync(prompt);

        summaryRepository.findByVideoId(videoId).ifPresent(s -> summaryRepository.delete(s));

        Summary summary = Summary.builder()
                .video(video)
                .summaryText(response)
                .keyPoints(String.join(",", extractKeyPoints(response)))
                .summaryType(Summary.SummaryType.valueOf(summaryType))
                .build();

        Summary saved = summaryRepository.save(summary);
        log.info("Summary ({}) generated for video: {}", summaryType, videoId);

        return mapSummaryToDTO(saved);
    }

    /**
     * Extract action items from summary
     *
     * @param videoId the video ID
     * @return list of action items with timestamps
     */
    public List<Map<String, Object>> extractActionItemsWithTimestamps(Long videoId) {
        log.info("Extracting action items with timestamps for video: {}", videoId);

        if (!videoRepository.existsById(videoId)) {
            throw new ResourceNotFoundException("Video not found");
        }

        List<TranscriptChunk> chunks = transcriptChunkRepository.findByVideoIdOrderByStartTime(videoId);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No transcript chunks found");
        }

        String transcript = buildTranscriptWithTimestamps(chunks);
        String prompt = buildActionItemsPrompt(transcript);
        String response = ollamaClient.generateTextSync(prompt);

        return parseActionItems(response, chunks);
    }

    /**
     * Extract and rank key concepts by importance
     *
     * @param videoId the video ID
     * @return list of key concepts with relevance scores
     */
    public List<Map<String, Object>> extractKeyConcepts(Long videoId) {
        log.info("Extracting key concepts for video: {}", videoId);

        if (!videoRepository.existsById(videoId)) {
            throw new ResourceNotFoundException("Video not found");
        }

        List<TranscriptChunk> chunks = transcriptChunkRepository.findByVideoIdOrderByStartTime(videoId);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No transcript chunks found");
        }

        String transcript = chunks.stream()
                .map(TranscriptChunk::getChunkText)
                .collect(Collectors.joining(" "));

        String prompt = buildKeyConceptsPrompt(transcript);
        String response = ollamaClient.generateTextSync(prompt);

        return parseKeyConcepts(response);
    }

    // ============= Private Prompt Building Methods =============

    private String buildComprehensivePrompt(String transcript) {
        return String.format("""
            Analyze the following video transcript and provide a comprehensive summary with the following structure:
            
            ## CONCISE NOTES
            Provide 3-5 bullet points with the most important information from the transcript.
            
            ## KEY CONCEPTS
            List 5-10 key concepts or topics discussed, each with a brief 1-2 sentence explanation.
            
            ## ACTION ITEMS
            Identify any action items, tasks, or decisions mentioned. Format as:
            - [TIMESTAMP] Task description | Owner (if mentioned) | Priority (HIGH/MEDIUM/LOW)
            
            ## IMPORTANT TIMESTAMPS
            Highlight important moments with timestamps and brief descriptions. Format as:
            - [MM:SS] What happened or was discussed
            
            Please be thorough but concise. Focus on actionable insights and key takeaways.
            
            TRANSCRIPT:
            %s
            
            ANALYSIS:""", limitLength(transcript, 4000));
    }

    private String buildTypeSpecificPrompt(String transcript, String summaryType) {
        return switch (summaryType) {
            case "BRIEF" -> String.format(
                    "Provide a BRIEF 2-3 sentence summary of the following transcript:\n\n%s\n\nSummary:",
                    limitLength(transcript, 2000)
            );
            case "GENERAL" -> String.format(
                    "Provide a comprehensive 4-6 paragraph summary of the following transcript:\n\n%s\n\nSummary:",
                    limitLength(transcript, 3000)
            );
            case "DETAILED" -> String.format(
                    "Provide a DETAILED summary with multiple sections (overview, key points, conclusions):\n\n%s\n\nDetailed Summary:",
                    limitLength(transcript, 3500)
            );
            default -> String.format(
                    "Summarize the following transcript:\n\n%s\n\nSummary:",
                    limitLength(transcript, 3000)
            );
        };
    }

    private String buildActionItemsPrompt(String transcript) {
        return String.format("""
            Extract all action items, tasks, and decisions from the following transcript.
            For each item, include: task description, responsible person (if mentioned), priority, and timestamp.
            
            Format each item as:
            [MM:SS] Task | Responsible | Priority (HIGH/MEDIUM/LOW)
            
            TRANSCRIPT:
            %s
            
            ACTION ITEMS:""", limitLength(transcript, 3000));
    }

    private String buildKeyConceptsPrompt(String transcript) {
        return String.format("""
            Extract the most important concepts, ideas, and terms from the following transcript.
            For each concept, provide a brief explanation and rate its importance (1-10).
            
            Format as:
            - Concept (Importance: X/10): Explanation
            
            TRANSCRIPT:
            %s
            
            KEY CONCEPTS:""", limitLength(transcript, 3000));
    }

    // ============= Private Parsing Methods =============

    private ComprehensiveSummaryResult parseComprehensiveSummary(String response,
                                                                  List<TranscriptChunk> chunks) {
        try {
            ComprehensiveSummaryResult result = new ComprehensiveSummaryResult();

            // Extract sections
            result.setConciseNotes(extractSection(response, "CONCISE NOTES", "KEY CONCEPTS"));
            result.setKeyConcepts(extractListItems(extractSection(response, "KEY CONCEPTS", "ACTION ITEMS")));
            result.setActionItems(parseActionItems(extractSection(response, "ACTION ITEMS", "IMPORTANT TIMESTAMPS"), chunks));
            result.setImportantTimestamps(parseTimestamps(extractSection(response, "IMPORTANT TIMESTAMPS", null)));

            return result;
        } catch (Exception e) {
            log.error("Error parsing comprehensive summary", e);
            return new ComprehensiveSummaryResult();
        }
    }

    private List<Map<String, Object>> parseActionItems(String actionItemsText,
                                                        List<TranscriptChunk> chunks) {
        List<Map<String, Object>> items = new ArrayList<>();

        try {
            String[] lines = actionItemsText.split("\n");
            Pattern pattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\]\\s*(.+?)\\s*\\|\\s*(.+?)\\s*\\|\\s*(.+?)(?:\\||$)");

            for (String line : lines) {
                if (line.trim().isEmpty()) continue;

                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    int minutes = Integer.parseInt(matcher.group(1));
                    int seconds = Integer.parseInt(matcher.group(2));
                    int timeInSeconds = minutes * 60 + seconds;

                    Map<String, Object> item = new HashMap<>();
                    item.put("title", matcher.group(3).trim());
                    item.put("assignedTo", matcher.group(4).trim());
                    item.put("priority", matcher.group(5).trim());
                    item.put("timeInSeconds", timeInSeconds);
                    items.add(item);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing action items", e);
        }

        return items;
    }

    private List<Map<String, Object>> parseTimestamps(String timestampsText) {
        List<Map<String, Object>> timestamps = new ArrayList<>();

        try {
            String[] lines = timestampsText.split("\n");
            Pattern pattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\]\\s*(.+)");

            for (String line : lines) {
                if (line.trim().isEmpty()) continue;

                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    int minutes = Integer.parseInt(matcher.group(1));
                    int seconds = Integer.parseInt(matcher.group(2));
                    int timeInSeconds = minutes * 60 + seconds;

                    Map<String, Object> ts = new HashMap<>();
                    ts.put("timeInSeconds", timeInSeconds);
                    ts.put("description", matcher.group(3).trim());
                    timestamps.add(ts);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing timestamps", e);
        }

        return timestamps;
    }

    private List<Map<String, Object>> parseKeyConcepts(String conceptsText) {
        List<Map<String, Object>> concepts = new ArrayList<>();

        try {
            String[] lines = conceptsText.split("\n");
            Pattern pattern = Pattern.compile("-\\s*(.+?)\\s*\\(Importance:\\s*(\\d+)/10\\):\\s*(.+)");

            for (String line : lines) {
                if (line.trim().isEmpty()) continue;

                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    Map<String, Object> concept = new HashMap<>();
                    concept.put("term", matcher.group(1).trim());
                    concept.put("importance", Integer.parseInt(matcher.group(2)));
                    concept.put("description", matcher.group(3).trim());
                    concepts.add(concept);
                } else if (line.startsWith("-")) {
                    // Fallback for simpler format
                    Map<String, Object> concept = new HashMap<>();
                    concept.put("term", line.substring(1).trim());
                    concept.put("importance", 5);
                    concepts.add(concept);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing key concepts", e);
        }

        return concepts;
    }

    // ============= Utility Methods =============

    private String buildTranscriptWithTimestamps(List<TranscriptChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        for (TranscriptChunk chunk : chunks) {
            sb.append(String.format("[%02d:%02d - %02d:%02d] %s\n",
                    chunk.getStartTime() / 60, chunk.getStartTime() % 60,
                    chunk.getEndTime() / 60, chunk.getEndTime() % 60,
                    chunk.getChunkText()
            ));
        }
        return sb.toString();
    }

    private String extractSection(String text, String startMarker, String endMarker) {
        try {
            int startIdx = text.indexOf(startMarker);
            if (startIdx == -1) return "";

            startIdx += startMarker.length();
            int endIdx = endMarker != null ? text.indexOf(endMarker, startIdx) : text.length();
            if (endIdx == -1) endIdx = text.length();

            return text.substring(startIdx, endIdx).trim();
        } catch (Exception e) {
            log.debug("Error extracting section: {} to {}", startMarker, endMarker);
            return "";
        }
    }

    private List<String> extractListItems(String text) {
        List<String> items = new ArrayList<>();
        try {
            String[] lines = text.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("-") || trimmed.startsWith("•") || trimmed.startsWith("*")) {
                    items.add(trimmed.substring(1).trim());
                }
            }
        } catch (Exception e) {
            log.debug("Error extracting list items");
        }
        return items;
    }

    private List<String> extractKeyPoints(String text) {
        List<String> keyPoints = new ArrayList<>();
        String[] sentences = text.split("\\. ");
        for (int i = 0; i < Math.min(5, sentences.length); i++) {
            String point = sentences[i].trim();
            if (point.length() > 10) {
                keyPoints.add(point);
            }
        }
        return keyPoints;
    }

    private String limitLength(String text, int maxLength) {
        if (text.length() > maxLength) {
            return text.substring(0, maxLength) + "...";
        }
        return text;
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

    private SummaryDTO mapSummaryToDTO(Summary summary, ComprehensiveSummaryResult result) {
        return SummaryDTO.builder()
                .id(summary.getId())
                .videoId(summary.getVideo().getId())
                .summaryText(summary.getSummaryText())
                .keyPoints(String.join(",", result.getKeyConcepts()))
                .summaryType(summary.getSummaryType().toString())
                .build();
    }

    // ============= Helper Class =============

    private static class ComprehensiveSummaryResult {
        private String conciseNotes;
        private List<String> keyConcepts;
        private List<Map<String, Object>> actionItems;
        private List<Map<String, Object>> importantTimestamps;

        public String getConciseNotes() { return conciseNotes; }
        public void setConciseNotes(String conciseNotes) { this.conciseNotes = conciseNotes; }

        public List<String> getKeyConcepts() { return keyConcepts; }
        public void setKeyConcepts(List<String> keyConcepts) { this.keyConcepts = keyConcepts; }

        public List<Map<String, Object>> getActionItems() { return actionItems; }
        public void setActionItems(List<Map<String, Object>> actionItems) { this.actionItems = actionItems; }

        public List<Map<String, Object>> getImportantTimestamps() { return importantTimestamps; }
        public void setImportantTimestamps(List<Map<String, Object>> importantTimestamps) {
            this.importantTimestamps = importantTimestamps;
        }

        public String toJson() {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("actionItems", actionItems);
                data.put("importantTimestamps", importantTimestamps);
                return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data);
            } catch (Exception e) {
                return "{}";
            }
        }
    }
}
