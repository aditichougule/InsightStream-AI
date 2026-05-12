package com.aivideoip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for TranscriptChunk
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptChunkDTO {
    
    private Long id;
    private Long videoId;
    private String chunkText;
    private Integer startTime;
    private Integer endTime;
    private String speaker;
    private String topic;
    private String embedding;
}
