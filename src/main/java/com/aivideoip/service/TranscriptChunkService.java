package com.aivideoip.service;

import com.aivideoip.dto.TranscriptChunkDTO;
import com.aivideoip.entity.TranscriptChunk;
import com.aivideoip.entity.Video;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.TranscriptChunkRepository;
import com.aivideoip.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Service for managing transcript chunks
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TranscriptChunkService {

    private final TranscriptChunkRepository chunkRepository;
    private final VideoRepository videoRepository;

    @Value("${app.chunking.min-size:50}")
    private int minChunkSize;

    @Value("${app.chunking.target-size:500}")
    private int targetChunkSize;

    @Value("${app.chunking.max-size:1000}")
    private int maxChunkSize;

    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[.!?]+\\s+");
    private static final Pattern WORD_PATTERN = Pattern.compile("\\s+");

    /**
     * Intelligently chunk transcript text based on:
     * 1. Token/word count (target size with min/max bounds)
     * 2. Semantic boundaries (sentences, paragraphs)
     * 3. Timestamps (preserve natural boundaries)
     * 
     * @param videoId the video ID
     * @param fullTranscript the complete transcript text
     * @param startTime initial timestamp in seconds
     * @param endTime final timestamp in seconds
     * @return list of created TranscriptChunkDTOs
     */
    @Transactional
    public List<TranscriptChunkDTO> performSemanticChunking(Long videoId, String fullTranscript, 
                                                            Integer startTime, Integer endTime) {
        log.info("Performing semantic chunking for video: {}, transcript length: {}", videoId, fullTranscript.length());
        
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));

        // Clear existing chunks for this video
        chunkRepository.deleteByVideoId(videoId);
        
        // Perform intelligent chunking
        List<ChunkSegment> segments = chunkTranscript(fullTranscript);
        log.debug("Created {} semantic segments from transcript", segments.size());

        // Estimate timestamps for each chunk based on word count distribution
        distributeTimestamps(segments, startTime, endTime);

        // Save chunks to database
        List<TranscriptChunkDTO> savedChunks = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            ChunkSegment segment = segments.get(i);
            
            TranscriptChunk chunk = TranscriptChunk.builder()
                    .video(video)
                    .chunkText(segment.getText())
                    .startTime(segment.getStartTime())
                    .endTime(segment.getEndTime())
                    .topic(extractTopic(segment.getText()))
                    .build();

            TranscriptChunk saved = chunkRepository.save(chunk);
            savedChunks.add(mapToDTO(saved));
            log.debug("Saved chunk {}: {} words, {} to {} seconds", 
                    i + 1, countWords(segment.getText()), segment.getStartTime(), segment.getEndTime());
        }

        log.info("Semantic chunking completed: {} chunks created for video {}", savedChunks.size(), videoId);
        return savedChunks;
    }

    /**
     * Intelligently chunk text based on word count and semantic boundaries
     */
    private List<ChunkSegment> chunkTranscript(String text) {
        List<ChunkSegment> chunks = new ArrayList<>();
        
        // Split by sentences first
        String[] sentences = SENTENCE_PATTERN.split(text);
        
        StringBuilder currentChunk = new StringBuilder();
        int currentWordCount = 0;
        
        for (String sentence : sentences) {
            if (sentence.trim().isEmpty()) continue;
            
            int sentenceWordCount = countWords(sentence);
            
            // Check if adding this sentence would exceed max size
            if (currentWordCount + sentenceWordCount > maxChunkSize && currentWordCount > 0) {
                // Save current chunk and start a new one
                chunks.add(new ChunkSegment(currentChunk.toString().trim()));
                currentChunk = new StringBuilder();
                currentWordCount = 0;
            }
            
            // Add sentence to current chunk
            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
            }
            currentChunk.append(sentence.trim());
            currentWordCount += sentenceWordCount;
            
            // If we've reached target size or exceeded it slightly, start new chunk
            if (currentWordCount >= targetChunkSize) {
                chunks.add(new ChunkSegment(currentChunk.toString().trim()));
                currentChunk = new StringBuilder();
                currentWordCount = 0;
            }
        }
        
        // Don't forget the last chunk
        if (currentChunk.length() > minChunkSize) {
            chunks.add(new ChunkSegment(currentChunk.toString().trim()));
        } else if (currentChunk.length() > 0 && !chunks.isEmpty()) {
            // Merge small remaining text with last chunk
            ChunkSegment lastChunk = chunks.get(chunks.size() - 1);
            lastChunk.setText(lastChunk.getText() + " " + currentChunk.toString().trim());
        }
        
        return chunks;
    }

    /**
     * Distribute timestamps across chunks proportionally based on word count
     */
    private void distributeTimestamps(List<ChunkSegment> segments, Integer startTime, Integer endTime) {
        if (segments.isEmpty()) return;
        
        // Calculate total words
        int totalWords = segments.stream()
                .mapToInt(s -> countWords(s.getText()))
                .sum();
        
        if (totalWords == 0) totalWords = 1; // Avoid division by zero
        
        int totalDuration = endTime - startTime;
        int currentTime = startTime;
        
        for (int i = 0; i < segments.size(); i++) {
            ChunkSegment segment = segments.get(i);
            int segmentWords = countWords(segment.getText());
            
            // Last segment gets remaining time
            int segmentDuration;
            if (i == segments.size() - 1) {
                segmentDuration = endTime - currentTime;
            } else {
                segmentDuration = Math.round(((float) segmentWords / totalWords) * totalDuration);
            }
            
            segment.setStartTime(currentTime);
            segment.setEndTime(currentTime + segmentDuration);
            currentTime += segmentDuration;
        }
    }

    /**
     * Extract topic/title from text (first 5-10 words or up to first sentence)
     */
    private String extractTopic(String text) {
        String[] words = WORD_PATTERN.split(text.trim());
        
        // Take up to 10 words or up to first sentence
        int wordCount = Math.min(10, words.length);
        String[] topicWords = new String[wordCount];
        System.arraycopy(words, 0, topicWords, 0, wordCount);
        
        String topic = String.join(" ", topicWords);
        
        // Remove trailing punctuation
        return topic.replaceAll("[.!?]*$", "");
    }

    /**
     * Count words in text
     */
    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        return WORD_PATTERN.split(text.trim()).length;
    }

    /**
     * Get chunking statistics for a video
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getChunkingStatistics(Long videoId) {
        log.debug("Fetching chunking statistics for video: {}", videoId);
        
        if (!videoRepository.existsById(videoId)) {
            throw new ResourceNotFoundException("Video not found");
        }

        List<TranscriptChunk> chunks = chunkRepository.findByVideoIdOrderByStartTime(videoId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("videoId", videoId);
        stats.put("totalChunks", chunks.size());
        
        int totalWords = 0;
        int totalDuration = 0;
        int minSize = Integer.MAX_VALUE;
        int maxSize = 0;
        
        for (TranscriptChunk chunk : chunks) {
            int wordCount = countWords(chunk.getChunkText());
            totalWords += wordCount;
            minSize = Math.min(minSize, wordCount);
            maxSize = Math.max(maxSize, wordCount);
            totalDuration = chunk.getEndTime(); // Last chunk's end time
        }
        
        stats.put("totalWords", totalWords);
        stats.put("averageWordsPerChunk", chunks.isEmpty() ? 0 : totalWords / chunks.size());
        stats.put("minChunkSize", minSize == Integer.MAX_VALUE ? 0 : minSize);
        stats.put("maxChunkSize", maxSize);
        stats.put("totalDurationSeconds", totalDuration);
        
        return stats;
    }

    /**
     * Search chunks by keyword (full-text search)
     */
    @Transactional(readOnly = true)
    public List<TranscriptChunkDTO> searchChunks(Long videoId, String keyword) {
        log.debug("Searching chunks in video {} for keyword: {}", videoId, keyword);
        
        if (!videoRepository.existsById(videoId)) {
            throw new ResourceNotFoundException("Video not found");
        }

        List<TranscriptChunk> chunks = chunkRepository.findByVideoIdOrderByStartTime(videoId);
        String keywordLower = keyword.toLowerCase();
        
        return chunks.stream()
                .filter(chunk -> chunk.getChunkText().toLowerCase().contains(keywordLower))
                .map(this::mapToDTO)
                .toList();
    }

    public TranscriptChunkDTO createChunk(Long videoId, TranscriptChunkDTO dto) {
        log.info("Creating transcript chunk for video: {}", videoId);
        
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));

        TranscriptChunk chunk = TranscriptChunk.builder()
                .video(video)
                .chunkText(dto.getChunkText())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .speaker(dto.getSpeaker())
                .topic(dto.getTopic())
                .embedding(dto.getEmbedding())
                .build();

        TranscriptChunk saved = chunkRepository.save(chunk);
        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<TranscriptChunkDTO> getVideoChunks(Long videoId, Pageable pageable) {
        log.debug("Fetching chunks for video: {}", videoId);
        
        if (!videoRepository.existsById(videoId)) {
            throw new ResourceNotFoundException("Video not found");
        }

        Page<TranscriptChunk> chunks = chunkRepository.findByVideoId(videoId, pageable);
        List<TranscriptChunkDTO> dtos = chunks.getContent().stream()
                .map(this::mapToDTO)
                .toList();

        return new PageImpl<>(dtos, pageable, chunks.getTotalElements());
    }

    @Transactional(readOnly = true)
    public TranscriptChunkDTO getChunkById(Long chunkId) {
        log.debug("Fetching chunk: {}", chunkId);
        
        return chunkRepository.findById(chunkId)
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Transcript chunk not found"));
    }

    public TranscriptChunkDTO updateChunk(Long chunkId, TranscriptChunkDTO dto) {
        log.info("Updating chunk: {}", chunkId);
        
        TranscriptChunk chunk = chunkRepository.findById(chunkId)
                .orElseThrow(() -> new ResourceNotFoundException("Transcript chunk not found"));

        chunk.setChunkText(dto.getChunkText());
        chunk.setStartTime(dto.getStartTime());
        chunk.setEndTime(dto.getEndTime());
        chunk.setSpeaker(dto.getSpeaker());
        chunk.setTopic(dto.getTopic());
        chunk.setEmbedding(dto.getEmbedding());

        TranscriptChunk updated = chunkRepository.save(chunk);
        return mapToDTO(updated);
    }

    public void deleteChunk(Long chunkId) {
        log.info("Deleting chunk: {}", chunkId);
        
        if (!chunkRepository.existsById(chunkId)) {
            throw new ResourceNotFoundException("Transcript chunk not found");
        }
        
        chunkRepository.deleteById(chunkId);
    }

    private TranscriptChunkDTO mapToDTO(TranscriptChunk chunk) {
        return TranscriptChunkDTO.builder()
                .id(chunk.getId())
                .videoId(chunk.getVideo().getId())
                .chunkText(chunk.getChunkText())
                .startTime(chunk.getStartTime())
                .endTime(chunk.getEndTime())
                .speaker(chunk.getSpeaker())
                .topic(chunk.getTopic())
                .embedding(chunk.getEmbedding())
                .build();
    }
    
    /**
     * Helper class to represent a text segment during chunking
     */
    private static class ChunkSegment {
        private String text;
        private Integer startTime;
        private Integer endTime;

        public ChunkSegment(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Integer getStartTime() {
            return startTime;
        }

        public void setStartTime(Integer startTime) {
            this.startTime = startTime;
        }

        public Integer getEndTime() {
            return endTime;
        }

        public void setEndTime(Integer endTime) {
            this.endTime = endTime;
        }
    }
}
