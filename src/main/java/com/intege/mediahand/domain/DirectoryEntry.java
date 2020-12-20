package com.intege.mediahand.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Entity
public @Data
class DirectoryEntry {

    @Id
    @GeneratedValue
    @Setter(AccessLevel.NONE)
    private int id;

    @Column(unique = true)
    private String path;

    protected DirectoryEntry() {
    }

    public DirectoryEntry(String path) {
        this.path = path;
    }

    public DirectoryEntry(int id, String path) {
        this.id = id;
        this.path = path;
    }

}
