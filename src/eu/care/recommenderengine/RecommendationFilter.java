package eu.care.recommenderengine;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.json.simple.JSONObject;

import eu.care.main.DemonstratorMain;
import eu.care.main.Utils;

/**
 * 
 * @author Stephan Hammer, Alexander Diefenbach
 *
 */

public class RecommendationFilter {
	
	private DemonstratorMain main;
	private Vector<Recommendation> recommendationsToShow;
	private String debugString = "";
	private HashMap<String, Integer> temporaryRecHistory;
	private LocalDateTime lastHistoryCleanup;
	private long historyCleanupInterval = 60; //in minutes
	private int maximumDisplayCount = 3;
	private Random random = new Random(System.currentTimeMillis());
	
	//Case config
	private int noCaseData = 0;
	private int missingInfo = 0;
	private int positiveRating = 0;
	private int negativeRating = 0;
	private int neutralRating = 0;
	private int lightConditionOutdoorsEqual = 0;
	private int lightConditionOutdoorsDifferent = 0;
	private int moodEqual = 0;
	private int moodSimilar = 0;
	private int moodDifferent = 0;
	private int weatherConditionEqual = 0;
	private int weatherConditionSimilar = 0;
	private int weatherConditionDiverging = 0;
	private int weatherConditionDifferent = 0;
	private int weatherConditionOpposite = 0;
	private int daytimeEqual = 0;
	private int daytimeSimilar = 0;
	private int daytimeDifferent = 0;
	private int daytimeOpposite = 0;
	public int casesEqualThreshold = 1000;
	
	public RecommendationFilter(DemonstratorMain main, long historyCleanupInterval, int maximumDisplayCount){
		this.main = main;
		this.historyCleanupInterval = historyCleanupInterval;
		this.maximumDisplayCount = maximumDisplayCount;
		
		temporaryRecHistory = new HashMap<String, Integer>();
		lastHistoryCleanup = LocalDateTime.now(Clock.systemUTC());
		
		noCaseData = Integer.parseInt((((JSONObject)main.jsonConfig.get("CaseRating")).get("noCaseData").toString()));
		missingInfo = Integer.parseInt((((JSONObject)main.jsonConfig.get("CaseRating")).get("missingInfo").toString()));
		positiveRating = Integer.parseInt((((JSONObject)main.jsonConfig.get("CaseRating")).get("positiveRating").toString()));
		negativeRating = Integer.parseInt((((JSONObject)main.jsonConfig.get("CaseRating")).get("negativeRating").toString()));
		neutralRating = Integer.parseInt((((JSONObject)main.jsonConfig.get("CaseRating")).get("neutralRating").toString()));
		lightConditionOutdoorsEqual = Integer.parseInt((((JSONObject)main.jsonConfig.get("CaseRating")).get("lightConditionOutdoorsEqual").toString()));
		lightConditionOutdoorsDifferent = Integer.parseInt((((JSONObject)main.jsonConfig.get("CaseRating")).get("lightConditionOutdoorsDifferent").toString()));
		moodEqual = Integer.parseInt((((JSONObject)main.jsonConfig.get("CaseRating")).get("moodEqual").toString()));
		moodSimilar = Integer.parseInt((((JSONObject)main.jsonConfig.get("CaseRating")).get("moodSimilar").toString()));
		moodDifferent = Integer.parseInt((((JSONObject)main.jsonConfig.get("CaseRating")).get("moodDifferent").toString()));
		weatherConditionEqual = Integer.parseInt((((JSONObject)main.jsonConfig.get("CaseRating")).get("weatherConditionEqual").toString()));
		weatherConditionSimilar = Integer.parseInt((((JSONObject)main.jsonConfig.get("CaseRating")).get("weatherConditionSimilar").toString()));
		weatherConditionDiverging = Integer.parseInt((((JSONObject)main.jsonConfig.get("CaseRating")).get("weatherConditionDiverging").toString()));
		weatherConditionDifferent = Integer.parseInt((((JSONObject)main.jsonConfig.get("CaseRating")).get("weatherConditionDifferent").toString()));
		weatherConditionOpposite = Integer.parseInt((((JSONObject)main.jsonConfig.get("CaseRating")).get("weatherConditionOpposite").toString()));
		daytimeEqual = Integer.parseInt((((JSONObject)main.jsonConfig.get("CaseRating")).get("daytimeEqual").toString()));
		daytimeSimilar = Integer.parseInt((((JSONObject)main.jsonConfig.get("CaseRating")).get("daytimeSimilar").toString()));
		daytimeDifferent = Integer.parseInt((((JSONObject)main.jsonConfig.get("CaseRating")).get("daytimeDifferent").toString()));
		daytimeOpposite = Integer.parseInt((((JSONObject)main.jsonConfig.get("CaseRating")).get("daytimeOpposite").toString()));
		casesEqualThreshold = Integer.parseInt((((JSONObject)main.jsonConfig.get("CaseRating")).get("casesEqualThreshold").toString()));
		
	}
	
