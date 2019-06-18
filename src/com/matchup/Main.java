package com.matchup;

import org.json.simple.parser.*;
import org.json.simple.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;


public class Main {

    private static final long COMPETITION_ID = 20;

    private static final String BASE_URL = "https://inside01.api.orange.com";
    private static final String TODAY_MATCHES_ENDPOINT = "/ofc/v1/ofc/matches?date=";
    private static final String BEARER_TOKEN = "HWSqpZLM3POWEw9U57lx0XI2xFaN";
    private static String TodayDate = "2019-04-09"; // yyyy-mm-dd
    private static JSONArray todayCompetitionMatches;
    private static MatchHandler[] matchHandlers;
    private static int SLEEP_INTERVAL_TIME = 10; //in seconds


    public static void main(String[] args) {

        boolean changed;


        //TodayDate = fetchTodayDate();

        try {
            todayCompetitionMatches = getCompetitionMatches(fetchTodayMatches(), COMPETITION_ID);
            matchHandlers = new MatchHandler[todayCompetitionMatches.size()];
            for (int i = 0; i < todayCompetitionMatches.size(); i++) {
                matchHandlers[i] = new MatchHandler(getMatchID(i), getMatchStartDate(i), getTeam1(i), getTeam2(i));
            }
            do {
                changed = false;
                for (MatchHandler matchHandler : matchHandlers) {
                    if ((!matchHandler.started()) && checkTime(matchHandler.getStartDate())) {
                        new Thread(matchHandler).start();
                        changed = true;
                    }
                }
                Thread.sleep(SLEEP_INTERVAL_TIME * 1000);
            } while (!changed);

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

    //returns the search results for today matches from API as String
    private static JSONArray fetchTodayMatches() {
        String searchString = BASE_URL + TODAY_MATCHES_ENDPOINT + TodayDate;
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

            return toJSONArray(stringBuffer.toString());
        } catch (Exception e) {
            System.out.println(e);
        }
        return new JSONArray();


    }

    private static JSONArray toJSONArray(String string) throws Exception {
        JSONParser parser = new JSONParser();
        return (JSONArray) parser.parse(string);

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
        Date matchDate = new Date();
        try {
            matchDate = dateFormat.parse(startDate);
        } catch (java.text.ParseException e) {
            System.out.println(e);
        }
        Calendar currentTime = Calendar.getInstance(), matchStartTime = Calendar.getInstance();
        matchStartTime.setTime(matchDate);
        if (matchStartTime.compareTo(currentTime) <= 0)
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


}
