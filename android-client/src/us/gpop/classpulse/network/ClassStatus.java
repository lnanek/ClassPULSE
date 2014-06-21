package us.gpop.classpulse.network;

import com.google.gson.Gson;

/**
 * The class status the server returns with the entire class aggregate numbers.
 *
 */
public class ClassStatus {
	
	private static transient final Gson gson = new Gson();

	public String id;
	
	public String time;
	
	public int totalUnderstand;
	
	public int totalDontUnderstand;
	
	public int totalStudents;
	
	@Override
	public String toString() {
		return gson.toJson(this);
	}
			
}
