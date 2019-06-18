package com.matchup;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackEnd {
    private static final String BASE_URL = "https://inside01.api.orange.com";
    private static final String SUMMARY_LANG = "en";
    private static final String MATCH_SUMMARY = "/ofc/v1/live/match/" + SUMMARY_LANG + "_summary_";
    private static final String BEARER_TOKEN = "HWSqpZLM3POWEw9U57lx0XI2xFaN";
    private static final String MATCH_DETAILS_URL = "https://prod.footballclub.orange.com/ofc/matches/";
    private static final String TODAY_MATCHES_ENDPOINT = "/ofc/v1/ofc/matches?date=";


    public JSONArray fetchMatchEvents(long matchID) {
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

    public JSONObject fetchMatchDetails(long matchID) {

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

    public JSONArray fetchTodayMatches(String todayDate) {
        String searchString = BASE_URL + TODAY_MATCHES_ENDPOINT + todayDate;
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

    private JSONArray toJSONArray(String string) throws Exception {
        JSONParser parser = new JSONParser();
        if (!string.equals(""))
            return (JSONArray) parser.parse(string);
        else return new JSONArray();

    }

    private JSONObject toJSONObject(String string) throws org.json.simple.parser.ParseException {

        JSONParser parser = new JSONParser();
        if (!string.equals(""))
            return (JSONObject) parser.parse(string);
        else return new JSONObject();

    }

}
