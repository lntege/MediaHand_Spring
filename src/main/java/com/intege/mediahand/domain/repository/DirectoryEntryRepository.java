package com.intege.mediahand.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.intege.mediahand.domain.DirectoryEntry;

@Repository
public interface DirectoryEntryRepository extends JpaRepository<DirectoryEntry, Integer> {

    DirectoryEntry findByPath(String path);

    @Query("SELECT DISTINCT path FROM DirectoryEntry")
    List<String> findAllBasePaths();

}
