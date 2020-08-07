package com.intege.mediahand.domain.repository;

import org.springframework.data.repository.CrudRepository;

import com.intege.mediahand.domain.MediaEntry;

public interface MediaEntryRepository extends CrudRepository<MediaEntry, Integer> {

    MediaEntry findByTitle(String title);

}
