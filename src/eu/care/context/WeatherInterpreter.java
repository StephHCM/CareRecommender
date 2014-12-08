package eu.care.context;

public class WeatherInterpreter {

	public String interpreterWeatherCondition(String conditionRating){
		
		String weatherCondition = "";
		
		switch (conditionRating){
			case "0.0":
				weatherCondition = "veryBad";
				break;
			case "0.25":
				weatherCondition = "bad";
				break;
			case "0.5":
				weatherCondition = "ok";
				break;
			case "0.75":
				weatherCondition = "good";
				break;
			case "1.0":
				weatherCondition = "veryGood";
				break;
			default:
				weatherCondition = "ok";
				break;
		}
		
		return weatherCondition;
	}
}