	public Vector<Recommendation> useNoFilter(Vector<Recommendation> recommendations){
		
		recommendationsToShow = new Vector<Recommendation>();
		debugString = "";
		
		for(Recommendation rec : recommendations){
			recommendationsToShow.add(rec);

			debugString = rec.getTitle() + ", ";
			main.myLogger.addNewObjectToArray(main.myLogger.sentRecArray, rec.getTitle(), "chosen recommendation");
		}
		
		Utils.printWithDate("Chosen recommendations: " + debugString, Utils.DEBUGLEVEL.DEBUG);
		
		return recommendationsToShow;
	}
	
	public Vector<Recommendation> chooseCaseBasedRecommendation(Vector<Recommendation> possibleRecommendations,	HashMap<String, String> contextInformation) {
		Vector<Recommendation> result = new Vector<Recommendation>();
		Vector<Integer> recommendationIDs = new Vector<Integer>();
		for(Recommendation rec: possibleRecommendations){
			recommendationIDs.add(Integer.parseInt(rec.getID()));
		}
		//Get all known past cases for the possible recommendation IDs
		Vector<HashMap<String, String>> pastCases = main.mongoDBConnection.getCasesForRecommendationIDs(recommendationIDs, main.caseDB, main.caseCollection);
		for (HashMap<String, String>pastCase: pastCases){			
			pastCase.put("Similarity", Integer.toString(calculateCaseRating(contextInformation, pastCase)));
			Utils.printWithDate("Known case for ID: " + pastCase.get("RecID"), Utils.DEBUGLEVEL.DEBUG);
		}
		//Add possible recommendations with no known past cases as average similarity
		for (Recommendation rec : possibleRecommendations){
			boolean found = false;
			for (HashMap<String, String> pastCase: pastCases){
				if(pastCase.get("RecID").equals(rec.getID())){
					found = true;
					break;
				}
			}
			if(!found){
				Utils.printWithDate("No past case data for case ID:" + rec.getID(), Utils.DEBUGLEVEL.DEBUG);
				HashMap<String, String> newCase = new HashMap<String, String>();
				newCase.put("Similarity", Integer.toString(noCaseData));
				newCase.put("RecID", rec.getID());
				pastCases.add(newCase);
			}
		}
		//Sort cases for similarity, descending
		//Faster to find highest similarity, but clearer what happens if we do a complete sort
		pastCases.sort(new Comparator<HashMap<String, String>>(){
			@Override
			public int compare(HashMap<String, String> o1,
					HashMap<String, String> o2) {
				if(Integer.parseInt(o1.get("Similarity")) < Integer.parseInt(o2.get("Similarity"))){
					return 1;
				}
				else{
					return -1;
				}
			}
		});
		//Output similarities for debugging purposes
		for(HashMap<String, String> pastCase: pastCases){
			Utils.printWithDate("Similarity: " + pastCase.get("Similarity") + " ID: " + pastCase.get("RecID"), Utils.DEBUGLEVEL.DEBUG);
		}
		//Use the most similar case and recommend it
		String recID = pastCases.firstElement().get("RecID");
		Utils.printWithDate("Chosen rec ID:" + recID, Utils.DEBUGLEVEL.DEBUG);
		for (Recommendation rec : possibleRecommendations){
			if(rec.getID().equals(recID)){
				result.add(rec);

			    //Maintain order so other recommenders don't freak out
				rec.setLastTimeDisplayed(LocalDateTime.now(Clock.systemUTC()));
			    rec.setLastContext(contextInformation);
			    int displayCounter;
				//update temporary histories
			    //filter history
			    if(temporaryRecHistory.containsKey(rec.getID())){ 
				    displayCounter = temporaryRecHistory.get(rec.getID());
			    }
			    else{
			    	displayCounter = 0;
			    }
			    temporaryRecHistory.put(rec.getID(), displayCounter + 1);
			    //main history
			    addRecToMainHistory(rec);
			    //Logging
			    Utils.printWithDate("Chosen recommendation: " + rec.getTitle(), Utils.DEBUGLEVEL.DEBUG);
				main.myLogger.addNewObjectToArray(main.myLogger.sentRecArray, rec.getTitle(), "chosen recommendation");
				
				break;
			}
		}
		//Shouldn't happen, safety measure so we never return no recommendation
		if(result.isEmpty()){
			Utils.printWithDate("Couldn't find recommendation based on past cases, guessing :(", Utils.DEBUGLEVEL.WARNING);
			return chooseRandomRecommendation(possibleRecommendations, contextInformation);
		}
		else{
			return result;
		}
	}
	
