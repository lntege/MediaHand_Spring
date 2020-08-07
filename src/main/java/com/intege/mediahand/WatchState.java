package com.intege.mediahand;

import java.util.HashMap;
import java.util.Map;

public enum WatchState {

    DOWNLOADING, WATCHING, WATCHED, WANT_TO_WATCH, REWATCHING, ALL;

    private static final Map<String, WatchState> nameIndex = new HashMap<>(WatchState.values().length);

    static {
        for (WatchState state : WatchState.values()) {
            nameIndex.put(state.name(), state);
        }
    }

    public static WatchState lookupByName(String name) {
        return nameIndex.get(name);
    }

}
