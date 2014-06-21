package us.gpop.classpulse.network;

import com.google.gson.Gson;

public class ServerResponse {
	
	private static transient final Gson gson = new Gson();

	public int dontUnderstandTotal;
	
	public int understandTotal;
	
	public int studentsTotal;
	
	public String className;

	@Override
	public String toString() {
		return gson.toJson(this);
	}
			
}
