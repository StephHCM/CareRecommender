package eu.care.recommenderengine;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

import org.json.simple.JSONObject;

import eu.care.main.DemonstratorMain;
import eu.care.main.Utils;

/**
 * 
 * @author Stephan Hammer
 *
 */
public class RecommendationFilter {
	
	private DemonstratorMain main;
	private Vector<JSONObject> recommendationsToShow;
	private String debugString = "";
	private HashMap<String, Integer> temporaryRecHistory;
	private LocalDateTime lastHistoryCleanup;
	private long historyCleanupInterval = 60; //in minutes
	private int maximumDisplayCount = 3;
	
	public RecommendationFilter(DemonstratorMain main, long historyCleanupInterval, int maximumDisplayCount){
		this.main = main;
		this.historyCleanupInterval = historyCleanupInterval;
		this.maximumDisplayCount = maximumDisplayCount;
		
		temporaryRecHistory = new HashMap<String, Integer>();
		lastHistoryCleanup = LocalDateTime.now(Clock.systemUTC());
	}
	
	public Vector<JSONObject> useNoFilter(Vector<Recommendation> recommendations){
		
		recommendationsToShow = new Vector<JSONObject>();
		debugString = "";
		
		for(Recommendation rec : recommendations){
			recommendationsToShow.add(rec.getJsonRepresentation());

			debugString = rec.getTitle() + ", ";
			main.myLogger.addNewObjectToArray(main.myLogger.sentRecArray, rec.getTitle(), "chosen recommendation");
		}
		
		Utils.printWithDate("Chosen recommendations: " + debugString, Utils.DEBUGLEVEL.DEBUG);
		
		return recommendationsToShow;
	}
	
	public Vector<JSONObject> chooseRecommendationsOfRandomCategory(Vector<Recommendation> recommendations){
		
		recommendationsToShow = new Vector<JSONObject>();
		debugString = "";
		
		//choose a random category
	    int rnd = new Random().nextInt(main.possibleCategories.size());
		String chosenCategory =  main.possibleCategories.get(rnd);
	    
	    Utils.printWithDate("Recommendations of category : " + chosenCategory, Utils.DEBUGLEVEL.DEBUG);
		
		for(Recommendation rec : recommendations){
			
			if(rec.getCategories().contains(chosenCategory)){			
				recommendationsToShow.add(rec.getJsonRepresentation());

				debugString = rec.getTitle() + ", ";
				main.myLogger.addNewObjectToArray(main.myLogger.sentRecArray, rec.getTitle(), "chosen recommendation");
			}
		}
		
		Utils.printWithDate("Chosen recommendations: " + debugString, Utils.DEBUGLEVEL.DEBUG);
		
		return recommendationsToShow;
	}
	
	public Vector<JSONObject> chooseRandomRecommendation(Vector<Recommendation> recommendations, HashMap<String, String> contextInformation){
		
		//stores all recommendations. needed if all recommendations were displayed more than "maximumDisplayCount" times in the last "historyCleanupInterval" minutes.
		Vector<Recommendation> helperVector = new Vector<Recommendation>();
		for(Recommendation r : recommendations){
			helperVector.add(r);
		}
		
		recommendationsToShow = new Vector<JSONObject>();
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
	    recommendationsToShow.add(rec.getJsonRepresentation());
	    
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
