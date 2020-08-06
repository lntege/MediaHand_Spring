package com.intege.mediahand.domain.old;

import java.util.Objects;

public class DirectoryEntry {

    private int id;
    private String path;

    public DirectoryEntry(String path) {
        this.path = path;
    }

    public DirectoryEntry(int id, String path) {
        this.id = id;
        this.path = path;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DirectoryEntry that = (DirectoryEntry) o;
        return this.id == that.id &&
                this.path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.path);
    }
}