	public int calculateCaseRating(HashMap<String, String> contextInformation, HashMap<String, String> pastCase){
		int rating = 0;
		rating = calculateCaseSimilarity(contextInformation, pastCase);
		if(pastCase.containsKey("UserRating")){
			int userRating = Integer.parseInt(pastCase.get("UserRating"));
			switch (userRating){
			case 1: 
				rating = rating + negativeRating;
				break;
			case 2:
				rating = rating + neutralRating;
				break;
			case 3:
				rating = rating + positiveRating;
				break;
			}
		}
		return rating;
	}
	
	public int calculateCaseSimilarity(HashMap<String, String> contextInformation, HashMap<String, String> pastCase){
		int rating= 0;
		if(contextInformation.containsKey("lightConditionOutdoors") && pastCase.containsKey("lightConditionOutdoors")){
			if(contextInformation.get("lightConditionOutdoors").equals(pastCase.get("lightConditionOutdoors"))){
				rating = rating + lightConditionOutdoorsEqual;
			}
			else{
				rating = rating + lightConditionOutdoorsDifferent;
			}
		}
		else{
			rating = rating + missingInfo;
		}
		if(contextInformation.containsKey("weatherCondition") && pastCase.containsKey("weatherCondition")){
			String currentWeather = contextInformation.get("weatherCondition");
			String pastWeather = contextInformation.get("weatherCondition");
			switch (currentWeather){
			case "veryBad":
				switch (pastWeather){
				case "veryBad":
					rating = rating + weatherConditionEqual;
					break;
				case "bad":
					rating = rating + weatherConditionSimilar;
					break;
				case "ok":
					rating = rating + weatherConditionDiverging;
					break;
				case "good":
					rating = rating + weatherConditionDifferent;
					break;
				case "veryGood":
					rating = rating + weatherConditionOpposite;
					break;
				}
				break;
			case "bad":
				switch (pastWeather){
				case "veryBad":
					rating = rating + weatherConditionSimilar;
					break;
				case "bad":
					rating = rating + weatherConditionEqual;
					break;
				case "ok":
					rating = rating + weatherConditionSimilar;
					break;
				case "good":
					rating = rating + weatherConditionDiverging;
					break;
				case "veryGood":
					rating = rating + weatherConditionDifferent;
					break;
				}
				break;
			case "ok":
				switch (pastWeather){
				case "veryBad":
					rating = rating + weatherConditionDiverging;
					break;
				case "bad":
					rating = rating + weatherConditionSimilar;
					break;
				case "ok":
					rating = rating + weatherConditionEqual;
					break;
				case "good":
					rating = rating + weatherConditionSimilar;
					break;
				case "veryGood":
					rating = rating + weatherConditionDiverging;
					break;
				}
				break;
			case "good":
				switch (pastWeather){
				case "veryBad":
					rating = rating + weatherConditionDifferent;
					break;
				case "bad":
					rating = rating + weatherConditionDiverging;
					break;
				case "ok":
					rating = rating + weatherConditionSimilar;
					break;
				case "good":
					rating = rating + weatherConditionEqual;
					break;
				case "veryGood":
					rating = rating + weatherConditionSimilar;
					break;
				}
				break;
			case "veryGood":
				switch (pastWeather){
				case "veryBad":
					rating = rating + weatherConditionOpposite;
					break;
				case "bad":
					rating = rating + weatherConditionDifferent;
					break;
				case "ok":
					rating = rating + weatherConditionDiverging;
					break;
				case "good":
					rating = rating + weatherConditionSimilar;
					break;
				case "veryGood":
					rating = rating + weatherConditionEqual;
					break;
				}
				break;
			}
		}
		else{
			rating = rating + missingInfo;
		}
		if(contextInformation.containsKey("userMood") && pastCase.containsKey("userMood")){
			String currentMood = contextInformation.get("userMood");
			String pastMood = contextInformation.get("userMood");
			if(currentMood.equals(pastMood)){
				rating = rating + moodEqual;
			}
			else if((currentMood.equals("goodMood")&&pastMood.equals("badMood"))||(currentMood.equals("badMood")&&pastMood.equals("goodMood"))){
				rating = rating + moodDifferent;
			}
			else{
				rating = rating + moodSimilar;
			}
		}
		else{
			rating = rating + missingInfo;
		}
		if(contextInformation.containsKey("daytime") && pastCase.containsKey("daytime")){
			String currentDaytime = contextInformation.get("daytime");
			String pastDaytime = pastCase.get("daytime");
			switch(currentDaytime){
				case "night":
					switch(pastDaytime){
					case "night":
						rating = rating + daytimeEqual;
						break;
					case "morning":
						rating = rating + daytimeSimilar;
						break;
					case "forenoon":
						rating = rating + daytimeDifferent;
						break;
					case "lunchtime":
						rating = rating + daytimeOpposite;
						break;
					case "afternoon":
						rating = rating + daytimeDifferent;
						break;
					case "evening":
						rating = rating + daytimeSimilar;
						break;
					}
					break;
				case "morning":
					switch(pastDaytime){
					case "night":
						rating = rating + daytimeSimilar;
						break;
					case "morning":
						rating = rating + daytimeEqual;
						break;
					case "forenoon":
						rating = rating + daytimeSimilar;
						break;
					case "lunchtime":
						rating = rating + daytimeDifferent;
						break;
					case "afternoon":
						rating = rating + daytimeOpposite;
						break;
					case "evening":
						rating = rating + daytimeDifferent;
						break;
					}
					break;
				case "forenoon":
					switch(pastDaytime){
					case "night":
						rating = rating + daytimeDifferent;
						break;
					case "morning":
						rating = rating + daytimeSimilar;
						break;
					case "forenoon":
						rating = rating + daytimeEqual;
						break;
					case "lunchtime":
						rating = rating + daytimeSimilar;
						break;
					case "afternoon":
						rating = rating + daytimeDifferent;
						break;
					case "evening":
						rating = rating + daytimeOpposite;
						break;
					}
					break;
				case "lunchtime":
					switch(pastDaytime){
					case "night":
						rating = rating + daytimeOpposite;
						break;
					case "morning":
						rating = rating + daytimeDifferent;
						break;
					case "forenoon":
						rating = rating + daytimeSimilar;
						break;
					case "lunchtime":
						rating = rating + daytimeEqual;
						break;
					case "afternoon":
						rating = rating + daytimeSimilar;
						break;
					case "evening":
						rating = rating + daytimeDifferent;
						break;
					}
					break;
				case "afternoon":
					switch(pastDaytime){
					case "night":
						rating = rating + daytimeDifferent;
						break;
					case "morning":
						rating = rating + daytimeOpposite;
						break;
					case "forenoon":
						rating = rating + daytimeDifferent;
						break;
					case "lunchtime":
						rating = rating + daytimeSimilar;
						break;
					case "afternoon":
						rating = rating + daytimeEqual;
						break;
					case "evening":
						rating = rating + daytimeSimilar;
						break;
					}
					break;
				case "evening":
					switch(pastDaytime){
					case "night":
						rating = rating + daytimeSimilar;
						break;
					case "morning":
						rating = rating + daytimeDifferent;
						break;
					case "forenoon":
						rating = rating + daytimeOpposite;
						break;
					case "lunchtime":
						rating = rating + daytimeDifferent;
						break;
					case "afternoon":
						rating = rating + daytimeSimilar;
						break;
					case "evening":
						rating = rating + daytimeEqual;
						break;
					}
					break;
			}
		}
		else{
			rating = rating + missingInfo;
		}
		return rating;
	}
	
