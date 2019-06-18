package com.matchup;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DataBase {

    private static final String EVENT_STRUCTURE = "matchID integer," + "idEvent integer," + "eventType integer," + "eventLabel text," +
            "txtMinute integer," + "time integer," + " player1 text," + " player2 text," + "idTeam integer," + "team text ";

    private final String path;
    private final String name;
    private Connection connection;

    DataBase(String path, String name) {
        this.path = path;
        this.name = name;

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path + name + ".db");

        } catch (java.sql.SQLException e) {
            System.out.println(e);
        }
    }

    synchronized public void insert(String tableName, JSONObject event, long matchID) {

        long idEvent, eventType, txtMinute, time, idTeam;
        String eventLabel, player1, player2, team;

        idEvent = (long) event.get("idEvent");
        eventType = (long) event.get("eventType");
        txtMinute = Long.valueOf((event.get("txtMinute")).toString().substring(0, ((String) event.get("txtMinute")).indexOf("'")));
        time = (long) event.get("time");
        idTeam = (long) event.get("idTeam");
        eventLabel = isEmptyOrNull((String) event.get("eventLabel")) ? "NULL" : (String) event.get("eventLabel");
        player1 = isEmptyOrNull((String) (event.get("player1"))) ? "NULL" : (String) event.get("player1");
        player2 = isEmptyOrNull((String) (event.get("player2"))) ? "NULL" : (String) event.get("player2");
        team = isEmptyOrNull((String) (event.get("team"))) ? "NULL" : (String) event.get("team");

        try {

            Statement statement = connection.createStatement();
            statement.execute("INSERT INTO " + tableName + " (matchID, idEvent, eventType, txtMinute, time, idTeam, eventLabel, player1, player2, team)"
                    + " VALUES " + " ( " + matchID + "," + idEvent + ", " + eventType + ", " + txtMinute + ", " + time + ", " + idTeam
                    + ", '" + eventLabel + "', '" + player1 + "', '" + player2 + "', '" + team + "');");
        } catch (java.sql.SQLException e) {
            System.out.println(e);
        }

    }

    private boolean isEmptyOrNull(String string) {
        boolean empty;
        String nullstring = null;


        if (string != nullstring) {
            if (string.equals(" "))
                return true;
            if (string.isEmpty())
                return true;

            return false;
        }
        return true;


    }

    public void close() {
        try {
            connection.close();
        } catch (java.sql.SQLException e) {
            System.out.println(e);
        }
    }

    public int getNumberOfEvents(String tableName, long matchID) {

        try {
            Statement statement = connection.createStatement();
            ResultSet countResultSet = statement.executeQuery("SELECT COUNT(*) AS size FROM " + tableName + "WHERE matchID =" + matchID + " ;");
            return countResultSet.getInt("size");
        } catch (java.sql.SQLException e) {
            System.out.println(e);
        }
        return 0;
    }

    synchronized public void addEventsToTable(String tableName, JSONArray matchEvents, long matchID) {

        try {
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS " + tableName + " ( " + EVENT_STRUCTURE + ") ;"); //table name is "events{matchID}"
            for (Object event : matchEvents) {
                if (!isInTable(tableName, (JSONObject) event)) {
                    insert(tableName, (JSONObject) event, matchID);

                }
            }

        } catch (java.sql.SQLException e) {
            System.out.println(e);
        }
    }

    public boolean isInTable(String tableName, JSONObject event) throws java.sql.SQLException {


        Statement statement = connection.createStatement();
        ResultSet countResultSet = statement.executeQuery("SELECT COUNT(*) AS instances FROM " + tableName + " WHERE idEvent = " + event.get("idEvent") + ";");
        boolean result = (countResultSet.getInt("instances") > 0);
        return result;


    }
}
