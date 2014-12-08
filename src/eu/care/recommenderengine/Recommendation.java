package eu.care.recommenderengine;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Vector;

import org.json.simple.JSONObject;

/**
 * 
 * @author Stephan Hammer
 *
 */
public class Recommendation {	

	private JSONObject jsonRepresentation;
	private String id;
	private String title;
	private Vector<String> categories;
	private Vector<String> tags;
	
	private LocalDateTime lastTimeDisplayed;
	private HashMap<String, String> lastContext;
	
	public void setJsonRepresentation(JSONObject jsonString){
		this.jsonRepresentation = jsonString;
	}
	
	public JSONObject getJsonRepresentation(){
		return this.jsonRepresentation;
	}
	
	public void setID(String id){
		this.id = id;
	}
	
	public String getID(){
		return this.id;
	}
	
	public void setTitle(String title){
		this.title = title;
	}
	
	public String getTitle(){
		return this.title;
	}
	
	public void setTags(Vector<String> tagsVector){
		tags = new Vector<String>();
		for(String tag : tagsVector){
			tags.add(tag);
		}
	}
	
	public Vector<String> getTags(){
		return this.tags;
	}
	
	public void setCategories(Vector<String> categoriesVector){
		categories = new Vector<String>();
		for(String category : categoriesVector){
			categories.add(category);
		}
	}
	
	public Vector<String> getCategories(){
		return this.categories;
	}
	
	public void setLastTimeDisplayed(LocalDateTime currentDateTime){
		this.lastTimeDisplayed = currentDateTime;
	}
	
	public LocalDateTime getLastTimeDisplayed(){
		return this.lastTimeDisplayed;
	}
	
	public void setLastContext(HashMap<String, String> currentContext){
		this.lastContext = currentContext;
	}
	
	public HashMap<String, String> getLastContext(){
		return this.lastContext;
	}
	
}
