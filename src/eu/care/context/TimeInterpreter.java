package eu.care.context;

import java.time.Clock;
import java.time.LocalTime;

import eu.care.main.Utils;

public class TimeInterpreter {

	LocalTime morningStart, forenoonStart, lunchtimeStart, afternoonStart, eveningStart, nightStart;
	
	public TimeInterpreter(){
		morningStart = LocalTime.of(6, 0);
		forenoonStart = LocalTime.of(10, 0);
		lunchtimeStart = LocalTime.of(12, 0);
		afternoonStart = LocalTime.of(14, 0);
		eveningStart = LocalTime.of(17, 0);
		nightStart = LocalTime.of(21, 0);
	}

	public String getCurrentDayTime(LocalTime time){
		String interpretedDaytime = "";
		LocalTime currentTime = time;
		
		if(currentTime.isBefore(morningStart)){
			interpretedDaytime = "night";
		}
		else if(currentTime.isAfter(morningStart)){
			interpretedDaytime = "morning";
			
			if(currentTime.isAfter(forenoonStart)){
				interpretedDaytime = "forenoon";
				
				if(currentTime.isAfter(lunchtimeStart)){
					interpretedDaytime = "lunchtime";
					
					if(currentTime.isAfter(afternoonStart)){
						interpretedDaytime = "afternoon";
						
						if(currentTime.isAfter(eveningStart)){
							interpretedDaytime = "evening";
							
							if(currentTime.isAfter(nightStart)){
								interpretedDaytime = "night";
							}
						}
					}
				}
			}
		}
		
		return interpretedDaytime;
	}
	
	public String getCurrentLightConditionsOutdoors(LocalTime sunrise, LocalTime sunset, LocalTime currentTime){
		String currentLightConditionsOutside = "";
		
		Utils.printWithDate("Current time: " + currentTime + " Sunrise: " + sunrise + " Sunset: " + sunset, Utils.DEBUGLEVEL.DEBUG);
		
		if(currentTime.isBefore(sunrise)){
			currentLightConditionsOutside = "dark";
		}
		else if(currentTime.isAfter(sunrise)){
			currentLightConditionsOutside = "bright";
			
			if(currentTime.isAfter(sunset)){
				currentLightConditionsOutside = "dark";
			}
		}
		
		return currentLightConditionsOutside;
	}
	
}
