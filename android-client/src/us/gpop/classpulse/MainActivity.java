package us.gpop.classpulse;

import java.util.ArrayList;

import us.gpop.classpulse.device.Detector;
import us.gpop.classpulse.device.DetectorListener;
import us.gpop.classpulse.device.DeviceEmail;
import us.gpop.classpulse.device.ScreenWaker;
import us.gpop.classpulse.device.SwipeDetector;
import us.gpop.classpulse.network.ApiClient;
import us.gpop.classpulse.network.ApiClient.ApiClientListener;
import us.gpop.classpulse.network.ClassStatus;
import us.gpop.classpulse.sensors.FilteredOrientationTracker;
import us.gpop.classpulse.sensors.LocationTracker;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	private static class GlassSetup {
		private static Detector setup(final DetectorListener listener, Context context) {
			return new SwipeDetector(listener, context);
		}
	}
	
	private static final int POLL_PERIOD_MS = 5 * 1000;
	
	private static final float NOD_TRIGGER_SUM = 25;

	private static final float SHAKE_TRIGGER_SUM = 25;

	private static final String LOG_TAG = MainActivity.class.getSimpleName();
	
	private Handler handler;

	private LocationTracker location;
	
	private ScreenWaker screenWaker;
	
	private FilteredOrientationTracker tracker;
	
	private Detector swipes;
	
	private String email;
	
	private boolean resumed;
	
	private boolean setup;
	
	private TextView understandCountView;
	
	private TextView dontUnderstandCountView;

	private TextView understandCountTotalView;
	
	private TextView dontUnderstandCountTotalView;
	
	private TextView userCountView;
	
	private TextView classTitle;
	
	private int understandCount;
	
	private int dontUnderstandCount;
	
	private View glassInstructions;
	
	private View androidButtons;

	private View understandButton;

	private View dontUnderstandButton;
	
	private String className = "ADV 320F";
		
	private ClassStatus classStatus;
	
	private Runnable pollServer = new Runnable() {
		@Override
		public void run() {
			Log.i(LOG_TAG, "pollServer#run");
			
			// Poll the server if we aren't currently sending something
			if (!client.isSending()) {
				Log.i(LOG_TAG, "polling server...");
				client.sendToServer(understandCount, dontUnderstandCount, location, email, className);
			} else {
				Log.i(LOG_TAG, "skipping poll...");				
			}
			
			// Run again later
			if ( null != handler ) {
				handler.postDelayed(this, POLL_PERIOD_MS);
			}
		}		
	};
	
	private DetectorListener detectorListener = new DetectorListener() {
		@Override
		public void onSwipeDownOrBack() {
			Log.i(LOG_TAG, "onSwipeDownOrBack");
			finish();
		}
		@Override
		public void onSwipeForwardOrVolumeUp() {
			Log.i(LOG_TAG, "onSwipeForwardOrVolumeUp");
			onUnderstand();
		}

		@Override
		public void onSwipeBackOrVolumeDown() {
			Log.i(LOG_TAG, "onSwipeBackOrVolumeDown");
			onDontUnderstand();
		}
		@Override
		public void onTap() {
			Log.i(LOG_TAG, "onTap");
		}
	};
	
	private ApiClientListener clientListener = new ApiClientListener() {

		@Override
		public void onSendSuccess(ClassStatus result) {
			Log.i(LOG_TAG, "onSendSuccess result = " + result);
			MainActivity.this.classStatus = result;
			updateUi();	
		}

		@Override
		public void onSendFail() {
			Log.i(LOG_TAG, "onSendFail");
		}		
	};
	
	private ApiClient client = new ApiClient(clientListener);
		
	private FilteredOrientationTracker.Listener trackerListener = new FilteredOrientationTracker.Listener() {		
		@Override
		public void onUpdate(float[] gyro, float[] gyroSum) {
			//Log.i(LOG_TAG, "xGyro = " + gyro[1] + " xGyroSum = " 
			//		+ gyroSum[1] + " yGyro = " + gyro[0] + " yGyroSum = " + gyroSum[0]);
			// Nod head up and down
			if ( Math.abs(gyroSum[0]) > NOD_TRIGGER_SUM) {
				gyroSum[0] = 0;
				onUnderstand();
			}
			// Shake head left and right 			
			if ( Math.abs(gyroSum[1]) > SHAKE_TRIGGER_SUM) {
				gyroSum[1] = 0;
				onDontUnderstand();
			}
		}
	};
	
	private void onUnderstand() {
		Log.i(LOG_TAG, "onUnderstand");
		
		understandCount++;
		client.sendToServer(understandCount, dontUnderstandCount, location, email, className);
		updateUi();		
	}
	
	private void onDontUnderstand() {
		Log.i(LOG_TAG, "onDontUnderstand");
		
		dontUnderstandCount++;
		client.sendToServer(understandCount, dontUnderstandCount, location, email, className);
		updateUi();		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(LOG_TAG, "onCreate");
		screenWaker = new ScreenWaker(this);
		
		super.onCreate(savedInstanceState);
		
		// Parse response to voice prompt, if any
		final Intent intent = getIntent();
		if ( null != intent ) {
			final Bundle extras = intent.getExtras();
			if ( null != extras ) {
				if (extras.containsKey(RecognizerIntent.EXTRA_RESULTS)) {
					final ArrayList<String> voiceResults = 
							extras.getStringArrayList(RecognizerIntent.EXTRA_RESULTS);
					if (!voiceResults.isEmpty()) {
						className = voiceResults.get(0);
					}
				}				
			}			
		}
		
		setContentView(R.layout.activity_main);	
		understandCountView = (TextView) findViewById(R.id.understandCount);
		dontUnderstandCountView = (TextView) findViewById(R.id.dontUnderstandCount);
		dontUnderstandCountTotalView = (TextView) findViewById(R.id.dontUnderstandCountTotal);
		understandCountTotalView = (TextView) findViewById(R.id.understandCountTotal);
		userCountView = (TextView) findViewById(R.id.userCount);
		glassInstructions = findViewById(R.id.glassInstructions);
		androidButtons = findViewById(R.id.androidButtons);
		understandButton = findViewById(R.id.understandButton);
		classTitle = (TextView) findViewById(R.id.classTitle);
		classTitle.setText(className);
		
		understandButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onUnderstand();
			}
		});
		dontUnderstandButton = findViewById(R.id.dontUnderstandButton);
		dontUnderstandButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onDontUnderstand();
			}
		});
		
		email = DeviceEmail.get(this);
		Log.i(LOG_TAG, "user email = " + email);
				
		updateUi();
		
		if (Build.MODEL.toUpperCase().contains("GLASS")) {
				Log.d(LOG_TAG, "Glass detected, tracking side touch pad events...");
			swipes = GlassSetup.setup(detectorListener, this);
			androidButtons.setVisibility(View.GONE);
		} else {
				Log.d(LOG_TAG, "Not Glass: " + Build.MODEL);
				glassInstructions.setVisibility(View.GONE);
		}
	}
		
	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {	
		Log.i(LOG_TAG, "onGenericMotionEvent");
		
		swipes.onGenericMotionEvent(event);
		return super.onGenericMotionEvent(event);
	}

	private void updateUi() {
		Log.i(LOG_TAG, "updateUi");
		
		understandCountView.setText("Understand: " + understandCount);
		dontUnderstandCountView.setText("Don't understand: " + dontUnderstandCount);
		if (null != classStatus) {
			understandCountTotalView.setText("Total understand: " + classStatus.understandTotal);
			dontUnderstandCountTotalView.setText("Total don't: " + classStatus.dontUnderstandTotal);
			userCountView.setText(Integer.toString(classStatus.studentsTotal));
		}
	}

	@Override
	protected void onResume() {
		Log.i(LOG_TAG, "onResume hasFocus = " + hasWindowFocus());
		
		super.onResume();
		resumed = true;
		setupOrCleanup();
		screenWaker.onResume();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		Log.i(LOG_TAG, "onWindowFocusChanged hasFocus = " + hasFocus);
		
		super.onWindowFocusChanged(hasFocus);
		setupOrCleanup();
	}

	@Override
	protected void onPause() {
		Log.i(LOG_TAG, "onPause hasFocus = " + hasWindowFocus());
		
		super.onPause();		
		resumed = false;
		setupOrCleanup();
		screenWaker.onPause();
	}
	
	/**
	 * Glass has a nasty habit of resuming, pausing, resuming, then giving window focus.
	 * So delay setup until we are both resumed and have window focus, otherwise cleanup.
	 **/
	private void setupOrCleanup() {
		Log.i(LOG_TAG, "setupOrCleanup");
		
		// Cleanup
		if ( !resumed && !hasWindowFocus() && setup ) {
			Log.i(LOG_TAG, "cleaning up...");
			setup = false;
			if ( null != location ) {
				location.stopListeningForLocations();
				location = null;
			}
			
			if ( null != tracker ) {
				tracker.onPause();
				tracker = null;
			}
			
			if ( null != handler ) {
				handler.removeCallbacksAndMessages(null);
				handler = null;
			}
			
			return;
		}
		
		// Startup
		if (resumed && hasWindowFocus() && !setup) {
			Log.i(LOG_TAG, "setup...");
			setup = true;
			if ( null == location ) {
				location = new LocationTracker(this);
				location.startAccquiringLocationData();
			}
			
			if ( null == tracker ) {
				tracker = new FilteredOrientationTracker(this, trackerListener);
				tracker.onResume();
			}
			
			if ( null == handler ) {
				handler = new Handler();
				handler.postDelayed(pollServer, POLL_PERIOD_MS);
			}
		}
	}
		
}
