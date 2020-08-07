package com.intege.mediahand.domain;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.intege.mediahand.WatchState;
import com.intege.utils.Check;

@Entity
public class SettingsEntry {

    @Id
    @GeneratedValue
    private int id;

    @Column(unique = true)
    private String profile;

    private int windowWidth;

    private int windowHeight;

    private boolean autoContinue;

    private boolean showAll;

    @Enumerated(EnumType.STRING)
    private WatchState watchState;

    public SettingsEntry(String profile, int windowWidth, int windowHeight, boolean autoContinue, boolean showAll, WatchState watchState) {
        this(0, profile, windowWidth, windowHeight, autoContinue, showAll, watchState);
    }

    public SettingsEntry(int id, String profile, int windowWidth, int windowHeight, boolean autoContinue, boolean showAll, WatchState watchState) {
        Check.notNullArgument(profile, "profile");

        this.id = id;
        this.profile = profile;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.autoContinue = autoContinue;
        this.showAll = showAll;
        this.watchState = watchState;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProfile() {
        return this.profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public int getWindowWidth() {
        return this.windowWidth;
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }

    public int getWindowHeight() {
        return this.windowHeight;
    }

    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }

    public boolean isAutoContinue() {
        return this.autoContinue;
    }

    public void setAutoContinue(final boolean autoContinue) {
        this.autoContinue = autoContinue;
    }

    public boolean isShowAll() {
        return this.showAll;
    }

    public void setShowAll(final boolean showAll) {
        this.showAll = showAll;
    }

    public WatchState getWatchState() {
        return this.watchState;
    }

    public void setWatchState(final WatchState watchState) {
        this.watchState = watchState;
    }

    public String getWatchStateValue() {
        if (this.watchState == null) {
            return "ALL";
        } else {
            return this.watchState.toString();
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SettingsEntry that = (SettingsEntry) o;
        return this.id == that.id &&
                this.windowWidth == that.windowWidth &&
                this.windowHeight == that.windowHeight &&
                this.autoContinue == that.autoContinue &&
                this.showAll == that.showAll &&
                this.profile.equals(that.profile) &&
                this.watchState == that.watchState;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.profile, this.windowWidth, this.windowHeight, this.autoContinue, this.showAll, this.watchState);
    }
}
