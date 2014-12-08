package eu.care.user;

import java.time.LocalDateTime;

/**
 * 
 * @author Stephan Hammer
 *
 */
public class UserModel {

	private String mood;
	private LocalDateTime lastUpdateMood;
	public int updateInterval;
	
	public UserModel(int updateInterval){
		mood = "";
		lastUpdateMood = LocalDateTime.now();
		this.updateInterval = updateInterval;
	}
	
	public void setLastUpdateMood(LocalDateTime lastUpdateMood){
		this.lastUpdateMood = lastUpdateMood;
	}
	
	public LocalDateTime getLastUpdateMood(){
		return this.lastUpdateMood;
	}
	
	public int getUpdateInterval(){
		return this.updateInterval;
	}
	
	public void setMood(String mood){
		this.mood = mood;
	}
	
	public String getMood(){
		return this.mood;
	}
}
