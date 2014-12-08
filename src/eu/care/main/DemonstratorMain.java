package eu.care.main;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import eu.care.communication.*;
import eu.care.context.ContextModel;
import eu.care.context.TimeInterpreter;
import eu.care.recommenderengine.*;
import eu.care.user.UserModel;

/**
 * 
 * @author Stephan Hammer
 *
 */
public class DemonstratorMain {
	
	//config file
	public String configFile;
	private JSONObject jsonConfig;

	//Helper
	public RecSysLogger myLogger;
	public MyJSONParser jsonParser;

	//Connections
	public static MyXMPPConnection xmppConnection;
	public static MyMongoDBConnection mongoDBConnection;
	private DatabaseConnection dbConnection;
	private static boolean shutdown = false;
	private String serverUri = "";

	//Context Information
	public HashMap<String, String> contextInformation;
	
	public ContextModel context;
	private int weatherConditionUpdateInterval = 60; //in minutes
	private int solarAltitudeUpdateInterval = 24; //in hours
	
	public UserModel user;	
	private int userMoodUpdateInterval = 120; //in minutes
	
	//Recommender System
	private Vector<JSONObject> recommendationsToShow;
	private Vector<Recommendation> possibleRecommendations;
	public HashMap<String, Recommendation> recommendationsHistory;
	public Vector<String> possibleCategories;
	private RecommendationFilter recommenderFilter;
	private long historyCleanupInterval = 60; //in minutes
	private int maximumDisplayCount = 3;
	
	// thread synchronization
	private static CountDownLatch latch_wait_XMPP = new CountDownLatch(1);
	public static CountDownLatch latch_wait_shutdown = new CountDownLatch(2);

	// development helper
	public boolean useXMPPConnection = true;
	public boolean useMongoDBConnection = true;
	private boolean getTriggerFromJSONFile = false;

