package com.intege.mediahand.repository.base;

import java.util.List;

public interface BaseRepository<T> {

    T create(T entry);

    T update(T entry);

    void remove(T entry);

    T find(T entry);

    List<T> findAll();

}
