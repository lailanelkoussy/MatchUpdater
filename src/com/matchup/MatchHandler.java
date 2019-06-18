package com.matchup;

import java.sql.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class MatchHandler implements Runnable {

    private static final String BEARER_TOKEN = "HWSqpZLM3POWEw9U57lx0XI2xFaN";
    private static final String BASE_URL = "https://inside01.api.orange.com";
    private static final String SUMMARY_LANG = "en";
    private static final String MATCH_SUMMARY = "/ofc/v1/live/match/" + SUMMARY_LANG + "_summary_";
    private static final String MATCH_DETAILS_URL = "https://prod.footballclub.orange.com/ofc/matches/";
    private static final String DB_PATH = "//Users/macbookpro/Documents/University/Internship/MatchUpdater/";
    private static final String EVENT_STRUCTURE = "idEvent integer," + "eventType integer," + "eventLabel text," +
            "txtMinute integer," + "time integer," + " player1 text," + " player2 text," + "idTeam integer," + "team text ";
    private final String DB_NAME;
    private final String TABLE_NAME;
    private final long EVENT_TYPE_GOAL = (long) 1;
    private final int CHECKING_INTERVAL = 5; //in seconds
    private final String team1, team2;
    private long matchID;
    private String startDate;
    private JSONArray matchEvents;
    private JSONObject matchDetails;
    private boolean start;
    private Connection dbConnection;
    private long resultTeam1, resultTeam2;

    MatchHandler(long matchID, String startDate, String team1, String team2) {

        start = false;
        this.matchID = matchID;
        this.startDate = startDate;
        DB_NAME = extractDBName();
        TABLE_NAME = "events" + this.matchID;
        this.team1 = team1;
        this.team2 = team2;
        matchDetails = fetchMatchDetails();
        resultTeam1 = (long) matchDetails.get("resultTeam1");
        resultTeam2 = (long) matchDetails.get("resultTeam2");

        try {
            dbConnection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH + DB_NAME + ".db");

        } catch (java.sql.SQLException e) {
            System.out.println(e);
        }
    }

    public boolean started() {
        return start;
    }

    public String getStartDate() {
        return startDate;
    }

    @Override
    public void run() {
        start = true;

        try {

            matchEvents = fetchMatchEvents();

            //write events to db for the first time (table on their own)
            createAndInitializeTable();
            while (matchNotEnded()) {
                checkAndUpdateEvents();
                Thread.sleep(CHECKING_INTERVAL * 1000);
            }

            printEvents();

        } catch (Exception e) {
            System.out.println(e);
        } finally {
            try {
                dbConnection.close();
            } catch (java.sql.SQLException e) {
                System.out.println(e);

            }
        }
    }

    private JSONArray toJSONArray(String string) throws Exception {
        JSONParser parser = new JSONParser();
        if (!string.equals(""))
            return (JSONArray) parser.parse(string);
        else return new JSONArray();

    }

    private JSONArray fetchMatchEvents() {
        String searchString = BASE_URL + MATCH_SUMMARY + matchID + ".json";
        URL url;
        try {
            url = new URL(searchString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + BEARER_TOKEN);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            if (connection.getResponseCode() == 200) {
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuffer stringBuffer = new StringBuffer();
                while ((inputLine = bufferedReader.readLine()) != null) {
                    stringBuffer.append(inputLine);
                }
                bufferedReader.close();

                return toJSONArray(stringBuffer.toString());
            }

        } catch (Exception e) {
            System.out.println(e);
        }
        return new JSONArray();


    }

    private String extractDBName() {
        String temp;
        temp = startDate.substring(0, startDate.indexOf(" "));
        temp = temp.replaceAll("-", "");
        return temp;

    }

    private void checkAndUpdateEvents() {

        try {
            matchEvents = fetchMatchEvents(); //getting match Events
        } catch (java.lang.Exception e) {
            System.out.println(e);
        }
        if (getTableSize() < matchEvents.size()) //there are new events
        {
            for (Object event : matchEvents) {
                try {
                    if (!isInTable((JSONObject) event)) {
                        insertintoDB((JSONObject) event);
                        updateMatchResults((JSONObject) event);
                    }

                } catch (java.sql.SQLException e) {
                    System.out.println(e);
                }
            }
        }

    }

    private void createAndInitializeTable() {

        try {
            Statement statement = dbConnection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ( " + EVENT_STRUCTURE + ") ;"); //table name is "events{matchID}"
            for (Object event : matchEvents) {
                if (!isInTable((JSONObject) event)) {
                    insertintoDB((JSONObject) event);
                    updateMatchResults((JSONObject) event);
                }
            }

        } catch (java.sql.SQLException e) {
            System.out.println(e);
        }


    }

    private void insertintoDB(JSONObject event) {

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

            Statement statement = dbConnection.createStatement();
            statement.execute("INSERT INTO " + TABLE_NAME + " (idEvent, eventType, txtMinute, time, idTeam, eventLabel, player1, player2, team)"
                    + " VALUES " + " ( " + idEvent + ", " + eventType + ", " + txtMinute + ", " + time + ", " + idTeam
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

    private boolean isInTable(JSONObject event) throws java.sql.SQLException {


        Statement statement = dbConnection.createStatement();
        ResultSet countResultSet = statement.executeQuery("SELECT COUNT(*) AS instances FROM " + TABLE_NAME + " WHERE idEvent = " + event.get("idEvent") + ";");
        return (countResultSet.getInt("instances") > 0);


    }

    private int getTableSize() {

        try {
            Statement statement = dbConnection.createStatement();
            ResultSet countResultSet = statement.executeQuery("SELECT COUNT(*) AS size FROM " + TABLE_NAME + ";");
            return countResultSet.getInt("size");
        } catch (java.sql.SQLException e) {
            System.out.println(e);
        }
        return 0;
    }

    private void updateMatchResults(JSONObject event) {

        if ((long) event.get("eventType") == EVENT_TYPE_GOAL) {
            if (event.get("team") == team1) {
                resultTeam1++;
            } else {
                resultTeam2++;
            }
            updateMatchImages();

        }
    }

    private void updateMatchImages() {
        System.out.println("Match results for " + team1 + " - " + team2 + " changed");
        System.out.println("Results are now: " + team1 + " " + resultTeam1 + " - " + resultTeam2 + " " + team2);

    }

    private JSONObject toJSONObject(String string) throws org.json.simple.parser.ParseException {

        JSONParser parser = new JSONParser();
        if (!string.equals(""))
            return (JSONObject) parser.parse(string);
        else return new JSONObject();

    }

    private JSONObject fetchMatchDetails() {

        String searchString = MATCH_DETAILS_URL + matchID;
        URL url;
        try {
            url = new URL(searchString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + BEARER_TOKEN);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer stringBuffer = new StringBuffer();
            while ((inputLine = bufferedReader.readLine()) != null) {
                stringBuffer.append(inputLine);
            }
            bufferedReader.close();

            return toJSONObject(stringBuffer.toString());
        } catch (Exception e) {
            System.out.println(e);
        }
        return new JSONObject();

    }

    //checks for match status CONTAINS HARDWIRED VALUES
    private boolean matchNotEnded() {
        long matchStatus;
        matchDetails = fetchMatchDetails();

        matchStatus = (long) matchDetails.get("status");
        if (matchStatus > 0 && matchStatus < 7)//checking for matchStatus codes
            return false;
        else return true;

    }

    synchronized private void printEvents() {

        System.out.println("Match ended with results: " + team1 + " " + resultTeam1 + " - " + resultTeam2 + " " + team2);


//        try {
//            Statement statement = dbConnection.createStatement();
//            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + TABLE_NAME + ";");
//            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
//            int columnsNumber = resultSetMetaData.getColumnCount();
//            while (resultSet.next()) {
//                System.out.println("Event: ");
//                for (int i = 1; i <= columnsNumber; i++) {
//                    String columnValue = resultSet.getString(i);
//                    if (resultSetMetaData.getColumnName(i).equals("txtMinute")) {
//                        System.out.println(resultSetMetaData.getColumnName(i) + ":  " + columnValue + "'");
//
//                    } else if (!columnValue.equals("NULL"))
//                        System.out.println(resultSetMetaData.getColumnName(i) + ":  " + columnValue);
//
//                }
//                System.out.println();
//
//            }
//
//        } catch (java.sql.SQLException e) {
//            System.out.println(e);
//        }


    }


}