	public DemonstratorMain(String configFile) {

		// Initialization
		jsonParser = new MyJSONParser(this);
		recommendationsHistory = new HashMap<String, Recommendation>();
	
		createShutDownHook();
		
		//load config-file		
		this.configFile = configFile;
		System.out.println("Using config file: " + configFile);

		try {
			jsonConfig = (JSONObject) jsonParser.myParser.parse((jsonParser.getJSONMessage(configFile)));
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		
		//load settings from config-file
		serverUri = ((JSONObject)jsonConfig.get("Drupal")).get("server").toString();
		//for debugging and offline testing
		useXMPPConnection = Boolean.parseBoolean(((JSONObject)jsonConfig.get("debugging")).get("useXMPPConnection").toString());
		useMongoDBConnection = Boolean.parseBoolean(((JSONObject)jsonConfig.get("debugging")).get("useMongoDBConnection").toString());
		getTriggerFromJSONFile = Boolean.parseBoolean(((JSONObject)jsonConfig.get("debugging")).get("getTriggerFromJSONFile").toString());
		Utils.systemDebugLevel = ((Long)((JSONObject)jsonConfig.get("debugging")).get("debuglevel")).intValue();
		
		// context update intervals (stored in config-file)
		solarAltitudeUpdateInterval = Integer.parseInt(((JSONObject)jsonConfig.get("others")).get("solarAltitudeUpdateInterval").toString());
		weatherConditionUpdateInterval = Integer.parseInt(((JSONObject)jsonConfig.get("others")).get("weatherConditionUpdateInterval").toString());
		context = new ContextModel(weatherConditionUpdateInterval, solarAltitudeUpdateInterval);
		
		userMoodUpdateInterval = Integer.parseInt(((JSONObject)jsonConfig.get("others")).get("userMoodUpdateInterval").toString());
		user = new UserModel(userMoodUpdateInterval);
		
		//recommendation filter: maximum number of repeated display of a recommendation + time interval for history cleanup
		historyCleanupInterval = Long.parseLong(((JSONObject)jsonConfig.get("others")).get("tempRecHistoryCleanupInterval").toString());
		maximumDisplayCount = Integer.parseInt(((JSONObject)jsonConfig.get("others")).get("maximumDisplayCountRec").toString());
		possibleCategories = new Vector<String>();
		recommenderFilter = new RecommendationFilter(this, historyCleanupInterval, maximumDisplayCount);
		
		// Initialize Logger
		myLogger = new RecSysLogger();
		
		// Initialize DrupalDB Connection
		dbConnection = new DatabaseConnection(this);
		
		// Initialize MongoDB Connection
		if(useMongoDBConnection){
			mongoDBConnection = new MyMongoDBConnection((JSONObject)jsonConfig.get("MongoDB"));
			mongoDBConnection.connect();
			if (shutdown) return;
		}

		// Initialize XMPP Connection
		if(useXMPPConnection){
			xmppConnection = new MyXMPPConnection(this);
			xmppConnection.connect(useXMPPConnection);
			if (shutdown) return;
		}
		
		// only for debugging: comment out the needed trigger message
		if (getTriggerFromJSONFile) {
			System.out.println("-------------GET TRIGGER MESSAGE FROM FILE-------------");
			String triggerMessage = "";

			//trigger message: no user in front of display
//			triggerMessage = jsonParser.getJSONMessage("trigger_absent.json");
				
			//trigger message: user in front of display => recommendation needed
			triggerMessage = jsonParser.getJSONMessage("trigger_user.json");
				
			//trigger message: user in front of display + bad roomclimate => specific recommendation needed
//			triggerMessage = jsonParser.getJSONMessage("trigger_user_roomclimate.json");

			Utils.printWithDate("Trigger-Message: " + triggerMessage, Utils.DEBUGLEVEL.DEBUG);
			if(!triggerMessage.equals("")){
				gatherContextInformation(triggerMessage);
			}
		}
		else if(xmppConnection != null){
			xmppConnection.initializeChatListener();
		}

		// waiting until CTRL+C -> shutdownhook opens this latch (works only if application is run from the console) 
		try {
			latch_wait_XMPP.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// gathers all context information that is needed to react appropriately
	// ((1) choose adequate recommendations, (2) stop displaying recommendations)
	public void gatherContextInformation(String triggerMessage) {
		// Decode trigger message and get:
		// (A) user presence (key: user_presence)
		// (B) tags related to specific contextual situations that require specific recommendations
		// (e.g. "humidRoom" -> "open window" (key: room_airquality);
		// "noMovement" -> "do exercise" (key: user_movement))
		contextInformation = new HashMap<String, String>();
		contextInformation = jsonParser.decodeTriggerMessage(triggerMessage);

		// no more recommendations needed
		if (contextInformation.get("user_presence").equals("user_absent")) {
			reactToCurrentContext(contextInformation);
		} 
		// get further context information to get appropriate recommendations: daytime, light conditions (outside), weather, user mood
		else if (contextInformation.get("user_presence").equals("user_present")) {
			// get current daytime (morning, forenoon, lunchtime, afternoon, evening, night)
			LocalTime time = LocalTime.now();
			contextInformation.put("daytime", new TimeInterpreter().getCurrentDayTime(time));
	
			//get sunrise, sunset and weather condition from MongoDB
			String sensorDB = ((JSONObject)jsonConfig.get("MongoDB")).get("dbNameSensors").toString();
			
			// get current light conditions (Outdoors)
			contextInformation.put("lightConditionOutdoors", context.getCurrentLightConditions(this, sensorDB));
			
			// get weather condition
			contextInformation.put("weatherCondition", context.getCurrentWeatherCondition(this, sensorDB));
	
			// get user mood
			//time since last update 
			long diffInMinutes = Duration.between(user.getLastUpdateMood(), LocalDateTime.now(Clock.systemUTC())).toMinutes();

			//if mood is unknown or "older" than interval-time
			if (user.getMood().equals("") || diffInMinutes > user.getUpdateInterval()) {
				requestUserMood();
			}
			else{
				contextInformation.put("userMood", user.getMood());
				reactToCurrentContext(contextInformation);
			}
		}
	}

	// HashMap contextTriggers contains the following information:
	// K: user_presence: user_present || user_absent
	// K: user_mood: badMood || neutralMood || goodMood
	// K: user_movement: NULL || noMovement
	// K: daytime: morning || forenoon || lunchtime || afternoon || evening || night
	// K: room_airquality: NULL || humid_room || ???
	public void reactToCurrentContext(HashMap<String, String> contextInformation) {

		String jsonRecommendations = "";

		if (contextInformation.get("user_presence").equals("user_absent")) {
			// no more recommendations needed
			myLogger.addNewObjectToArray(myLogger.sentRecArray, "no recommendation", "no more recommendations needed");
			
			Utils.printWithDate("User absent", Utils.DEBUGLEVEL.DEBUG);
			myLogger.addNewObjectToArray(myLogger.triggerArray, "user_absent", "user is no longer in front of display");

			Vector<JSONObject> stopContent = new Vector<JSONObject>(); 
			sendContentToOutputDevice(stopContent, "stopRecommendations");

		} else if (contextInformation.get("user_presence").equals("user_present")) {

			String daytime = contextInformation.get("daytime");
			
			//current conditions (outdoors)
			String outdoorConditions = "";
			
			String lightConditions = contextInformation.get("lightConditionOutdoors");
			if (!lightConditions.equals("")) {
				outdoorConditions += lightConditions + ",";
			}
			
			String weatherCondition = contextInformation.get("weatherCondition");
			if (!weatherCondition.equals("")) {
				outdoorConditions += weatherCondition + ",";
			}
			
			outdoorConditions = cutOffLastCommaOfString(outdoorConditions);
		
			String importantTags = "";

			if (contextInformation.containsKey("user_movement")) {
				importantTags += contextInformation.get("user_state") + ",";
			}

			if (contextInformation.containsKey("room_airquality")) {
				importantTags += contextInformation.get("room_airquality") + ",";
			}

			String userMood = contextInformation.get("userMood");
			if (userMood.equals("badMood")) {
				importantTags += userMood + ",";
			}

			importantTags = cutOffLastCommaOfString(importantTags);
			
			//send query to database to receive all possible recommendations
			jsonRecommendations = dbConnection.sendQuery(importantTags, daytime, outdoorConditions);

			// parse jsonRecommendations
			possibleRecommendations = jsonParser.getRecommendationsFromJSON(jsonRecommendations);

			// Filter recommendations
			boolean chooseSingleCategory = Boolean.parseBoolean(((JSONObject)jsonConfig.get("RecSys")).get("filterByCategory").toString());
			boolean chooseSingleRecommendation = Boolean.parseBoolean(((JSONObject)jsonConfig.get("RecSys")).get("chooseSingleRecommendation").toString());
			
			if(chooseSingleRecommendation){
				recommendationsToShow = recommenderFilter.chooseRandomRecommendation(possibleRecommendations, contextInformation);
			}
			else if(chooseSingleCategory){
				recommendationsToShow = recommenderFilter.chooseRecommendationsOfRandomCategory(possibleRecommendations);
			}
			else{
				recommendationsToShow = recommenderFilter.useNoFilter(possibleRecommendations);
			}

			//send chosen recommendations to output device
			sendContentToOutputDevice(recommendationsToShow, "recommendations");
		}
	}

	private void requestUserMood() {	
		JSONArray askForMoodContentInArray = new JSONArray();
		JSONObject askForMoodContent = new JSONObject();
			
		String jsonAskForMoodContent = dbConnection.sendSurvey("mood");
		if(!jsonAskForMoodContent.equals("[]")){
			try {
				askForMoodContentInArray = (JSONArray) jsonParser.myParser.parse(jsonAskForMoodContent);
				askForMoodContent = (JSONObject) askForMoodContentInArray.get(0);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				
			Vector<JSONObject> askForMoodVector = new Vector<JSONObject>();
			askForMoodVector.add(askForMoodContent);
				
			sendContentToOutputDevice(askForMoodVector, "surveys");
		}
	}
	
	private void sendContentToOutputDevice(Vector<JSONObject> contentToShow, String type){
		//if care display is used
		if(useXMPPConnection){
			xmppConnection.sendContentToScreen(contentToShow, type);
		}
		//for debugging without care_screen or output on other devices such as robots
		else{
			Utils.printWithDate("Sent a " + type  + " to output device. json-representation: " + contentToShow.toString(), Utils.DEBUGLEVEL.DEBUG);
			
			//if type is "survey" you have to simulate the user's answer
			if(type.equals("surveys")){
				//user_mood: badMood || neutralMood || goodMood
				contextInformation.put("userMood", "neutralMood");
				reactToCurrentContext(contextInformation);
			}
			else if(type.equals("recommendations")){
				//get images of all chosen recommendations
				Vector<String> imageNames = jsonParser.getImagesOfRecommendations(contentToShow.toString());
				for(String imageName : imageNames){
					//get image url
					String imageURL = serverUri + "/care_prototype/sites/default/files/" + imageName;
					Utils.printWithDate("image url = " + imageURL, Utils.DEBUGLEVEL.DEBUG);
				}
				
				//TODO Insert Java-GUI etc.
			}
		}
	}
	
	private static void createShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
	        
			@Override
			public void run() {
				// open latch
				latch_wait_XMPP.countDown();
				
				shutdown = true;
				Utils.printWithDate("Shutting down ...", Utils.DEBUGLEVEL.WARNING);

	            //check if Objects were created
	    		if (xmppConnection != null) 	xmppConnection.closeConnection();
	    		if (mongoDBConnection != null) 	mongoDBConnection.closeConnection();

				// waiting until XMPP disconnected -> main opens this latch
				try {
					latch_wait_shutdown.await();
					
					Utils.printWithDate("Shut down", Utils.DEBUGLEVEL.WARNING);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});
	}
	
	public String cutOffLastCommaOfString(String toCut){
			
		//cut off last ","
		if (!toCut.equals("")) {
			toCut = toCut.substring(0, toCut.length() - 1);
		}
		
		return toCut;
	}

	public static void main(String[] args) {
		String configFile = "";
		
		for(String arg : args){
			if(arg.contains("config") && arg.contains(".json")){
				configFile = arg;
			}
		}
		
		if(configFile.equals("")) new DemonstratorMain("config_test.json");
		else new DemonstratorMain(configFile);
	}
}
