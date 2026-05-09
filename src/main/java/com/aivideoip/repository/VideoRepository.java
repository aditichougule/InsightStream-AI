package com.aivideoip.repository;

import com.aivideoip.entity.Video;
import com.aivideoip.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Video entity
 */
@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    Page<Video> findByOwner(User owner, Pageable pageable);

    List<Video> findByOwnerAndActive(User owner, Boolean active);

    Page<Video> findByProcessingStatus(Video.ProcessingStatus status, Pageable pageable);

    List<Video> findByProcessingStatusAndActive(Video.ProcessingStatus status, Boolean active);
}
