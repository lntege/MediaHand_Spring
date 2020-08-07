package com.intege.mediahand.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.intege.mediahand.domain.SettingsEntry;

public interface SettingsEntryRepository extends JpaRepository<SettingsEntry, Integer> {

    SettingsEntry findByProfile(String profile);

}
