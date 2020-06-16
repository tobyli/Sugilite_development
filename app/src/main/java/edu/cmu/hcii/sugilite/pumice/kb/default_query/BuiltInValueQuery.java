package edu.cmu.hcii.sugilite.pumice.kb.default_query;

import android.location.Location;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.value.SugiliteValue;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceValueQueryKnowledge;

/**
 * @author toby
 * @date 6/12/20
 * @time 9:15 AM
 */
public class BuiltInValueQuery<T> implements SugiliteValue, Serializable {

    private String readableDescription;
    private ValueQuery<T> valueQuery;


    public BuiltInValueQuery(String readableDescription, ValueQuery valueQuery) {
        this.readableDescription = readableDescription;
        this.valueQuery = valueQuery;
    }

    @Override
    public T evaluate(SugiliteData sugiliteData) {
        return valueQuery.query();
    }

    @Override
    public String getReadableDescription() {
        return readableDescription;
    }

    public String getReadableValue(T result) {
        return valueQuery.getStringReadableQuery(result);
    }


    public String getFeedbackMessage(T result) {
        return valueQuery.getFeedbackMessage(result);
    }


    // get a list of all built-in values as a list of PumiceValueQueryKnowledge
    public static List<PumiceValueQueryKnowledge> getAllBuiltInValues() {
        List<PumiceValueQueryKnowledge> results = new ArrayList<>();
        results.add(new PumiceValueQueryKnowledge("current time", currentTimeBuiltInValueQuery.readableDescription, PumiceValueQueryKnowledge.ValueType.NUMERICAL, currentTimeBuiltInValueQuery));
        results.add(new PumiceValueQueryKnowledge("current temperature", currentTemperatureBuiltInValueQuery.readableDescription, PumiceValueQueryKnowledge.ValueType.NUMERICAL, currentTemperatureBuiltInValueQuery));
        return results;
    }

    // default ones
    // the current time
    private static BuiltInValueQuery<Date> currentTimeBuiltInValueQuery = new BuiltInValueQuery<>("time", new ValueQuery<Date>() {
        @Override
        public Date query() {
            return new Date(System.currentTimeMillis());
        }

        @Override
        public String getStringReadableQuery(Date currentTime) {
            return Const.dateFormat_simple.format(currentTime);
        }

        @Override
        public String getFeedbackMessage(Date currentTime) {
            return String.format("The current time is %s", getStringReadableQuery(currentTime));
        }
    });

    // the current temperature
    public static class WeatherResult {
        public Integer temperature;
        public String location;
        public String timeString;

        public WeatherResult(String location, String timeString, Integer temperature) {
            this.temperature = temperature;
            this.timeString = timeString;
            this.location = location;
        }
    }

    private static WeatherResult retrieveWeatherForLocation(double lat, double lon) throws JSONException {
        BuiltInValueHttpManager builtInValueHttpManager = BuiltInValueHttpManager.getInstance();
        String latLonRequestResult = builtInValueHttpManager.sendGet(String.format("https://api.weather.gov/points/%.3f,%.3f", lat, lon));
        JSONObject latLonRequestResultObject = new JSONObject(latLonRequestResult);
        JSONObject latLonRequestResultProperties = latLonRequestResultObject.getJSONObject("properties");
        String forecastUrl = latLonRequestResultProperties.getString("forecast");
        String weatherRequestResult = builtInValueHttpManager.sendGet(forecastUrl);
        JSONObject weatherRequestResultObject = new JSONObject(weatherRequestResult);
        JSONObject weatherRequestResultObjectProperties = weatherRequestResultObject.getJSONObject("properties");
        JSONArray weatherRequestResultArray = weatherRequestResultObjectProperties.getJSONArray("periods");
        JSONObject currentForecastObject = weatherRequestResultArray.getJSONObject(0);
        JSONObject relativeLocation = latLonRequestResultProperties.getJSONObject("relativeLocation");
        JSONObject relativeLocationProperties = relativeLocation.getJSONObject("properties");


        Integer currentTemperature = currentForecastObject.getInt("temperature");
        String timeString = currentForecastObject.getString("name");
        String location = relativeLocationProperties.getString("city");

        return new WeatherResult(location, timeString, currentTemperature);
    }

    private static BuiltInValueQuery<WeatherResult> currentTemperatureBuiltInValueQuery = new BuiltInValueQuery<>("temperature", new ValueQuery<WeatherResult>() {
        @Override
        public WeatherResult query() {
            Location bestLocation = PumiceDemonstrationUtil.getBestLocation();
            try {
                if (bestLocation == null) {
                    throw new Exception("Failed to get the current location");
                }
                double latitude = bestLocation.getLatitude();
                double longitude = bestLocation.getLongitude();
                WeatherResult weatherResult = retrieveWeatherForLocation(latitude, longitude);
                return weatherResult;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public String getStringReadableQuery(WeatherResult weatherResult) {
            return String.format("%d degrees fahrenheit", weatherResult.temperature);
        }

        @Override
        public String getFeedbackMessage(WeatherResult weatherResult) {
            return String.format("The current temperature at %s is %d degrees for %s", weatherResult.location, weatherResult.temperature, weatherResult.timeString);

        }
    });

}
