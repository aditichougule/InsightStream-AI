package com.aivideoip.worker;

import com.aivideoip.entity.Video;
import com.aivideoip.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Async worker for video processing tasks
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoProcessingWorker {

    private final VideoService videoService;

    /**
     * Process video asynchronously
     */
    @Async
    public void processVideo(Long videoId) {
        try {
            log.info("Starting video processing for ID: {}", videoId);

            // Update status to downloading
            videoService.updateProcessingStatus(videoId, Video.ProcessingStatus.DOWNLOADING);
            simulateDownload();

            // Update status to transcribing
            videoService.updateProcessingStatus(videoId, Video.ProcessingStatus.TRANSCRIBING);
            simulateTranscription();

            // Update status to summarizing
            videoService.updateProcessingStatus(videoId, Video.ProcessingStatus.SUMMARIZING);
            simulateSummarization();

            // Update status to completed
            videoService.updateProcessingStatus(videoId, Video.ProcessingStatus.COMPLETED);
            log.info("Video processing completed for ID: {}", videoId);

        } catch (Exception ex) {
            log.error("Error processing video: {}", videoId, ex);
            videoService.setErrorMessage(videoId, ex.getMessage());
        }
    }

    private void simulateDownload() throws InterruptedException {
        log.info("Simulating video download...");
        Thread.sleep(2000);
    }

    private void simulateTranscription() throws InterruptedException {
        log.info("Simulating video transcription...");
        Thread.sleep(3000);
    }

    private void simulateSummarization() throws InterruptedException {
        log.info("Simulating video summarization...");
        Thread.sleep(2000);
    }
}
