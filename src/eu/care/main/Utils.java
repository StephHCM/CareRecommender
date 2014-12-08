package eu.care.main;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Utils {
	
	public static int systemDebugLevel = 0;

	public static enum DEBUGLEVEL {WARNING, GENERAL, DEBUG}
	
	public static void printWithDate(String message, DEBUGLEVEL debuglevel) {

		  if (debuglevel.ordinal() <= systemDebugLevel ) {		
			  SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.SSS'Z'");
			  sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			  
			  System.out.println(sdf.format(new Date())+ "\t"+debuglevel.toString()+"\t"+message);
		  }
	}
	
}
