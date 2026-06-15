package com.aivideoip.service;

import com.aivideoip.dto.KBIndexRequest;
import com.aivideoip.dto.KBStatusDTO;
import com.aivideoip.entity.KnowledgeBase;
import com.aivideoip.entity.Video;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.KnowledgeBaseRepository;
import com.aivideoip.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceTest {
    
    @Mock
    private KnowledgeBaseRepository kbRepository;
    
    @Mock
    private VideoRepository videoRepository;
    
    @Mock
    private EmbeddingPipelineService embeddingPipelineService;
    
    @InjectMocks
    private KnowledgeBaseService kbService;
    
    private KBIndexRequest indexRequest;
    private List<Video> testVideos;
    
    @BeforeEach
    void setUp() {
        indexRequest = KBIndexRequest.builder()
                .kbName("Machine Learning Course")
                .videoIds(Set.of(1L, 2L, 3L))
                .description("Complete ML course materials")
                .build();
        
        testVideos = new ArrayList<>();
        for (long i = 1; i <= 3; i++) {
            Video video = Video.builder()
                    .title("Lecture " + i)
                    .sourceUrl("https://youtube.com/test" + i)
                    .build();
            video.setId(i);
            testVideos.add(video);
        }
    }
    
    @Test
    void testCreateAndIndexKnowledgeBase() {
        // Arrange
        when(kbRepository.findByName(indexRequest.getKbName()))
                .thenReturn(Optional.empty());
        when(videoRepository.findAllById(indexRequest.getVideoIds()))
                .thenReturn(testVideos);
        when(kbRepository.save(any(KnowledgeBase.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        
        // Act
        KBStatusDTO result = kbService.createAndIndexKnowledgeBase(indexRequest);
        
        // Assert
        assertNotNull(result);
        assertEquals("Machine Learning Course", result.getKbName());
        assertEquals(3, result.getVideoCount());
        assertTrue(result.getIndexed());
        assertEquals("ACTIVE", result.getStatus());
    }
    
    @Test
    void testCreateKnowledgeBaseDuplicateName() {
        // Arrange
        KnowledgeBase existingKb = KnowledgeBase.builder()
                .name(indexRequest.getKbName())
                .build();
        when(kbRepository.findByName(indexRequest.getKbName()))
                .thenReturn(Optional.of(existingKb));
        
        // Act & Assert
        assertThrows(IllegalStateException.class, 
                () -> kbService.createAndIndexKnowledgeBase(indexRequest));
    }
    
    @Test
    void testReindexExistingKnowledgeBase() {
        // Arrange
        indexRequest.setReindex(true);
        KnowledgeBase existingKb = KnowledgeBase.builder()
                .name(indexRequest.getKbName())
                .videos(new ArrayList<>(testVideos.subList(0, 1)))
                .indexed(true)
                .build();
        
        when(kbRepository.findByName(indexRequest.getKbName()))
                .thenReturn(Optional.of(existingKb));
        when(videoRepository.findAllById(indexRequest.getVideoIds()))
                .thenReturn(testVideos);
        when(kbRepository.save(any(KnowledgeBase.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        
        // Act
        KBStatusDTO result = kbService.createAndIndexKnowledgeBase(indexRequest);
        
        // Assert
        assertNotNull(result);
        assertEquals(3, result.getVideoCount());
    }
    
    @Test
    void testGetKnowledgeBaseStatus() {
        // Arrange
        KnowledgeBase kb = KnowledgeBase.builder()
                .name("ML Course")
                .videos(testVideos)
                .indexed(true)
                .status("ACTIVE")
                .build();
        
        when(kbRepository.findByName("ML Course"))
                .thenReturn(Optional.of(kb));
        
        // Act
        KBStatusDTO result = kbService.getKnowledgeBaseStatus("ML Course");
        
        // Assert
        assertNotNull(result);
        assertEquals("ML Course", result.getKbName());
        assertTrue(result.getIndexed());
    }
    
    @Test
    void testGetKnowledgeBaseNotFound() {
        // Arrange
        when(kbRepository.findByName("NonExistent"))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
                () -> kbService.getKnowledgeBaseStatus("NonExistent"));
    }
    
    @Test
    void testAddVideosToKnowledgeBase() {
        // Arrange
        Set<Long> newVideoIds = Set.of(4L, 5L);
        Video video4 = Video.builder().title("Lecture 4").sourceUrl("url4").build();
        video4.setId(4L);
        Video video5 = Video.builder().title("Lecture 5").sourceUrl("url5").build();
        video5.setId(5L);
        
        KnowledgeBase kb = KnowledgeBase.builder()
                .name("ML Course")
                .videos(new ArrayList<>(testVideos))
                .indexed(true)
                .build();
        
        when(kbRepository.findByName("ML Course"))
                .thenReturn(Optional.of(kb));
        when(videoRepository.findAllById(newVideoIds))
                .thenReturn(List.of(video4, video5));
        when(kbRepository.save(any(KnowledgeBase.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        
        // Act
        KBStatusDTO result = kbService.addVideosToKnowledgeBase("ML Course", newVideoIds);
        
        // Assert
        assertNotNull(result);
        assertEquals(5, result.getVideoCount());
    }
    
    @Test
    void testRemoveVideoFromKnowledgeBase() {
        // Arrange
        KnowledgeBase kb = KnowledgeBase.builder()
                .name("ML Course")
                .videos(new ArrayList<>(testVideos))
                .build();
        
        when(kbRepository.findByName("ML Course"))
                .thenReturn(Optional.of(kb));
        when(videoRepository.findById(1L))
                .thenReturn(Optional.of(testVideos.get(0)));
        when(kbRepository.save(any(KnowledgeBase.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        
        // Act
        KBStatusDTO result = kbService.removeVideoFromKnowledgeBase("ML Course", 1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.getVideoCount());
    }
    
    @Test
    void testListAllKnowledgeBases() {
        // Arrange
        List<KnowledgeBase> kbs = List.of(
            KnowledgeBase.builder().name("KB1").videos(testVideos).build(),
            KnowledgeBase.builder().name("KB2").videos(new ArrayList<>()).build()
        );
        
        when(kbRepository.findAll()).thenReturn(kbs);
        
        // Act
        List<KBStatusDTO> results = kbService.listAllKnowledgeBases();
        
        // Assert
        assertEquals(2, results.size());
    }
    
    @Test
    void testGetIndexedKnowledgeBases() {
        // Arrange
        List<KnowledgeBase> indexedKbs = List.of(
            KnowledgeBase.builder().name("KB1").indexed(true).videos(testVideos).build()
        );
        
        when(kbRepository.findByIndexedTrue()).thenReturn(indexedKbs);
        
        // Act
        List<KBStatusDTO> results = kbService.getIndexedKnowledgeBases();
        
        // Assert
        assertEquals(1, results.size());
        assertTrue(results.get(0).getIndexed());
    }
    
    @Test
    void testDeleteKnowledgeBase() {
        // Arrange
        KnowledgeBase kb = KnowledgeBase.builder().name("ML Course").build();
        when(kbRepository.findByName("ML Course"))
                .thenReturn(Optional.of(kb));
        
        // Act
        kbService.deleteKnowledgeBase("ML Course");
        
        // Assert
        verify(kbRepository, times(1)).delete(kb);
    }
}
