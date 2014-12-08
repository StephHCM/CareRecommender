package eu.care.context;

/**
 * 
 * @author Stephan Hammer
 *
 */
public class UserMoodInterpreter {

	public String interpreterUserMood(String userAnswer){
		
		String userMood = "";
		
		switch (userAnswer){
			case "happy": case "good":
				userMood = "goodMood";
				break;
			case "bad": case "sad":
				userMood = "badMood";
				break;
			default:
				userMood = "neutralMood";
				break;
		}
		
		return userMood;
	}
}
