package eu.care.context;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Random;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import eu.care.main.DemonstratorMain;
import eu.care.main.Utils;

public class ContextModel {
	
	//weather condition
	private String weatherCondition;
	private LocalDateTime lastUpdateWeatherCondition;
	private int updateIntervalWeatherCondition;
	
	//light conditions (outdoors)
	private LocalTime sunset;
	private LocalTime sunrise;
	private LocalDateTime lastUpdateSolarAltitude;
	private int updateIntervalSolarAltitude;
	
	private ArrayList<String> possibleConditions = new ArrayList<String>();
	private Random random = new Random(System.currentTimeMillis());
	
	public ContextModel(int updateIntervalWeatherCondition, int updateIntervalSolarAltitude){
		weatherCondition = "";
		lastUpdateWeatherCondition = LocalDateTime.now(Clock.systemUTC());
		this.updateIntervalWeatherCondition = updateIntervalWeatherCondition;
		
		lastUpdateSolarAltitude = LocalDateTime.now(Clock.systemUTC());
		this.updateIntervalSolarAltitude = updateIntervalSolarAltitude;
	
		possibleConditions.add("0.0");
		possibleConditions.add("0.25");
		possibleConditions.add("0.5");
		possibleConditions.add("0.75");
		possibleConditions.add("1.0");
	}
	
	public String getCurrentWeatherCondition(DemonstratorMain main, String dbName, boolean randomWeather){
		
		try {
			//time since last update of the weather condition
			long diffInMinutes = Duration.between(lastUpdateWeatherCondition, LocalDateTime.now(Clock.systemUTC())).toMinutes();
		
			if (randomWeather){
				String conditionRating = possibleConditions.get(random.nextInt(possibleConditions.size()));
				weatherCondition = new WeatherInterpreter().interpreterWeatherCondition(conditionRating);
				Utils.printWithDate("Current weather condition: " + weatherCondition, Utils.DEBUGLEVEL.GENERAL);
				lastUpdateWeatherCondition = LocalDateTime.now(Clock.systemUTC());
			}
			else{
				//if times for sunrise and sunset are unknown or "older" than interval-time
				if (weatherCondition.isEmpty() || diffInMinutes > updateIntervalWeatherCondition) {
					
					String weatherConditionJSON = "";
					if (DemonstratorMain.mongoDBConnection != null){
						weatherConditionJSON = DemonstratorMain.mongoDBConnection.getLastWeatherConditionWithRating(dbName);
					}
					else{
						//TODO get weatherCondition from json-file
					}	
					if(!weatherConditionJSON.isEmpty()){
						JSONObject weatherConditionJSONObject = (JSONObject) main.jsonParser.myParser.parse(weatherConditionJSON);
						Utils.printWithDate("Weather condition:" + weatherConditionJSONObject, Utils.DEBUGLEVEL.DEBUG);
							
						String currentWeatherCondition = main.jsonParser.getWeatherConditionRating(weatherConditionJSONObject);
						weatherCondition = new WeatherInterpreter().interpreterWeatherCondition(currentWeatherCondition);
	
						Utils.printWithDate("Current weather condition: " + weatherCondition, Utils.DEBUGLEVEL.GENERAL);
						lastUpdateWeatherCondition = LocalDateTime.now(Clock.systemUTC());
					}
					else{
						Utils.printWithDate("No weather conditions received", Utils.DEBUGLEVEL.DEBUG);
						weatherCondition = "";
					}
				}
				else{
					Utils.printWithDate("Current weather condition: " + weatherCondition + " (lastUpdate " + diffInMinutes + " minutes ago)", Utils.DEBUGLEVEL.GENERAL);
				}
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return weatherCondition;
	}
	
	public String getCurrentLightConditions(DemonstratorMain main, String dbName, boolean useJSON, LocalTime currentTime){
		String lightConditions = "";
		
		try {
			//time since last update of the solar altitude (times for sunrise and sunset)
			long diffInHours = Duration.between(lastUpdateSolarAltitude, LocalDateTime.now(Clock.systemUTC())).toHours();
			
			//if times for sunrise and sunset are unknown or "older" than interval-time
			if ((sunrise == null || sunset == null) || diffInHours > updateIntervalSolarAltitude) {
				
				String sunriseSunsetJSON = "";
				
				if (DemonstratorMain.mongoDBConnection != null && !useJSON){
					sunriseSunsetJSON = DemonstratorMain.mongoDBConnection.getCurrentSunsetSunriseTimes(dbName);
				}
				else{
					sunriseSunsetJSON = main.jsonParser.getJSONMessage("sunTimes.json");
				}
				
				if(!sunriseSunsetJSON.isEmpty()){
					JSONObject sunriseSunsetJSONObject = (JSONObject) main.jsonParser.myParser.parse(sunriseSunsetJSON);
					Utils.printWithDate("Sunrise/Sunset:" + sunriseSunsetJSONObject, Utils.DEBUGLEVEL.DEBUG);
					
					//Date Format: 2014-10-29T06:01:43.823Z
					DateTimeFormatter myDateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
					
					sunrise = LocalDateTime.parse(main.jsonParser.getDataFromSunTimesJSON(sunriseSunsetJSONObject, "sunriseEnd"), myDateTimeFormat).toLocalTime();
					sunset = LocalDateTime.parse(main.jsonParser.getDataFromSunTimesJSON(sunriseSunsetJSONObject, "sunsetStart"), myDateTimeFormat).toLocalTime();
					
					lightConditions = new TimeInterpreter().getCurrentLightConditionsOutdoors(sunrise, sunset, currentTime);
					Utils.printWithDate("Current light condition (outdoors): " + lightConditions, Utils.DEBUGLEVEL.GENERAL);
					
					lastUpdateSolarAltitude = LocalDateTime.now(Clock.systemUTC());
				}
				//no information
				else if (sunrise == null || sunset == null){
					Utils.printWithDate("No updated times for sunrise and sunset received", Utils.DEBUGLEVEL.DEBUG);
					lightConditions = "";
				}
				//old information
				else{
					Utils.printWithDate("No updated times for sunrise and sunset received. Use old times.", Utils.DEBUGLEVEL.DEBUG);
					lightConditions = new TimeInterpreter().getCurrentLightConditionsOutdoors(sunrise, sunset, currentTime);
					Utils.printWithDate("Current light condition (outdoors): " + lightConditions + " (lastUpdate(Sunrise/Sunset) " + diffInHours + " hours ago)", Utils.DEBUGLEVEL.GENERAL);
				}
			}
			else{
				lightConditions = new TimeInterpreter().getCurrentLightConditionsOutdoors(sunrise, sunset, currentTime);
				Utils.printWithDate("Current light condition at (outdoors): " + lightConditions + " (lastUpdate(Sunrise/Sunset) " + diffInHours + " hours ago)", Utils.DEBUGLEVEL.GENERAL);
			}
			

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return lightConditions;
	}
}
