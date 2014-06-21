package us.gpop.classpulse.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import us.gpop.classpulse.sensors.LocationTracker;
import android.util.Log;

import com.google.gson.Gson;

public class ApiClient {

	public interface ApiClientListener {
		void onSendSuccess(ServerResponse result);

		void onSendFail();
	}

	private static final String LOG_TAG = ApiClient.class.getSimpleName();

	public static final String UPLOAD_URL = "http://gpop-server.com/classpulse/post.php";

	final Gson gson = new Gson();

	public ApiClient() {
	}

	public void sendToServer(
			final int understandCount,
			final int dontUnderstandCount,
			final LocationTracker locationTracker,
			final String email,
			final String className,
			final ApiClientListener listener) {

		final ApiMessage message = new ApiMessage();
		message.understandCount = understandCount;
		message.dontUnderstandCount = dontUnderstandCount;
		if (locationTracker.hasLocation()) {
			message.lat = locationTracker.getLatitude();
			message.lon = locationTracker.getLongitude();
		}
		message.email = email;
		message.className = className;

		final String json = gson.toJson(message);

		Log.d(LOG_TAG, "sendToServer json = " + json);

		final Thread uploadThread = new Thread(new Runnable() {
			@Override
			public void run() {

				try {
					HttpPost httpPost = new HttpPost(UPLOAD_URL);

					StringEntity params = new StringEntity(json);
					httpPost.setEntity(params);

					DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
					HttpResponse response = defaultHttpClient.execute(httpPost);
					Log.d(LOG_TAG, "Response status: " + response.getStatusLine());
					
					ServerResponse result = gson.fromJson ( 
							new InputStreamReader(response.getEntity().getContent()), 
							ServerResponse.class );
					
					/*
					BufferedReader br = new BufferedReader(
							new InputStreamReader((response.getEntity().getContent())));

					String output;
					Log.d(LOG_TAG, "Output from Server .... \n");
					while ((output = br.readLine()) != null) {
						Log.d(LOG_TAG, output);
					}
					 */
					
					listener.onSendSuccess(result);

				} catch (Exception e) {
					Log.d(LOG_TAG, "There was a problem uploading: " + e.toString());
					listener.onSendFail();
				}
			}
		});
		uploadThread.start();
	}

}
