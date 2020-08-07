package com.intege.mediahand.domain.repository;

import org.springframework.data.repository.CrudRepository;

import com.intege.mediahand.domain.SettingsEntry;

public interface SettingsEntryRepository extends CrudRepository<SettingsEntry, Integer> {

    SettingsEntry findByProfile(String profile);

}
