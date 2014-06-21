package us.gpop.classpulse;

import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.util.Log;

public class ApiClient {
	
	public interface ApiClientListener {
		void onSendSuccess();
		void onSendFail();
	}
	
	private static final String LOG_TAG = ApiClient.class.getSimpleName();

	public static final String UPLOAD_URL = "http://gpop-server.com/classpulse/post.php";
	
	public ApiClient() {
	}
	
	public void sendToServer(
			final int understandCount, 
			final int dontUnderstandCount, 
			final LocationTracker locationTracker,
			final String email,
			final String className,
			final ApiClientListener listener) {
		
		Log.d(LOG_TAG, "sendToServer");

		final Thread uploadThread = new Thread(new Runnable() {
			@Override
			public void run() {


				final ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("understandCount", 
						Integer.toString(understandCount)));
				nameValuePairs.add(new BasicNameValuePair("dontUnderstandCount", 
						Integer.toString(dontUnderstandCount)));
				nameValuePairs.add(new BasicNameValuePair("className", className));
				nameValuePairs.add(new BasicNameValuePair("email", email));
				if ( locationTracker.hasLocation() ) {
				nameValuePairs.add(new BasicNameValuePair("lat", 
						Double.toString(locationTracker.getLatitude())));
				nameValuePairs.add(new BasicNameValuePair("lon", 
						Double.toString(locationTracker.getLongitude())));
				}
				
				try {
					HttpPost httpPost = new HttpPost(UPLOAD_URL);
					httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
					DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
					HttpResponse response = defaultHttpClient.execute(httpPost);
					Log.d(LOG_TAG, "Response status: " + response.getStatusLine());
					
					listener.onSendSuccess();

				} catch (Exception e) {
					Log.d(LOG_TAG, "There was a problem uploading: " + e.toString());
					listener.onSendFail();
				}
			}
		});
		uploadThread.start();
	}

}
