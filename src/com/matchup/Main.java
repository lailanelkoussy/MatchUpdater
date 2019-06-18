package com.matchup;

import org.json.simple.parser.*;
import org.json.simple.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class Main {

    private static final long COMPETITION_ID = 2;
    private static String todayDate = "2018-08-20"; // yyyy-mm-dd
    private static JSONArray todayCompetitionMatches;
    private static List<MatchHandler> matchHandlers = new ArrayList<>();
    private static int SLEEP_INTERVAL_TIME = 10; //in seconds
    private static String SET_DATE = "2018-08-20 17:59:30";
    private static BackEnd backEnd = new BackEnd();


    public static void main(String[] args) {

        boolean done;

        //todayDate = fetchTodayDate();

        try {
            todayCompetitionMatches = getCompetitionMatches(backEnd.fetchTodayMatches(todayDate), COMPETITION_ID);
            matchHandlers = new ArrayList<>();
            for (int i = 0; i < todayCompetitionMatches.size(); i++) {
                matchHandlers.add(new MatchHandler(getMatchID(i), getMatchStartDate(i), getTeam1(i), getTeam2(i)));
            }
            Collections.sort(matchHandlers);
            for (MatchHandler matchHandler : matchHandlers) {
                System.out.println(matchHandler.getName() + " " + matchHandler.getStartDate());
            }
            do {
                done = true;
                for (MatchHandler matchHandler : matchHandlers) {
                    if ((!matchHandler.started()) && checkTime(matchHandler.getStartDate())) {
                        System.out.println(SET_DATE);
                        System.out.println("Starting thread " + matchHandler.getName());
                        new Thread(matchHandler).start();
                    }
                    if(!matchHandler.started())
                        done = false;
                }
                Thread.sleep(SLEEP_INTERVAL_TIME * 10 / 5);
                incrementStartDate();
            } while (!done);

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    //returns today's date in format yyyy-MM-dd
    public static String fetchTodayDate() {

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.now();
        return dateTimeFormatter.format(localDate); //2016-11-16

    }


    private static JSONArray getCompetitionMatches(JSONArray searchResults, long competitionID) {

        JSONArray competitionMatchesArray = new JSONArray();
        JSONObject objectAtI;

        for (int i = 0; i < searchResults.size(); i++) {
            objectAtI = (JSONObject) searchResults.get(i);
            if ((long) objectAtI.get("idCompetition") == competitionID) {
                competitionMatchesArray.add(objectAtI);
            }

        }
        return competitionMatchesArray;
    }

    private static long getMatchID(int i) {
        JSONObject objectAtI = (JSONObject) todayCompetitionMatches.get(i);
        return (long) objectAtI.get("idMatch");

    }

    private static String getMatchStartDate(int i) {
        JSONObject objectAtI = (JSONObject) todayCompetitionMatches.get(i);
        return (String) objectAtI.get("startDate");

    }

    private static boolean checkTime(String startDate) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date matchDate = new Date(), setDate = new Date();
        try {
            matchDate = dateFormat.parse(startDate);
            setDate = dateFormat.parse(SET_DATE);
        } catch (java.text.ParseException e) {
            System.out.println(e);
        }
        Calendar setCalendar = Calendar.getInstance(), matchStartTime = Calendar.getInstance();
        matchStartTime.setTime(matchDate);
        setCalendar.setTime(setDate);

        if (matchStartTime.compareTo(setCalendar) <= 0)
            return true;
        else return false;

    }

    private static String getTeam1(int i) {
        JSONObject objectAtI = (JSONObject) todayCompetitionMatches.get(i);
        JSONObject team1 = (JSONObject) objectAtI.get("team1");
        return (String) team1.get("name");
    }

    private static String getTeam2(int i) {
        JSONObject objectAtI = (JSONObject) todayCompetitionMatches.get(i);
        JSONObject team2 = (JSONObject) objectAtI.get("team2");
        return (String) team2.get("name");
    }

    private static void incrementStartDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date matchDate = new Date();
        try {
            matchDate = dateFormat.parse(SET_DATE);
        } catch (java.text.ParseException e) {
            System.out.println(e);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(matchDate);
        calendar.add(Calendar.SECOND, SLEEP_INTERVAL_TIME);
        calendar.add(Calendar.MONTH, 1);


        SET_DATE = calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) < 10 ? "0" +
                calendar.get(Calendar.MONTH) : calendar.get(Calendar.MONTH)) + "-"
                + (calendar.get(Calendar.DAY_OF_MONTH) < 10 ? "0"
                + calendar.get(Calendar.DAY_OF_MONTH) : calendar.get(Calendar.DAY_OF_MONTH)) + " " +
                calendar.get(Calendar.HOUR_OF_DAY) + ":" + (calendar.get(Calendar.MINUTE) < 10 ? "0" +
                calendar.get(Calendar.MINUTE) : calendar.get(Calendar.MINUTE)) + ":"
                + (calendar.get(Calendar.SECOND) < 10 ? "0" + calendar.get(Calendar.SECOND) : calendar.get(Calendar.SECOND));

    }


}
