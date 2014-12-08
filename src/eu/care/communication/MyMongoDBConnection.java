package eu.care.communication;


import java.util.HashMap;

import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoTimeoutException;
import com.mongodb.ServerAddress;

import eu.care.main.DemonstratorMain;
import eu.care.main.Utils;

import org.json.simple.JSONObject;

/**
 * 
 * @author Andreas Seiderer
 *
 */
public class MyMongoDBConnection {
	
	private MongoClient mongoClient;
	private boolean shutdown = false;
	
	private String hostname;
	private int port;
	
	private boolean isConnected = false;
	
	
	public MyMongoDBConnection (JSONObject jsonConfig) {
		this((String)jsonConfig.get("server"), ((Long)jsonConfig.get("port")).intValue());
	}
	
	/**
	 * 
	 * @param hostname name of the host
	 * @param port default port is usually 27017
	 */
	public MyMongoDBConnection(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}
	
	/**
	 * 
	 */
	public void connect() {
		MongoClientOptions.Builder builder = new MongoClientOptions.Builder();
		//builder.socketTimeout(0);
		builder.socketKeepAlive(true);
		
		boolean notConnected = true;
		
		while (notConnected && !shutdown) {

			try {
				Utils.printWithDate("Connect to MongoDB at " + hostname + " (Port " + port + ") ... ", Utils.DEBUGLEVEL.GENERAL);
				mongoClient = new MongoClient( new ServerAddress(hostname, port), builder.build() );
				
				//cause exceptions if database is not open
				mongoClient.getDatabaseNames();
				
				notConnected = false;			
				
			} catch (Exception e) {
				if (shutdown) return;
				Utils.printWithDate("MongoDB: could not connect to " + hostname + "; retrying ...", Utils.DEBUGLEVEL.WARNING);
			}
		}
		
		if (shutdown) return;
		
		Utils.printWithDate("connected", Utils.DEBUGLEVEL.GENERAL);
		isConnected = true;
	}
	
	/**
	 * 
	 * @param data
	 * @param dbName
	 * @param collection
	 */
	public void insertDataDirectly(HashMap<String, String> data, String dbName, String collection) {
		if (mongoClient != null) {
			DB db = mongoClient.getDB( dbName );
			DBCollection coll = db.getCollection(collection);
			BasicDBObject dbObj	= createDBObject(data);
			try {
				Utils.printWithDate("Inserting answer in DB(" + dbName + ")...", Utils.DEBUGLEVEL.DEBUG);
				coll.insert(dbObj);
			} catch (MongoTimeoutException e) {
				Utils.printWithDate("MongoDB not connected. Try to reconnect...", Utils.DEBUGLEVEL.WARNING);
				connect();
			}	
		}
	}
	
	private BasicDBObject createDBObject(HashMap<String, String> data){
		BasicDBObject newDBObject = new BasicDBObject();
			
			for(String key : data.keySet()){
				newDBObject.append(key, data.get(key));
			}
			
		return newDBObject;
	}
	
	/**
	 * 
	 * @param dbName
	 * @return
	 */
	public void getDBObjectsByFieldValue(String dbName, String collection, String field, String value) {
		DB db = mongoClient.getDB( dbName );
		DBCollection coll = db.getCollection(collection);
		
		BasicDBObject query = new BasicDBObject(field, value);
		DBCursor cursor = coll.find(query);

		try {
		   while(cursor.hasNext()) {
			   Utils.printWithDate("DBObject: " + cursor.next(), Utils.DEBUGLEVEL.GENERAL);
		   }
		} finally {
		   cursor.close();
		}
	}
	
	public String getCurrentSunsetSunriseTimes(String dbName) { 
		DB db = mongoClient.getDB( dbName ); 
		CommandResult res = db.doEval("function() { return SunCalc_getTimesToday(); }"); 
		
		if (!res.ok()){
			System.err.println(res.getErrorMessage());
		}
		else if (res.get("retval") != null){
			return res.get("retval").toString(); 
		}
		return null; 
	}
	
	public String getLastWeatherConditionWithRating(String dbName) { 
		DB db = mongoClient.getDB( dbName ); 
		CommandResult res = db.doEval("function() { return getLastWeatherConditionWithRating(); }"); 
		Utils.printWithDate("CommandResult (WeatherCondition): " + res.toString(), Utils.DEBUGLEVEL.DEBUG);
		
		if (!res.ok()){
			System.err.println(res.getErrorMessage());
		}
		else if (res.get("retval") != null){
			return res.get("retval").toString(); 
		}
		return null; 
	}
	
	/**
	 * 
	 */
	public void closeConnection() {
		shutdown = true;
		
		if (mongoClient != null) {
			mongoClient.close();
			
			if (isConnected) Utils.printWithDate("MongoDB Disconnected.", Utils.DEBUGLEVEL.WARNING);
			DemonstratorMain.latch_wait_shutdown.countDown();
		}
		
		isConnected = false;
		
	}

}
