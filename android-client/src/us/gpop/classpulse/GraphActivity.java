package us.gpop.classpulse;

import java.util.ArrayList;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;
import us.gpop.classpulse.device.Detector;
import us.gpop.classpulse.device.DetectorListener;
import us.gpop.classpulse.device.DeviceEmail;
import us.gpop.classpulse.device.ScreenWaker;
import us.gpop.classpulse.device.SwipeDetector;
import us.gpop.classpulse.graph.AckGraph;
import us.gpop.classpulse.network.ApiClient;
import us.gpop.classpulse.network.ApiClient.ApiClientListener;
import us.gpop.classpulse.network.ClassStatus;
import us.gpop.classpulse.network.Graph;
import us.gpop.classpulse.sensors.FilteredOrientationTracker;
import us.gpop.classpulse.sensors.LocationTracker;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.glass.media.Sounds;
import com.google.inject.Inject;

public class GraphActivity extends RoboActivity {

	private static class GlassSetup {
		private static Detector setup(final DetectorListener listener, Context context) {
			return new SwipeDetector(listener, context);
		}
	}

	private static enum HeadMotion {
		LEFT, RIGHT, UP, DOWN;
	}

	// How long after triggering an understand or don't until you can trigger
	// again.
	private static final int TRIGGER_BREAK_MS = 5 * 1000;

	// How long to wait before checking the server for the latest totals
	private static final int POLL_PERIOD_MS = 5 * 1000;

	private static final float NOD_TRIGGER_SUM = 20;

	private static final float SHAKE_TRIGGER_SUM = 20;

	private static final String LOG_TAG = GraphActivity.class.getSimpleName();

	private Handler handler;

	private LocationTracker location;

	private ScreenWaker screenWaker;

	private FilteredOrientationTracker tracker;

	private Detector swipes;

	private String email;

	private HeadMotion lastHeadMotion;

	private boolean resumed;

	private boolean setup;

	private boolean recentlyTriggered;

	@InjectView(R.id.understandCount)
	private TextView understandCountView;

	@InjectView(R.id.dontUnderstandCount)
	private TextView dontUnderstandCountView;

	@InjectView(R.id.understandCountTotal)
	private TextView understandCountTotalView;

	@InjectView(R.id.dontUnderstandCountTotal)
	private TextView dontUnderstandCountTotalView;

	@InjectView(R.id.userCount)
	private TextView userCountView;

	@InjectView(R.id.glassStatus)
	private TextView glassStatus;

	@InjectView(R.id.titleBar)
	private View titleBar;
	
	@InjectView(R.id.debugReadings)
	private View debugReadings;

	@InjectView(R.id.classTitle)
	private TextView classTitle;

	private int understandCount;

	private int dontUnderstandCount;

	@InjectView(R.id.glassInstructions)
	private View glassInstructions;

	@InjectView(R.id.androidButtons)
	private View androidButtons;

	@InjectView(R.id.understandButton)
	private TextView understandButton;

	@InjectView(R.id.dontUnderstandButton)
	private TextView dontUnderstandButton;

	private String className = "ADV 320F";

	private Graph graph;
	
	private AckGraph ackLineGraph = new AckGraph();

	private Bundle intentExtras;
	
