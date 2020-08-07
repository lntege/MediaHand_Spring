package com.intege.mediahand.repository.base;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import com.intege.mediahand.utils.MessageUtil;

/*Tables:
 *
 * "mediaTable"
 * "dirTable"
 * "mediaState"
 *
 */

public class Database {

//    private static final Database database = new Database("AnimeDatabase", "lueko", "1234", false);

    private Connection connection;
    private Statement statement;

    private Database(String databaseName, String username, String password, boolean removeoldTables) {
        init(databaseName, username, password, removeoldTables);
    }

    public static Database getInstance() {
        //        return Database.database;
        return null;
    }

    /**
     * Connects to the specified database and creates or opens the mediaTable.
     *
     * @param databaseName the name of the database
     * @param username the username to connect to the database
     * @param password the password to connect to the database
     * @param removeOldTables determines whether the old table should be removed to create new tables
     */
    private void init(String databaseName, String username, String password, boolean removeOldTables) {

        openConnection(databaseName, username, password);

        /*
         * Removing old table to create a new table.
         */
        if (removeOldTables) {
            dropTables();
        }
    }

    /**
     * Connects to the specified database.
     *
     * @param databaseName the name of the database to connect to
     * @param username the username to connect to the database
     * @param password the password to connect to the database
     */
    private void openConnection(final String databaseName, final String username, final String password) {
        /*
         * Checking for JDBC driver.
         */
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (ClassNotFoundException e) {
            MessageUtil.warningAlert(e, "Driver not found!");
            System.exit(-1);
        }

        /*
         * Connecting to local database.
         */
        try {
            this.connection = DriverManager.getConnection(
                    "jdbc:hsqldb:file:" + databaseName + "; shutdown = true", username, password);
            this.statement = this.connection.createStatement();
        } catch (SQLException e) {
            MessageUtil.warningAlert(e);
            System.exit(-1);
        }
    }

    /**
     * Closes all connections of the connected database.
     */
    public void closeConnections() {
        try {
            this.statement.close();
        } catch (SQLException e) {
            MessageUtil.warningAlert(e);
        }

        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (SQLException e) {
                MessageUtil.warningAlert(e);
            }
        }
    }

    // TODO [lueko]: refactor to repository implementations e.g. dropTable boolean into each constructor and set in repositoryFactory
    public void dropTables() {
        try {
            this.statement.execute("DROP TABLE mediaTable");
            this.statement.execute("DROP TABLE dirTable");
            this.statement.execute("DROP TABLE settingsTable");
        } catch (SQLException e) {
            MessageUtil.warningAlert(e, "Could not drop tables!");
        }
    }

    public void printTables() {
        DBTablePrinter.printTable(this.connection, "mediaTable");
        DBTablePrinter.printTable(this.connection, "dirTable");
        DBTablePrinter.printTable(this.connection, "settingsTable");
    }

    public Statement getStatement() {
        return this.statement;
    }
}
