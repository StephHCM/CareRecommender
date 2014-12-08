package eu.care.communication;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import eu.care.context.UserMoodInterpreter;
import eu.care.main.DemonstratorMain;
import eu.care.main.Utils;
import eu.care.recommenderengine.Recommendation;

/**
 * 
 * @author Stephan Hammer, Andreas Seiderer
 *
 */
public class MyXMPPConnection {

	//config file
	private JSONObject jsonConfig;
	
	public XMPPConnection connection;
	private DemonstratorMain main;
	private MyJSONParser jsonParser;

	// Config-Data: hostXMPP: 192.168.0.101
	private static String hostXMPP, usernameXMPP, passwordXMPP, receiverXMPP, publishNode, screenXMPP, loggerXMPP, certificateName;
	private static int portXMPP;
	private boolean shutdown = false;
	private String displayDateOfLastUserResponse = "";

	public MyXMPPConnection(DemonstratorMain main) {
		this.main = main;
		jsonParser = new MyJSONParser(main);
		
		//load config-information
		try {
			jsonConfig = (JSONObject) main.jsonParser.myParser.parse((main.jsonParser.getJSONMessage(main.configFile)));
			hostXMPP = ((JSONObject)jsonConfig.get("XMPP")).get("server").toString();
			portXMPP = Integer.parseInt(((JSONObject)jsonConfig.get("XMPP")).get("port").toString());
			publishNode = ((JSONObject)jsonConfig.get("XMPP")).get("publishNode").toString();
			usernameXMPP = ((JSONObject)jsonConfig.get("XMPP")).get("user").toString();
			passwordXMPP = ((JSONObject)jsonConfig.get("XMPP")).get("pw").toString();
			certificateName = ((JSONObject)jsonConfig.get("XMPP")).get("certificate").toString();
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public void connect(boolean useXMPPConnection){
		if (useXMPPConnection) {

			Utils.printWithDate("XMPP Connection to: " + hostXMPP + " (Port " + portXMPP + ") ... ", Utils.DEBUGLEVEL.GENERAL);

			ConnectionConfiguration xmppConfig = new ConnectionConfiguration(hostXMPP, portXMPP);
			xmppConfig.setReconnectionAllowed(true);
			loadCertificate(xmppConfig);
			
			connection = new XMPPTCPConnection(xmppConfig);

			boolean retryConnection = true;
			
			while (retryConnection && !shutdown) {
				try {
					connection.connect();
					connection.login(usernameXMPP, passwordXMPP);
	
					Presence p = new Presence(Presence.Type.available);
					p.setStatus("Recommender available");
					connection.sendPacket(p);
	
					Utils.printWithDate("connected", Utils.DEBUGLEVEL.GENERAL);
					retryConnection = false;
	
				} catch (SmackException | IOException | XMPPException e) {
					e.printStackTrace();
				}
			}
			
			if (shutdown) return;
			
			initializePubSubConnection();
			
		} else {
			System.out.println("-------------NO CONNECTION TO XMPP SERVER-------------");
		}
	}

	public void loadCertificate(ConnectionConfiguration xmppConfig) {
		// Use the certificate file "server.crt" -> has to be the same like from the server
		InputStream is;
		try {
			is = new FileInputStream(certificateName);
			// You could get a resource as a stream instead.

			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate caCert = (X509Certificate) cf.generateCertificate(is);

			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
			ks.load(null); // You don't need the KeyStore instance to come from a file.
			ks.setCertificateEntry("caCert", caCert);

			tmf.init(ks);

			SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, tmf.getTrustManagers(), null);

			xmppConfig.setCustomSSLContext(sslContext);
		} catch (CertificateException | NoSuchAlgorithmException
				| KeyStoreException | KeyManagementException | IOException e) {
			e.printStackTrace();
		}
	}

	public void initializePubSubConnection() {
		// Create a pubsub manager using an existing XMPPConnection
		PubSubManager mgr = new PubSubManager(connection);
		try {
			LeafNode node = null;

			node = mgr.getNode(publishNode);
			node.addItemEventListener(new ItemEventCoordinator());		
			node.subscribe(usernameXMPP + "@" + hostXMPP);

		} catch (NoResponseException | NotConnectedException | XMPPException.XMPPErrorException e) {
			e.printStackTrace();
		}
	}

	class ItemEventCoordinator implements ItemEventListener {
		@Override
		public void handlePublishedItems(ItemPublishEvent items) {

			List<PayloadItem> payloads = items.getItems();
			for (PayloadItem item : payloads) {
				String jsonMessage = parseXML(item.toXML());
				
				Utils.printWithDate("TriggerMessage: " + jsonMessage, Utils.DEBUGLEVEL.GENERAL);
				
				if(!jsonMessage.equals("")){
					main.gatherContextInformation(jsonMessage);
				}
			} 
		}

	}
	
	public void initializeChatListener() {
		System.out.println("Initialize Chat Listener");
	ChatManager chatManager = ChatManager.getInstanceFor(connection);
	chatManager.addChatListener(new ChatManagerListener() {
		
		@Override
		public void chatCreated(Chat chat, boolean arg1) {
			chat.addMessageListener(new MessageListener() {

				@Override
				public void processMessage(Chat chat, Message message) {
					String careScreen = screenXMPP + "@" + hostXMPP;
					String sentFrom = chat.getParticipant().toLowerCase();
					
					Utils.printWithDate("Message received at: " + LocalDateTime.now(Clock.systemUTC()), Utils.DEBUGLEVEL.DEBUG);
					Utils.printWithDate("CHATLISTENER: Received message from " + sentFrom + ": " + message.getBody(), Utils.DEBUGLEVEL.GENERAL);

					if(sentFrom.contains(careScreen.toLowerCase())){
						interpretUserResponse(message.getBody());
					}
				}
			});
		}
		});
	}

	public String parseXML(String xmlString){
	
		String jsonMessage = "";
		
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
		    builder = builderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
		    e.printStackTrace();  
		}
		
		try {
			Document document;
			if (!xmlString.equals("")){
				System.out.println("-------------Received XML-------------");
				document = builder.parse(new InputSource(new ByteArrayInputStream(xmlString.getBytes("utf-8"))));
				jsonMessage = document.getElementsByTagName("trigger").item(0).getTextContent();
			}
		} catch (SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return jsonMessage;
	}

	// receiver: screenXMPP
	public void sendContentToScreen(Vector<JSONObject> contentToShow, String type) {

		System.out.println("=== send content ===");

		sendMessage("screenUser", jsonParser.encodeMessageToScreen(contentToShow, type));

		// main.myLogger.addJSONMessage(main.myLogger.sentRecObj, message);

		// receiver: loggerXMPP
		System.out.println("=== send/store log-message ===");
		sendMessage("loggerUser", main.myLogger.createLoggingMessage());
	}

	public void sendMessage(String receiver, String message) {

		receiverXMPP = ((JSONObject)jsonConfig.get("XMPP")).get(receiver).toString().toLowerCase();
		screenXMPP = ((JSONObject)jsonConfig.get("XMPP")).get("screenUser").toString().toLowerCase();
		loggerXMPP = ((JSONObject)jsonConfig.get("XMPP")).get("loggerUser").toString().toLowerCase();

		if (main.useXMPPConnection) {
			ChatManager chatmanager = ChatManager.getInstanceFor(connection);
			Chat newChat = chatmanager.createChat(receiverXMPP + "@" + hostXMPP, new MessageListener() {
					@Override
					public void processMessage(Chat chat, Message message) {			
						String careScreen = screenXMPP + "@" + hostXMPP;
						String sentFrom = chat.getParticipant().toLowerCase();
						
						Utils.printWithDate("Message received at: " + LocalDateTime.now(Clock.systemUTC()), Utils.DEBUGLEVEL.DEBUG);
						Utils.printWithDate("MSGLISTENER: Received message from " + sentFrom + ": " + message.getBody(), Utils.DEBUGLEVEL.GENERAL);
						if(sentFrom.contains(careScreen.toLowerCase())){
							interpretUserResponse(message.getBody());
						}
						}
					});
			try {
				newChat.sendMessage(message);
			} catch (NotConnectedException | XMPPException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		Utils.printWithDate("SENT (@" + receiver + ") -> " + message, Utils.DEBUGLEVEL.GENERAL);
		
		if(receiverXMPP.equals(loggerXMPP)){
			main.myLogger.clearLog();
		}
	}
	
	public void interpretUserResponse(String response){
		
		HashMap<String, String> userResponseInDetail = new HashMap<String, String>();
		userResponseInDetail = jsonParser.decodeUserResponse(response);

		Utils.printWithDate("Response at: " + userResponseInDetail.get("displayDate") + " type: " + userResponseInDetail.get("contentType"), Utils.DEBUGLEVEL.GENERAL);
		
		//Check whether last user response was already interpreted
		if(!displayDateOfLastUserResponse.equals(userResponseInDetail.get("displayDate"))){
			
			displayDateOfLastUserResponse = userResponseInDetail.get("displayDate");
			Utils.printWithDate("Interpreting user response...", Utils.DEBUGLEVEL.DEBUG);
			
			//user answered to the request for his/her current mood	
			if(userResponseInDetail.get("contentType").equals("survey")){
				String userAnswer = userResponseInDetail.get("userAnswer");
					Utils.printWithDate("userAnswer : " + userAnswer, Utils.DEBUGLEVEL.GENERAL);
				main.user.setMood(new UserMoodInterpreter().interpreterUserMood(userResponseInDetail.get("userAnswer")));
					Utils.printWithDate("userMood : " + main.user.getMood(), Utils.DEBUGLEVEL.DEBUG);	
				main.user.setLastUpdateMood(LocalDateTime.parse(userResponseInDetail.get("displayDate"), DateTimeFormatter.RFC_1123_DATE_TIME));
					Utils.printWithDate(" at " + main.user.getLastUpdateMood(), Utils.DEBUGLEVEL.DEBUG);
					
				main.contextInformation.put("userMood", main.user.getMood());
				main.reactToCurrentContext(main.contextInformation);
			}
			//recommendation was displayed + optional: was rated, quiz was answered
			else if(userResponseInDetail.get("contentType").equals("recommendation")){
				//get all needed information to store answer in user model: 
				//rec_id, rec_category, displayedAt, user_rating, quiz_result, quiz_answer already in HashMap "userResponseInDetail"
				
				//add context information: daytime, userMood, importantTags (stored with chosen recommendation)
				//get recommendation from main history
				Recommendation rec = main.recommendationsHistory.get(userResponseInDetail.get("contentID"));
				
				//get context information of recommendation
				HashMap<String, String> contextInformation = rec.getLastContext();
				userResponseInDetail.put("daytime", contextInformation.get("daytime"));
				userResponseInDetail.put("userMood", contextInformation.get("userMood"));
				userResponseInDetail.put("lightConditionOutdoors", contextInformation.get("lightConditionOutdoors"));
				userResponseInDetail.put("weatherCondition", contextInformation.get("weatherCondition"));
			
				String importantTags = "";
				
				if (contextInformation.containsKey("user_movement")) {
					importantTags += contextInformation.get("user_state") + ",";
				}

				if (contextInformation.containsKey("room_airquality")) {
					importantTags += contextInformation.get("room_airquality") + ",";
				}

				if(contextInformation.containsKey("userMood")){
					if (contextInformation.get("userMood").equals("badMood")) {
						importantTags += contextInformation.get("userMood") + ",";
					}
				}
				
				importantTags = main.cutOffLastCommaOfString(importantTags);
				
				userResponseInDetail.put("importantTags", importantTags);
				
				//insert into collection in mongoDB => user model
				if (DemonstratorMain.mongoDBConnection != null){
					String database = ((JSONObject)jsonConfig.get("MongoDB")).get("dbNameUserModel").toString();
					String collection = ((JSONObject)jsonConfig.get("MongoDB")).get("collectionNameUserModel").toString();
					DemonstratorMain.mongoDBConnection.insertDataDirectly(userResponseInDetail, database, collection);
				}
				else{
					Utils.printWithDate("User response was interpreted. No database entry.", Utils.DEBUGLEVEL.DEBUG);
					Iterator<String> keys = userResponseInDetail.keySet().iterator();
					while(keys.hasNext()){
						String key = keys.next();
						Utils.printWithDate("key: " + key + " value: " + userResponseInDetail.get(key), Utils.DEBUGLEVEL.DEBUG);
					}
				}
			}
		}
		else{
			Utils.printWithDate("User response already interpreted.", Utils.DEBUGLEVEL.DEBUG);
		}
	}
	
	public void closeConnection() {	
		shutdown = true;
		boolean wasConnected = connection.isConnected();
		
		try {
			connection.disconnect();
			if (wasConnected) Utils.printWithDate("XMPP Disconnected.", Utils.DEBUGLEVEL.WARNING);
			
			DemonstratorMain.latch_wait_shutdown.countDown();
		} catch (NotConnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
