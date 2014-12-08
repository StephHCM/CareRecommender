package eu.care.communication;

import java.io.IOException;

import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import eu.care.main.DemonstratorMain;
import eu.care.main.Utils;

/**
 * 
 * @author Stephan Hammer
 *
 */
public class DatabaseConnection {

	//config file
	private JSONObject jsonConfig;
	
	private DemonstratorMain main;
	private static String hostDrupal;
	private static String language = "en";
	private static String uriBase;

	public DatabaseConnection(DemonstratorMain main) {
		this.main = main;
		//load config-information
		try {
			jsonConfig = (JSONObject) main.jsonParser.myParser.parse((main.jsonParser.getJSONMessage(main.configFile)));
			hostDrupal = ((JSONObject)jsonConfig.get("Drupal")).get("server").toString();
			language = ((JSONObject)jsonConfig.get("others")).get("language").toString();
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//Example: http://care-test.multimedia.hs-augsburg.de/care_prototype/?q=de/rest/views/
		uriBase = "http://" + hostDrupal + "/care_prototype/?q=" + language + "/rest/views/";
	}

	//Example: uriBase + /rec.json&daytimes=night&outdoorConditions=dark&tags=badMood
	public String sendQuery(String tags, String daytime, String outdoorConditions) {
		String uri = uriBase + "rec.json";
		
		if(!daytime.equals("")){
			uri += "&daytimes=" + daytime;
		}
		if(!outdoorConditions.equals("")){
			uri += "&outdoorConditions=" + outdoorConditions;
		}
		if(!tags.equals("")){
			uri += "&tags=" + tags;
		}
		System.out.println("=== request content from database ===");
		Utils.printWithDate("sent query: " + uri, Utils.DEBUGLEVEL.GENERAL);

		main.myLogger.addDatabaseQuery(uri);

		String requestedData = "";

		try {
			Content response = Request.Get(uri).execute().returnContent();
			requestedData = response.toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return requestedData;
	}
	
	public String sendSurvey(String tag) {
		String uri = uriBase + "surveys.json";

		if(!tag.equals("")){
			uri += "&tags=" + tag;
		}
		
		System.out.println("=== request survey from database ===");
		Utils.printWithDate("sent query: " + uri, Utils.DEBUGLEVEL.GENERAL);
		
		main.myLogger.addDatabaseQuery(uri);

		String requestedData = "";

		try {
			Content response = Request.Get(uri).execute().returnContent();
			requestedData = response.toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return requestedData;
	} 
}
