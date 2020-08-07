package com.intege.mediahand.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.intege.mediahand.domain.MediaEntry;

public interface MediaEntryRepository extends JpaRepository<MediaEntry, Integer> {

    MediaEntry findByTitle(String title);

    @Query("SELECT DISTINCT mediaType FROM MediaEntry")
    List<String> findAllMediaTypes();

}
