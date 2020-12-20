package com.intege.mediahand.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.intege.mediahand.WatchState;
import com.intege.utils.Check;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Entity
public @Data
class SettingsEntry {

    @Id
    @GeneratedValue
    @Setter(AccessLevel.NONE)
    private int id;

    @Column(unique = true)
    private String profile;

    private int windowWidth;

    private int windowHeight;

    private boolean autoContinue;

    private boolean showAll;

    @Enumerated(EnumType.STRING)
    private WatchState watchState;

    private int windowPositionX;

    private int windowPositionY;

    private boolean playTeaser;

    protected SettingsEntry() {
    }

    public SettingsEntry(String profile, int windowWidth, int windowHeight, boolean autoContinue, boolean showAll, WatchState watchState, int windowPositionX, int windowPositionY,
                         boolean playTeaser) {
        Check.notNullArgument(profile, "profile");

        this.profile = profile;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.autoContinue = autoContinue;
        this.showAll = showAll;
        this.watchState = watchState;
        this.windowPositionX = windowPositionX;
        this.windowPositionY = windowPositionY;
        this.playTeaser = playTeaser;
    }

    public String getWatchStateValue() {
        if (this.watchState == null) {
            return "ALL";
        } else {
            return this.watchState.toString();
        }
    }

}
