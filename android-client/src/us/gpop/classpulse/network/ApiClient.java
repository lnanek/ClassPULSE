package us.gpop.classpulse.network;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import us.gpop.classpulse.sensors.LocationTracker;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.google.gson.Gson;

public class ApiClient {

	public interface ApiClientListener {
		void onSendSuccess(ClassStatus result);

		void onSendFail();
	}

	private static final String LOG_TAG = ApiClient.class.getSimpleName();

	public static final String UPLOAD_URL = "http://gpop-server.com/classpulse/post.php";

	private final Gson gson = new Gson();
	
	private final DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
	
	private Long lastSendUptimeMillis;
	
	private boolean isSending;
	
	private Handler uiHandler = new Handler();
	
	private ApiClientListener listener;

	public ApiClient(final ApiClientListener listener) {
		this.listener = listener;
	}
	
	public boolean isSending() {
		return isSending;
	}

	public void sendToServer(
			final int understandCount,
			final int dontUnderstandCount,
			final LocationTracker locationTracker,
			final String email,
			final String className) {
		
		isSending = true;
		lastSendUptimeMillis = SystemClock.uptimeMillis();

		final StudentStatus message = new StudentStatus();
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
					final HttpPost httpPost = new HttpPost(UPLOAD_URL);
					final StringEntity params = new StringEntity(json);
					httpPost.setEntity(params);
					final HttpResponse response = defaultHttpClient.execute(httpPost);
					
					final int statusCode = response.getStatusLine().getStatusCode();
					Log.d(LOG_TAG, "Response status: " + statusCode);
					if (HttpStatus.SC_OK != statusCode) {
						Log.d(LOG_TAG, "There was a problem uploading, response code: " + statusCode);
						deliverErrorOnUiThread();
						return;
					}
					
					final InputStream is = response.getEntity().getContent();
					final String returnedString = IOUtils.toString(is, "UTF-8");
					Log.d(LOG_TAG, "Server returned: " + returnedString);
					
					final int firstIndexOfCurlyBraceOpen = returnedString.indexOf('{');
					final String returnedJson = returnedString.substring(firstIndexOfCurlyBraceOpen);

					Log.d(LOG_TAG, "Parsing: " + returnedJson);
										
					// Deliver success with parsed response
					final ClassStatus result = gson.fromJson (returnedJson, ClassStatus.class );
					uiHandler.post(new Runnable() {
						@Override
						public void run() {
							isSending = false;
							listener.onSendSuccess(result);
						}						
					});		

				} catch (Exception e) {
					Log.d(LOG_TAG, "There was a problem uploading: " + e.toString());
					deliverErrorOnUiThread();
				}
			}
		});
		uploadThread.start();
	}
	
	private void deliverErrorOnUiThread() {
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				isSending = false;
				listener.onSendFail();
			}						
		});		
	}

}
