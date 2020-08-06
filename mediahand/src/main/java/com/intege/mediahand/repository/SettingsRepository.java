package com.intege.mediahand.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intege.mediahand.WatchState;
import com.intege.mediahand.domain.old.SettingsEntry;
import com.intege.mediahand.repository.base.BaseRepository;
import com.intege.mediahand.repository.base.Database;
import com.intege.mediahand.utils.MessageUtil;
import com.intege.utils.Check;

public class SettingsRepository implements BaseRepository<SettingsEntry> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsRepository.class);

    public SettingsRepository() {
        initTable();
    }

    private void initTable() {
        if (Database.getInstance().getStatement() != null) {
            try {
                Database.getInstance().getStatement().execute(
                        "CREATE TABLE settingsTable(ID INT IDENTITY PRIMARY KEY, PROFILE VARCHAR(255) NOT NULL UNIQUE, "
                                + "WIDTH INT NOT NULL, HEIGHT INT NOT NULL, AUTOCONTINUE BOOLEAN, SHOWALL BOOLEAN, WATCHSTATE VARCHAR(255))");
                MessageUtil.infoAlert("openSettingsTable", "Opened new settings table!");
            } catch (SQLException e) {
                try {
                    Database.getInstance().getStatement().execute("TABLE settingsTable");
                } catch (SQLException e2) {
                    MessageUtil.warningAlert(e2, "Could not open dirTable!");
                }
            }
        }
    }

    @Override
    public SettingsEntry create(SettingsEntry entry) {
        Check.notNullArgument(entry, "entry");
        try {
            SettingsEntry settingsEntry = find(entry);
            if (settingsEntry == null) {
                Database.getInstance().getStatement().execute(
                        "INSERT INTO SETTINGSTABLE (PROFILE, WIDTH, HEIGHT, AUTOCONTINUE, SHOWALL, WATCHSTATE) VALUES('"
                                + entry.getProfile() + "', '" + entry.getWindowWidth() + "', '"
                                + entry.getWindowHeight() + "', " + entry.isAutoContinue() + ", " + entry.isShowAll()
                                + ", '" + entry.getWatchState() + "')");
                return find(entry);
            } else {
                return settingsEntry;
            }
        } catch (SQLException e) {
            SettingsRepository.LOGGER.error("create", e);
        }
        return null;
    }

    @Override
    public SettingsEntry update(SettingsEntry entry) {
        Check.notNullArgument(entry, "entry");

        try {
            Database.getInstance().getStatement().execute(
                    "UPDATE SETTINGSTABLE SET PROFILE = '" + entry.getProfile() + "', WIDTH = '" +
                            entry.getWindowWidth() + "', HEIGHT = '" + entry.getWindowHeight() + "', AUTOCONTINUE = "
                            + entry.isAutoContinue() + ", SHOWALL = " + entry.isShowAll() + ", WATCHSTATE = '"
                            + entry.getWatchState() + "' WHERE ID = '" + entry.getId() + "'");
        } catch (SQLException e) {
            MessageUtil.warningAlert(e, "Could not update settings profile: " + entry.getProfile());
        }
        return find(entry);
    }

    @Override
    public void remove(SettingsEntry entry) {
    }

    @Override
    public SettingsEntry find(SettingsEntry entry) {
        Check.notNullArgument(entry, "entry");
        try (ResultSet result = Database.getInstance().getStatement().executeQuery(
                "SELECT * FROM SETTINGSTABLE WHERE PROFILE='" + entry.getProfile() + "'")) {
            if (result.next()) {
                return new SettingsEntry(result.getInt("ID"), result.getString("PROFILE"), result.getInt("WIDTH"), result.getInt("HEIGHT"), result.getBoolean("AUTOCONTINUE"), result.getBoolean("SHOWALL"), WatchState.lookupByName(result.getString("WATCHSTATE")));
            }
        } catch (SQLException e) {
            SettingsRepository.LOGGER.error("find", e);
        }
        return null;
    }

    public SettingsEntry find(final String profile) {
        return find(new SettingsEntry(profile, 0, 0, false, false, null));
    }

    @Override
    public List<SettingsEntry> findAll() {
        return null;
    }

}
