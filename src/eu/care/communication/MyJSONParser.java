package eu.care.communication;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import eu.care.main.DemonstratorMain;
import eu.care.main.Utils;
import eu.care.recommenderengine.Recommendation;

/**
 * 
 * @author Stephan Hammer
 *
 */
public class MyJSONParser {

	private DemonstratorMain main;
	public JSONParser myParser;

	public MyJSONParser(DemonstratorMain main) {
		this.main = main;
		myParser = new JSONParser();
	}

	@SuppressWarnings("unchecked")
	public HashMap<String, String> decodeTriggerMessage(String triggerMsg) {
		System.out.println("=== decode Trigger-Message ===");

		HashMap<String, String> triggerInformation = new HashMap<String, String>();
		
		try {
			JSONObject jsonMsg = (JSONObject) myParser.parse(triggerMsg);
			JSONArray userPresence = (JSONArray) jsonMsg.get("presence");

			// User present?
			if (userPresence.size() > 0) {
				triggerInformation.put("user_presence", "user_present");
				
				// get current triggers
				JSONArray currentTriggers = (JSONArray) jsonMsg.get("triggers");
				Utils.printWithDate("There are " + currentTriggers.size() + " triggers.", Utils.DEBUGLEVEL.GENERAL);

				//analyze triggers one by one
				if (currentTriggers.size() > 0) {					
					Iterator<JSONObject> triggerIterator = currentTriggers.iterator();
					while (triggerIterator.hasNext()) {
						JSONObject trigger = triggerIterator.next();
						
						//Trigger: badRoomClimate
						JSONArray badRoomClimateTrigger = (JSONArray) trigger.get("badRoomClimate");
						
						if(badRoomClimateTrigger != null){
							triggerInformation.put("room_airquality", "humid_room");
							main.myLogger.addNewObjectToArray(main.myLogger.triggerArray, "humid_room", "requires to open the windows");
						}

						//Trigger: noMovement
						JSONArray noMovementTrigger = (JSONArray) trigger.get("noMovement");
						if(noMovementTrigger != null){
							triggerInformation.put("user_movement", "noMovement");
							main.myLogger.addNewObjectToArray(main.myLogger.triggerArray, "movement", "requires recommendations for physical exercises");
						}
					}
				}
			} else {
				triggerInformation.put("user_presence", "user_absent");
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return triggerInformation;
	}
	
	@SuppressWarnings("unchecked")
	public Vector<Recommendation> getRecommendationsFromJSON(String jsonRecommendations){
		Vector<Recommendation> possibleRecommendations = new Vector<Recommendation>();
		
		JSONArray recommendations;
		try {
			recommendations = (JSONArray) myParser.parse(jsonRecommendations);
			Utils.printWithDate("There are " + recommendations.size() + " recommendations.", Utils.DEBUGLEVEL.GENERAL);

			if (recommendations.size() > 0) {
				//delete categories of last recommendation generation
				main.possibleCategories.clear();
				
				//analyze recommendations one by one
				Iterator<JSONObject> recommendationsIterator = recommendations.iterator();
				while (recommendationsIterator.hasNext()) {
					Recommendation rec = new Recommendation();
									
					//get json-representation of recommendation
					JSONObject jsonRec = recommendationsIterator.next();
					rec.setJsonRepresentation(jsonRec);
					
					//get id of recommendation
					JSONObject idObj = (JSONObject) jsonRec.get("field_id");
					JSONArray idArray = (JSONArray) idObj.get("und");
					JSONObject idJSONObj = (JSONObject) idArray.get(0);
					String id = (String) idJSONObj.get("value");
					rec.setID(id);
					
					//get title of recommendation
					String title = (String) jsonRec.get("title");
					rec.setTitle(title);
					
					//get categories of recommendation
					Vector<String> categories = new Vector<String>();
					if(jsonRec.get("field_category").getClass().getSimpleName().equals("JSONObject")){
						JSONObject objToPars = (JSONObject) jsonRec.get("field_category");
						categories = parseJSONArrayToStringVector(objToPars, "und", "tid");
					}

					rec.setCategories(categories);
					addCategoriesToMainCategoryVector(categories);
					
					//get tags of recommendation
					if(jsonRec.get("field_tags").getClass().getSimpleName().equals("JSONObject")){
						JSONObject objToPars = (JSONObject) jsonRec.get("field_tags");
						rec.setTags(parseJSONArrayToStringVector(objToPars, "und", "tid"));
					}
					else{
						rec.setTags(new Vector<String>());
					}

					possibleRecommendations.add(rec);
					
					main.myLogger.addNewObjectToArray(main.myLogger.receivedRecArray, title, "possible recommendation");
				}
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return possibleRecommendations;
	}
	
	public HashMap<String, String> decodeUserResponse(String userResponse){
		System.out.println("=== decode user response ===");

		HashMap<String, String> userResponseInDetail = new HashMap<String, String>();
		
		try {
			JSONObject jsonMsg = (JSONObject) myParser.parse(userResponse);

			String contentID = jsonMsg.get("id").toString();
			userResponseInDetail.put("contentID", contentID);
			
			JSONArray categoryArray = (JSONArray) jsonMsg.get("categoryTID");
			
			String categories = "";
			if (categoryArray.size() > 0) {
				Iterator<String> objIterator = categoryArray.iterator();
				while (objIterator.hasNext()) {
					categories = categories + objIterator.next() + ",";
				}
			}
			
			categories = main.cutOffLastCommaOfString(categories);

			userResponseInDetail.put("contentCategory", categories);
			
			String displayDate = jsonMsg.get("display").toString();
			userResponseInDetail.put("displayDate", displayDate);
			
			String userRating = jsonMsg.get("rating").toString();
			userResponseInDetail.put("userRating", userRating);
			
			JSONObject quizAnswer = (JSONObject) jsonMsg.get("answer");
			
			String quizResult = quizAnswer.get("value").toString();
			userResponseInDetail.put("quizResult", quizResult);
			
			String userAnswer = quizAnswer.get("text").toString();
			userResponseInDetail.put("userAnswer", userAnswer);
			
			String contentType = quizAnswer.get("type").toString();
			userResponseInDetail.put("contentType", contentType);		
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return userResponseInDetail;
	}
	
	@SuppressWarnings("unchecked")
	private Vector<String> parseJSONArrayToStringVector(JSONObject objToPars, String arrayName, String requestedObj){
		Vector<String> requestedFields = new Vector<String>();
		
		JSONArray jsonArray = (JSONArray) objToPars.get(arrayName);

		if (jsonArray.size() > 0) {
			Iterator<JSONObject> objIterator = jsonArray.iterator();
			while (objIterator.hasNext()) {
				JSONObject reqJSONObj = objIterator.next();
				requestedFields.add((String) reqJSONObj.get(requestedObj));
			}
		}
		
		return requestedFields;
	}
	
	public String getDataFromSunTimesJSON(JSONObject objToPars, String requestedObj){
		String requestedData = "";
		
		requestedData = ((JSONObject)((JSONObject) objToPars.get("sunTimes")).get(requestedObj)).get("$date").toString();
		
		return requestedData;
	}
	
	public String getWeatherConditionRating(JSONObject objToPars){
		String conditionRating = "";
		
		conditionRating = ((JSONObject) objToPars.get("conditionRating")).get("current").toString();
		
		return conditionRating;
	}
	
	public Vector<String> getImagesOfRecommendations(String jsonRecommendations){
			
		Vector<String> imageNames = new Vector<String>();
		
		JSONArray recommendations;
		try {
			recommendations = (JSONArray) myParser.parse(jsonRecommendations);
			Iterator<JSONObject> recommendationsIterator = recommendations.iterator();
			//analyze recommendations one by one
			while (recommendationsIterator.hasNext()) {
				JSONObject jsonRec = recommendationsIterator.next();
				//get name of recommendation image
				JSONObject imageObj = (JSONObject) jsonRec.get("field_backgroundimage");
				JSONArray imageArray = (JSONArray) imageObj.get("und");
				JSONObject imageJSONObj = (JSONObject) imageArray.get(0);
				String imageName = (String) imageJSONObj.get("uri");
				imageName = imageName.substring(imageName.lastIndexOf("/")+1);
				imageNames.add(imageName);
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return imageNames;
	}
	
//	private String getRoom(JSONObject objectInfo) {
//		String currentRoom = "";
//
//		JSONObject roomInfo = (JSONObject) objectInfo.get("room");
//		currentRoom = (String) roomInfo.get("roomType");
//
//		return currentRoom;
//	}

	public String getJSONMessage(String fileName) {
	
		JSONObject jsonObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		
		try {
			jsonObject = (JSONObject) myParser.parse(new FileReader(fileName));
		} catch (IOException | ParseException | ClassCastException e) {
			e.printStackTrace();
				try {
					jsonArray = (JSONArray) myParser.parse(new FileReader(fileName));
					return jsonArray.toString();
				} catch (IOException | ParseException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
		}
		return jsonObject.toString();
	}
	
	//collects all possible categories of this run
	private void addCategoriesToMainCategoryVector(Vector<String> additionalCategories){
		for(String category : additionalCategories){
			if(!main.possibleCategories.contains(category)){
				main.possibleCategories.add(category);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public String encodeMessageToScreen(Vector<JSONObject> contentToShow, String type){
		JSONObject jsonMessage = new JSONObject();
		
		JSONArray contentArray = new JSONArray();
		
		for(JSONObject content : contentToShow){
			contentArray.add(content);
		}
		
		jsonMessage.put("content", contentArray);
		jsonMessage.put("type", type);
		
		
		return jsonMessage.toString();
	}
}
