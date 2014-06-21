package us.gpop.classpulse;

import roboguice.activity.RoboActivity;
import android.os.Bundle;
import android.util.Log;

public class BaseActivity extends RoboActivity {
	private static final String LOG_TAG = BaseActivity.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(LOG_TAG, "onCreate " + getClass().getSimpleName() + " " + hashCode());
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(LOG_TAG, "onStart " + getClass().getSimpleName() + " " + hashCode());
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(LOG_TAG, "onResume " + getClass().getSimpleName() + " " + hashCode());
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(LOG_TAG, "onPause " + getClass().getSimpleName() + " " + hashCode());
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(LOG_TAG, "onStop " + getClass().getSimpleName() + " " + hashCode());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(LOG_TAG, "onDestroy " + getClass().getSimpleName() + " " + hashCode());
	}

	@Override
	public void finish() {
		Log.d(LOG_TAG, "finish " + getClass().getSimpleName() + " " + hashCode());
		super.finish();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		Log.d(LOG_TAG, "onWindowFocusChanged " + hasFocus + " " 
				+ getClass().getSimpleName() + " " + hashCode());
		super.onWindowFocusChanged(hasFocus);
	}		
}