	public Vector<Recommendation> chooseRecommendationsOfRandomCategory(Vector<Recommendation> recommendations){
		
		recommendationsToShow = new Vector<Recommendation>();
		debugString = "";
		
		//choose a random category
	    int rnd = new Random().nextInt(main.possibleCategories.size());
		String chosenCategory =  main.possibleCategories.get(rnd);
	    
	    Utils.printWithDate("Recommendations of category : " + chosenCategory, Utils.DEBUGLEVEL.DEBUG);
		
		for(Recommendation rec : recommendations){
			
			if(rec.getCategories().contains(chosenCategory)){			
				recommendationsToShow.add(rec);

				debugString = rec.getTitle() + ", ";
				main.myLogger.addNewObjectToArray(main.myLogger.sentRecArray, rec.getTitle(), "chosen recommendation");
			}
		}
		
		Utils.printWithDate("Chosen recommendations: " + debugString, Utils.DEBUGLEVEL.DEBUG);
		
		return recommendationsToShow;
	}
	
	public Vector<Recommendation> chooseRandomRecommendation(Vector<Recommendation> recommendations, HashMap<String, String> contextInformation){
		
		//stores all recommendations. needed if all recommendations were displayed more than "maximumDisplayCount" times in the last "historyCleanupInterval" minutes.
		Vector<Recommendation> helperVector = new Vector<Recommendation>();
		for(Recommendation r : recommendations){
			helperVector.add(r);
		}
		
		recommendationsToShow = new Vector<Recommendation>();
		Recommendation rec;
		
		//clears history every hour
		long diffInMinutes = Duration.between(lastHistoryCleanup, LocalDateTime.now(Clock.systemUTC())).toMinutes();
		
		Utils.printWithDate(diffInMinutes + " minutes since last history cleanup", Utils.DEBUGLEVEL.DEBUG);
		
		if(diffInMinutes >= historyCleanupInterval){
			temporaryRecHistory.clear();
			lastHistoryCleanup = LocalDateTime.now(Clock.systemUTC());
			
			Utils.printWithDate("temporary history cleared", Utils.DEBUGLEVEL.DEBUG);
		}
		
		//Prevents the system of displaying the same recommendation more than "maximumDisplayCount" times
		int displayCounter = 0;
		
		do{		
			//choose a random recommendation
		    int rnd = new Random().nextInt(recommendations.size());
		    rec = recommendations.get(rnd);
		    
		    if(temporaryRecHistory.containsKey(rec.getID())){ 
			    displayCounter = temporaryRecHistory.get(rec.getID());
		    }
		    else{
		    	displayCounter = 0;
		    }

		    Utils.printWithDate("Recommendation(" + rec.getTitle() + ") was displayed " + displayCounter + " times since last cleanup.", Utils.DEBUGLEVEL.DEBUG);
		    
		    if(displayCounter >= maximumDisplayCount){
		    	recommendations.remove(rnd);
		    	Utils.printWithDate(recommendations.size() + " possible recommendations left", Utils.DEBUGLEVEL.DEBUG);
		    }
		    
		    //if no recommendations are left to display, reset history and load recommendations that were stored in "helperVector"
		    if(recommendations.isEmpty()){
				for(Recommendation r : helperVector){
					recommendations.add(r);
				}
		    	temporaryRecHistory.clear();
		    	lastHistoryCleanup = LocalDateTime.now(Clock.systemUTC());
		    	
		    	Utils.printWithDate("temporary history cleared", Utils.DEBUGLEVEL.DEBUG);
		    	Utils.printWithDate(recommendations.size() + " possible recommendations left", Utils.DEBUGLEVEL.DEBUG);
		    }
		} 
		while(displayCounter >= maximumDisplayCount);
	    
		//vector contains all recommendations that should be displayed
	    recommendationsToShow.add(rec);
	    
	    //update recommendation with current timestamp and context
	    rec.setLastTimeDisplayed(LocalDateTime.now(Clock.systemUTC()));
	    rec.setLastContext(contextInformation);
	    
	    //update temporary histories
	    //filter history
	    temporaryRecHistory.put(rec.getID(), displayCounter + 1);
	    //main history
	    addRecToMainHistory(rec);

	    //Logging
	    Utils.printWithDate("Chosen recommendation: " + rec.getTitle(), Utils.DEBUGLEVEL.DEBUG);
		main.myLogger.addNewObjectToArray(main.myLogger.sentRecArray, rec.getTitle(), "chosen recommendation");
		
		return recommendationsToShow;
	}
	
	private void addRecToMainHistory(Recommendation rec){
	    if(main.recommendationsHistory.containsKey(rec.getID())){
	    	main.recommendationsHistory.replace(rec.getID(), rec);
	    }
	    else{
	    	main.recommendationsHistory.put(rec.getID(), rec);
	    }
	}

}
