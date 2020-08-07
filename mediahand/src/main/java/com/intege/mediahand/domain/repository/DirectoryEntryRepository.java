package com.intege.mediahand.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.intege.mediahand.domain.DirectoryEntry;

@Repository
public interface DirectoryEntryRepository extends JpaRepository<DirectoryEntry, Integer> {

    DirectoryEntry findByPath(String path);

}
