package us.gpop.classpulse.network;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import us.gpop.classpulse.sensors.LocationTracker;
import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ApiClient {

	public interface ApiClientListener {
		void onSendSuccess(Object result);

		void onSendFail();
	}

	private static final String LOG_TAG = ApiClient.class.getSimpleName();

	public static final String UPLOAD_URL = "http://gpop-server.com/classpulse/graph.php";

	public static final String CLASS_LIST_URL = "http://gpop-server.com/classpulse/list-classes.php";

	private final Gson gson = new Gson();
	
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

		final StudentStatus message = new StudentStatus();
		message.understandCount = understandCount;
		message.dontUnderstandCount = dontUnderstandCount;
		if (null != locationTracker && locationTracker.hasLocation()) {
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
				final HttpPost httpPost = new HttpPost(UPLOAD_URL);
				HttpResponse response = null;
				HttpEntity entity = null;
				try {
					final StringEntity params = new StringEntity(json);
					httpPost.setEntity(params);
					response = new DefaultHttpClient().execute(httpPost);
					
					final int statusCode = response.getStatusLine().getStatusCode();
					Log.d(LOG_TAG, "Response status: " + statusCode);
					if (HttpStatus.SC_OK != statusCode) {
						Log.d(LOG_TAG, "There was a problem uploading, response code: " + statusCode);
						deliverErrorOnUiThread();
						return;
					}
					
					entity = response.getEntity();
					
					final InputStream is = entity.getContent();
					final String returnedString = IOUtils.toString(is, "UTF-8");
					Log.d(LOG_TAG, "Server returned: " + returnedString);
					
					final int firstIndexOfCurlyBraceOpen = returnedString.indexOf('{');
					final String returnedJson;
					if ( -1 == firstIndexOfCurlyBraceOpen ) {
						returnedJson = returnedString;
					} else {
						returnedJson = returnedString.substring(firstIndexOfCurlyBraceOpen);
					}

					Log.d(LOG_TAG, "Parsing: " + returnedJson);
										
					// Deliver success with parsed response
					final Graph result = gson.fromJson (returnedJson, Graph.class );
					uiHandler.post(new Runnable() {
						@Override
						public void run() {
							isSending = false;
							listener.onSendSuccess(result);
						}						
					});		

				} catch (Exception e) {
					Log.d(LOG_TAG, "There was a problem uploading", e);
					deliverErrorOnUiThread();
				} finally {
					/*
					if ( null != entity ) {
						try {
							entity.consumeContent();
						} catch (IOException e) {
							Log.d(LOG_TAG, "Error closing connection", e);
						}
					}		
					*/			
					//httpPost.abort();
				}
			}
		});
		uploadThread.start();
	}
	

	public void getClassList() {
		final Thread getClassListThread = new Thread(new Runnable() {
			@Override
			public void run() {
				Log.d(LOG_TAG, "getClassList");
				
				HttpEntity entity = null;
				try {
					final HttpGet httpGet = new HttpGet(CLASS_LIST_URL);
					final HttpResponse response = new DefaultHttpClient().execute(httpGet);
					final int statusCode = response.getStatusLine().getStatusCode();
					if (HttpStatus.SC_OK != statusCode) {
						Log.d(LOG_TAG, "There was a problem retrieving, response code: " + statusCode);
						deliverErrorOnUiThread();
						return;
					}
					entity = response.getEntity();
					final InputStream is = entity.getContent();
					final String returnedString = IOUtils.toString(is, "UTF-8");
					Log.d(LOG_TAG, "Server returned: " + returnedString);
					Type type = new TypeToken<List<ClassStatus>>(){}.getType();
					final List<ClassStatus> result =  gson.fromJson(returnedString, type);     
					uiHandler.post(new Runnable() {
						@Override
						public void run() {
							isSending = false;
							listener.onSendSuccess(result);
						}						
					});		
				} catch (Exception e) {
					Log.d(LOG_TAG, "There was a problem retrieving: " + e.toString());
					deliverErrorOnUiThread();
				} finally {
					/*
					if ( null != entity ) {
						try {
							entity.consumeContent();
						} catch (IOException e) {
							Log.d(LOG_TAG, "Error closing connection", e);
						}
					}	
					*/				
					//httpPost.abort();
				}
			}
		});
		getClassListThread.start();
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