	@Inject
	private AudioManager audioManager;

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
			if (null != handler) {
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
			debugReadings.setAlpha(0.5f);
		}
	};

	private ApiClientListener clientListener = new ApiClientListener() {

		@Override
		public void onSendSuccess(final Object resultObject) {
			final Graph result = (Graph) resultObject;
			Log.i(LOG_TAG, "onSendSuccess result = " + result);			
			GraphActivity.this.graph = result;;
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
			// Log.i(LOG_TAG, "xGyro = " + gyro[1] + " xGyroSum = "
			// + gyroSum[1] + " yGyro = " + gyro[0] + " yGyroSum = " +
			// gyroSum[0]);
			if (recentlyTriggered) {
				gyroSum[0] = 0;
				gyroSum[1] = 0;
				return;
			}

			// Check if we have a new head motion
			HeadMotion newHeadMotion = null;
			// Nod head up and down
			if (Math.abs(gyroSum[0]) > NOD_TRIGGER_SUM && gyroSum[0] < 0) {
				Log.i(LOG_TAG, "head up");
				newHeadMotion = HeadMotion.UP;
			} else if (Math.abs(gyroSum[0]) > NOD_TRIGGER_SUM && gyroSum[0] > 0) {
				Log.i(LOG_TAG, "head down");
				newHeadMotion = HeadMotion.DOWN;

				// Shake head left and right
			} else if (Math.abs(gyroSum[1]) > SHAKE_TRIGGER_SUM && gyroSum[1] < 0) {
				Log.i(LOG_TAG, "head left");
				newHeadMotion = HeadMotion.LEFT;
			} else if (Math.abs(gyroSum[1]) > SHAKE_TRIGGER_SUM && gyroSum[1] > 0) {
				Log.i(LOG_TAG, "head right");
				newHeadMotion = HeadMotion.RIGHT;
			}

			// If we did, clear the movement sum
			if (newHeadMotion == null) {
				return;
			}
			gyroSum[0] = 0;
			gyroSum[1] = 0;

			Log.i(LOG_TAG, "last = " + lastHeadMotion + ", new = " + newHeadMotion);

			// If we just did the opposite head motion, trigger a nod or shake
			if (HeadMotion.LEFT == lastHeadMotion &&
					HeadMotion.RIGHT == newHeadMotion) {
				Log.i(LOG_TAG, "left right");
				lastHeadMotion = null;
				onDontUnderstand();
				return;
			} else if (HeadMotion.RIGHT == lastHeadMotion &&
					HeadMotion.LEFT == newHeadMotion) {
				Log.i(LOG_TAG, "right left");
				lastHeadMotion = null;
				onDontUnderstand();
				return;
			} else if (HeadMotion.UP == lastHeadMotion &&
					HeadMotion.DOWN == newHeadMotion) {
				Log.i(LOG_TAG, "up down");
				lastHeadMotion = null;
				onUnderstand();
				return;
			} else if (HeadMotion.DOWN == lastHeadMotion &&
					HeadMotion.UP == newHeadMotion) {
				Log.i(LOG_TAG, "down left");
				lastHeadMotion = null;
				onUnderstand();
				return;
			}

			// Log.i(LOG_TAG, "saving motion to check against next");
			lastHeadMotion = newHeadMotion;
		}
	};

	private Runnable resetTriggered = new Runnable() {
		@Override
		public void run() {
			recentlyTriggered = false;
			glassStatus.setText(R.string.glass_instructions);
			enableButtons();
		}
	};

	private void onUnderstand() {
		Log.i(LOG_TAG, "onUnderstand");			
		if (recentlyTriggered) {
			audioManager.playSoundEffect(Sounds.ERROR);
			return;
		}

		recentlyTriggered = true;
		handler.postDelayed(resetTriggered, TRIGGER_BREAK_MS);
		glassStatus.setText("Sent understood!");
		disableButtons();

		audioManager.playSoundEffect(Sounds.SUCCESS);
		
		understandCount++;
		client.sendToServer(understandCount, dontUnderstandCount, location, email, className);
		ackLineGraph.refreshGraph(this, true); // A "YAY/I UNDERSTAND" response.
		updateUi();
	}

	private void enableButtons() {
		if (!Build.MODEL.toUpperCase().contains("GLASS")) {
			androidButtons.setAlpha(1f);
			understandButton.setClickable(true);
			understandButton.setEnabled(true);
			dontUnderstandButton.setClickable(true);
			dontUnderstandButton.setEnabled(true);
		}
	}

	private void disableButtons() {
		if (!Build.MODEL.toUpperCase().contains("GLASS")) {
			androidButtons.setAlpha(0.2f);
			understandButton.setClickable(false);
			understandButton.setEnabled(false);
			dontUnderstandButton.setClickable(false);
			dontUnderstandButton.setEnabled(false);
		}
	}

	private void onDontUnderstand() {
		Log.i(LOG_TAG, "onDontUnderstand");	
		if (recentlyTriggered) {
			audioManager.playSoundEffect(Sounds.DISALLOWED);
			return;
		}

		recentlyTriggered = true;
		handler.postDelayed(resetTriggered, TRIGGER_BREAK_MS);
		disableButtons();
		glassStatus.setText("Sent don't understand!");

		audioManager.playSoundEffect(Sounds.ERROR);
		
		dontUnderstandCount++;
		client.sendToServer(understandCount, dontUnderstandCount, location, email, className);
		ackLineGraph.refreshGraph(this, false); // A "NAY/DON'T UNDERSTAND" response.
		updateUi();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(LOG_TAG, "onCreate");
		screenWaker = new ScreenWaker(this);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		super.onCreate(savedInstanceState);

		// Parse response to voice prompt, if any
		final Intent intent = getIntent();
		if (null != intent) {
			final Bundle extras = intent.getExtras();
			if (null != extras) {
				if (extras.containsKey(RecognizerIntent.EXTRA_RESULTS)) {
					final ArrayList<String> voiceResults =
							extras.getStringArrayList(RecognizerIntent.EXTRA_RESULTS);
					if (!voiceResults.isEmpty()) {
						className = voiceResults.get(0);
					}
				}
			}
		}

		setContentView(R.layout.activity_graph);
		classTitle.setText(className);

		understandButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onUnderstand();
			}
		});
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
			titleBar.setVisibility(View.GONE);
		} else {
			Log.d(LOG_TAG, "Not Glass: " + Build.MODEL);
			glassInstructions.setVisibility(View.GONE);
		}
		
        // Initialize the graph.
        ackLineGraph.setUpGraph(this);
        LinearLayout layout = (LinearLayout) findViewById(R.id.graph_container);
        layout.addView(ackLineGraph.graphView);
        

		if(intentExtras != null) {
			classTitle.setText(intentExtras.getString("className"));
			userCountView.setText(intentExtras.getString("totalStudents"));
			understandCountTotalView.setText("Total understand: " + intentExtras.getString("totalUnderstand"));
			dontUnderstandCountTotalView.setText("Total don't: " + intentExtras.getString("totalDontUnderstand"));
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
		
		if ( null == graph || null == graph.graph || graph.graph.isEmpty() ) {
			return;
		}
		final ClassStatus classStatus = graph.graph.get(graph.graph.size() - 1);
		if (null != classStatus) {
			understandCountTotalView.setText("Total understand: " + classStatus.totalUnderstand);
			dontUnderstandCountTotalView.setText("Total don't: " + classStatus.totalDontUnderstand);
			userCountView.setText(Integer.toString(classStatus.totalStudents));
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
	 * Glass has a nasty habit of resuming, pausing, resuming, then giving
	 * window focus. So delay setup until we are both resumed and have window
	 * focus, otherwise cleanup.
	 **/
	private void setupOrCleanup() {
		Log.i(LOG_TAG, "setupOrCleanup");

		// Cleanup
		if (!resumed && !hasWindowFocus() && setup) {
			Log.i(LOG_TAG, "cleaning up...");
			setup = false;
			if (null != location) {
				location.stopListeningForLocations();
				location = null;
			}

			if (null != tracker) {
				tracker.onPause();
				tracker = null;
			}

			if (null != handler) {
				handler.removeCallbacksAndMessages(null);
				handler = null;
			}

			return;
		}

		// Startup
		if (resumed && hasWindowFocus() && !setup) {
			Log.i(LOG_TAG, "setup...");
			setup = true;
			if (null == location) {
				location = new LocationTracker(this);
				location.startAccquiringLocationData();
			}

			if (Build.MODEL.toUpperCase().contains("GLASS")) {
				if (null == tracker) {
					tracker = new FilteredOrientationTracker(this, trackerListener);
					tracker.onResume();
				}
			}

			if (null == handler) {
				handler = new Handler();
				handler.postDelayed(pollServer, POLL_PERIOD_MS);
				glassStatus.setText(R.string.glass_instructions);
				recentlyTriggered = false;
			}
		}
	}

}