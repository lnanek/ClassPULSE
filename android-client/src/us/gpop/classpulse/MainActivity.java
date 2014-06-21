package us.gpop.classpulse;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	private static class GlassSetup {
		private static Detector setup(final DetectorListener listener, Context context) {
			return new SwipeDetector(listener, context);
		}
	}
	
	private static final float NOD_TRIGGER_SUM = 25;

	private static final float SHAKE_TRIGGER_SUM = 25;

	private static final String LOG_TAG = MainActivity.class.getSimpleName();

	private LocationTracker location;
	
	private ScreenWaker screenWaker;
	
	private FilteredOrientationTracker tracker;
	
	private Detector swipes;
	
	private String email;
	
	private boolean resumed;
	
	private boolean setup;
	
	private TextView understandCountView;
	
	private TextView dontUnderstandCountView;
	
	private int understandCount;
	
	private int dontUnderstandCount;
	
	private DetectorListener detectorListener = new DetectorListener() {
		@Override
		public void onSwipeDownOrBack() {
			finish();
		}
		@Override
		public void onSwipeForwardOrVolumeUp() {
			understandCount++;
			updateUi();
		}

		@Override
		public void onSwipeBackOrVolumeDown() {
			dontUnderstandCount++;
			updateUi();
		}
		@Override
		public void onTap() {
		}
	};
		
	private FilteredOrientationTracker.Listener trackerListener = new FilteredOrientationTracker.Listener() {		
		@Override
		public void onUpdate(float[] gyro, float[] gyroSum) {
			Log.i(LOG_TAG, "xGyro = " + gyro[1] + " xGyroSum = " 
					+ gyroSum[1] + " yGyro = " + gyro[0] + " yGyroSum = " + gyroSum[0]);
			
			// Look left and right 
			if ( Math.abs(gyroSum[0]) > NOD_TRIGGER_SUM) {
				gyroSum[0] = 0;
				understandCount++;
				updateUi();
			}
			
			if ( Math.abs(gyroSum[1]) > SHAKE_TRIGGER_SUM) {
				gyroSum[1] = 0;
				dontUnderstandCount++;
				updateUi();
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(LOG_TAG, "onCreate");
		screenWaker = new ScreenWaker(this);
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);	
		understandCountView = (TextView) findViewById(R.id.understandCount);
		dontUnderstandCountView = (TextView) findViewById(R.id.dontUnderstandCount);
		
		email = DeviceEmail.get(this);
		Log.i(LOG_TAG, "user email = " + email);
				
		updateUi();
		
		if (Build.MODEL.toUpperCase().contains("GLASS")) {
				Log.d(LOG_TAG, "Glass detected, tracking side touch pad events...");
			swipes = GlassSetup.setup(detectorListener, this);
		} else {
				Log.d(LOG_TAG, "Not Glass: " + Build.MODEL);
		}
	}
		
	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {		
		swipes.onGenericMotionEvent(event);
		return super.onGenericMotionEvent(event);
	}

	private void updateUi() {
		understandCountView.setText("Understand: " + understandCount);
		dontUnderstandCountView.setText("Don't understand: " + dontUnderstandCount);
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
		}
	}
	
	
}
