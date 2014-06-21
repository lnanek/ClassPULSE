package us.gpop.classpulse;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {
	
	private static final String LOG_TAG = MainActivity.class.getSimpleName();

	private LocationTracker location;
	
	private String email;
	
	private boolean resumed;
	
	private boolean setup;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(LOG_TAG, "onCreate");
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);		
		
		email = DeviceEmail.get(this);
		Log.i(LOG_TAG, "user email = " + email);
	}

	@Override
	protected void onResume() {
		Log.i(LOG_TAG, "onResume hasFocus = " + hasWindowFocus());
		
		super.onResume();
		resumed = true;
		setupOrCleanup();
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
		}
	}
	
	
}
