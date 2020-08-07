package com.intege.mediahand.domain.repository;

import org.springframework.data.repository.CrudRepository;

import com.intege.mediahand.domain.DirectoryEntry;

public interface DirectoryEntryRepository extends CrudRepository<DirectoryEntry, Integer> {

    DirectoryEntry findByPath(String path);

}
