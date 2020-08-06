package com.intege.mediahand.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intege.mediahand.domain.old.DirectoryEntry;
import com.intege.mediahand.repository.base.BaseRepository;
import com.intege.mediahand.repository.base.Database;
import com.intege.mediahand.utils.MessageUtil;
import com.intege.utils.Check;

public class BasePathRepository implements BaseRepository<DirectoryEntry> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasePathRepository.class);

    public BasePathRepository() {
        initTable();
    }

    /**
     * Creates or opens the directory table to allow writing and reading.
     */
    private void initTable() {
        if (Database.getInstance().getStatement() != null) {
            try {
                Database.getInstance().getStatement().execute("CREATE TABLE dirTable(ID INT IDENTITY PRIMARY KEY, " +
                        "PATH VARCHAR(255) NOT NULL)");
                MessageUtil.infoAlert("openDirTable", "Opened new directory table!");
            } catch (SQLException e) {
                try {
                    Database.getInstance().getStatement().execute("TABLE dirTable");
                } catch (SQLException e2) {
                    MessageUtil.warningAlert(e2, "Could not open dirTable!");
                }
            }
        }
    }

    @Override
    public DirectoryEntry create(DirectoryEntry entry) {
        Check.notNullArgument(entry, "entry");
        try {
            DirectoryEntry directoryEntry = find(entry);
            if (directoryEntry == null) {
                Database.getInstance().getStatement().execute(
                        "INSERT INTO dirTable (Path) VALUES('" + entry.getPath() + "')");
                return find(entry);
            } else {
                MessageUtil.infoAlert("create directory", "Directory \"" + entry.getPath() + "\" already exists");
                return directoryEntry;
            }
        } catch (SQLException e) {
            BasePathRepository.LOGGER.error("create", e);
        }
        return null;
    }

    @Override
    public DirectoryEntry update(DirectoryEntry entry) {
        return null;
    }

    @Override
    public void remove(DirectoryEntry entry) {
    }

    @Override
    public DirectoryEntry find(DirectoryEntry entry) {
        Check.notNullArgument(entry, "entry");
        try (ResultSet result = Database.getInstance().getStatement().executeQuery(
                "SELECT * FROM DIRTABLE WHERE PATH='" + entry.getPath() + "'")) {
            if (result.next()) {
                return new DirectoryEntry(result.getInt("ID"), result.getString("PATH"));
            }
        } catch (SQLException e) {
            BasePathRepository.LOGGER.error("find", e);
        }
        return null;
    }

    @Override
    public List<DirectoryEntry> findAll() {
        List<DirectoryEntry> directoryEntries = new ArrayList<>();

        try (ResultSet result = Database.getInstance().getStatement().executeQuery("SELECT * FROM DIRTABLE")) {
            while (result.next()) {
                directoryEntries.add(new DirectoryEntry(result.getInt("ID"), result.getString("PATH")));
            }
        } catch (SQLException e) {
            BasePathRepository.LOGGER.error("findAll", e);
        }
        return directoryEntries;
    }

}
