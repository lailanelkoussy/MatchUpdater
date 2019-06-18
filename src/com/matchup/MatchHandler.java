package com.matchup;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class MatchHandler implements Runnable, Comparable<MatchHandler> {

    private static final String DB_PATH = "//Users/macbookpro/Documents/University/Internship/MatchUpdater/";
    private static final String DB_NAME = "Matches";
    private final String TABLE_NAME;
    private final long EVENT_TYPE_GOAL = (long) 1;
    private final int CHECKING_INTERVAL = 5; //in seconds
    private final String team1, team2;
    private long matchID;
    private String startDate;
    private JSONArray matchEvents;
    private JSONObject matchDetails;
    private boolean start;
    private static DataBase dataBase;
    private long resultTeam1, resultTeam2;
    private static BackEnd backEnd;

    static {
        dataBase = new DataBase(DB_PATH, DB_NAME);
        backEnd = new BackEnd();
    }

    MatchHandler(long matchID, String startDate, String team1, String team2) {

        start = false;
        this.matchID = matchID;
        this.startDate = startDate;
        TABLE_NAME = "events" + extractTableName();
        this.team1 = team1;
        this.team2 = team2;
        matchDetails = backEnd.fetchMatchDetails(this.matchID);
        resultTeam1 = (long) matchDetails.get("resultTeam1");
        resultTeam2 = (long) matchDetails.get("resultTeam2");

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
            System.out.println(getName() + ": getting match events");
            matchEvents = backEnd.fetchMatchEvents(matchID);

            System.out.println(getName() + ": adding events to table");
            //write events to db for the first time (table on their own)
            dataBase.addEventsToTable(TABLE_NAME, matchEvents, matchID);
            while (matchNotEnded()) {
                checkAndUpdateEvents();
                Thread.sleep(CHECKING_INTERVAL * 1000);
            }

            printResults();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @Override
    public int compareTo(MatchHandler matchHandler) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date matchHandlerDate = new Date(), thisMatchDate = new Date();

        try {
            matchHandlerDate = dateFormat.parse(matchHandler.getStartDate());
            thisMatchDate = dateFormat.parse(this.startDate);
        } catch (java.text.ParseException e) {
            System.out.println(e);
        }
        Calendar thisMatchTime = Calendar.getInstance(), matchHandlerStartTime = Calendar.getInstance();
        matchHandlerStartTime.setTime(matchHandlerDate);
        thisMatchTime.setTime(thisMatchDate);

        return (thisMatchDate.compareTo(matchHandlerDate));

    }


    private String extractTableName() {
        String temp;
        temp = startDate.substring(0, startDate.indexOf(" "));
        temp = temp.replaceAll("-", "");
        return temp;

    }

    private void checkAndUpdateEvents() {

        try {
            matchEvents = backEnd.fetchMatchEvents(matchID); //getting match Events
        } catch (java.lang.Exception e) {
            System.out.println(e);
        }
        if (dataBase.getNumberOfEvents(TABLE_NAME, matchID) < matchEvents.size()) //there are new events
        {
            for (Object event : matchEvents) {
                try {
                    if (!dataBase.isInTable(TABLE_NAME, (JSONObject) event)) {
                        System.out.println(getName() + ": new event: " + event);
                        dataBase.insert(TABLE_NAME, (JSONObject) event, matchID);
                        updateMatchResults((JSONObject) event);
                    }
                } catch (java.sql.SQLException e) {
                    System.out.println(e);
                }
            }
        }

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


    //checks for match status CONTAINS HARDWIRED VALUES
    private boolean matchNotEnded() {
        long matchStatus;
        matchDetails = backEnd.fetchMatchDetails(matchID);

        matchStatus = (long) matchDetails.get("status");
        if (matchStatus > 0 && matchStatus < 7)//checking for matchStatus codes
            return false;
        else return true;

    }

    synchronized private void printResults() {

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

    public String getName() {
        return team1 + " - " + team2;
    }
}
