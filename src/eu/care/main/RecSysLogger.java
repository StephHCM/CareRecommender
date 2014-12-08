package eu.care.main;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * 
 * @author Stephan Hammer
 *
 */
@SuppressWarnings("unchecked")
public class RecSysLogger {
	
	public JSONObject mainObj, triggerObj, dbQueryObj, receivedRecObj, sentRecObj;
	public JSONArray triggerArray, receivedRecArray, sentRecArray;

	public RecSysLogger(){
		//create json-message
		mainObj = new JSONObject();
		
		//initialize important json-objects
		triggerObj = new JSONObject();
		triggerArray = new JSONArray();
		
		dbQueryObj = new JSONObject();
		
		receivedRecObj = new JSONObject();
		receivedRecArray = new JSONArray();
		
		sentRecObj = new JSONObject();
		sentRecArray = new JSONArray();
	}
	
	public void addDatabaseQuery(String uri){
		dbQueryObj.put("uri", uri);
	}
	
//	public void addJSONMessage(JSONObject parent, String jsonMessage){
//		parent.put("jsonMessage", jsonMessage);
//	}

	public void addNewObjectToArray(JSONArray parent, String type, String infos){
		JSONObject newObject = new JSONObject();
		newObject.put("type", type);
		newObject.put("infos", infos);
		
		parent.add(newObject);
	}
	
	public void addErrorMessage(JSONObject parent, String error){
		parent.put("error", error);
	}
	
	public String createLoggingMessage(){	
		triggerObj.put("objects", triggerArray);
		mainObj.put("triggers", triggerObj);
		
		mainObj.put("databaseQuery", dbQueryObj);
		
		receivedRecObj.put("objects", receivedRecArray);
		mainObj.put("receivedRecommendations", receivedRecObj);
		
		sentRecObj.put("objects", sentRecArray);
		mainObj.put("sentRecommendations", sentRecObj);
		
		return mainObj.toJSONString();
	}
	
	public void clearLog(){
		mainObj.clear();
		triggerObj.clear();
		triggerArray.clear();
		dbQueryObj.clear();
		receivedRecObj.clear();
		receivedRecArray.clear();
		sentRecObj.clear();
		sentRecArray.clear();
		
		Utils.printWithDate("log cleared", Utils.DEBUGLEVEL.DEBUG);
	}
}
