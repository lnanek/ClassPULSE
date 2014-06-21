package us.gpop.classpulse;

import java.util.ArrayList;

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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.SensorManager;
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
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.glass.media.Sounds;
import com.google.inject.Inject;

public class GraphActivity extends BaseActivity {

	private static class GlassSetup {
		private static Detector setup(final DetectorListener listener, Context context) {
			return new SwipeDetector(listener, context);
		}
	}

	private static enum HeadMotion {
		LEFT, RIGHT, UP, DOWN;
	}

	private static final int DOING_GOOD_BAR_COLOR = Color.parseColor("#000000");

	private static final int DOING_BAD_BAR_COLOR = Color.parseColor("#851a00");

	// How long after triggering an understand or don't until you can trigger
	// again.
	private static final int TRIGGER_BREAK_MS = 5 * 1000;

	// How long to wait before checking the server for the latest totals
	private static final int POLL_PERIOD_MS = 5 * 1000;

	private static final float NOD_TRIGGER_SUM = 8;

	private static final float SHAKE_TRIGGER_SUM = 8;

	private static final String LOG_TAG = GraphActivity.class.getSimpleName();

	private Handler handler = new Handler();

	private LocationTracker location;

	private ScreenWaker screenWaker;

	private FilteredOrientationTracker orientation;

	private Detector swipes;

	private String email;

	private HeadMotion lastVerticalHeadMotion;
	
	private HeadMotion lastHorizontalHeadMotion;

	private boolean recentlyTriggered;

	@InjectView(R.id.root)
	private View root;

	@InjectView(R.id.minusOnePleaseWait)
	private View minusOnePleaseWait;

	@InjectView(R.id.plusOnePleaseWait)
	private View plusOnePleaseWait;

	@InjectView(R.id.infoBar)
	private View infoBar;

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

	@InjectView(R.id.shakeInstruction)
	private TextView shakeInstruction;
	
	@InjectView(R.id.nodInstruction)
	private TextView nodInstruction;

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

	private boolean isShowingDebugReadings;

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
			toggleDisplayingDebugReading();
		}
	};

	private ApiClientListener clientListener = new ApiClientListener() {

		@Override
		public void onSendSuccess(final Object resultObject) {
			final Graph result = (Graph) resultObject;
			Log.i(LOG_TAG, "onSendSuccess result = " + result);
			GraphActivity.this.graph = result;
			// Don't force the screen on after the first time we see the data
			root.setKeepScreenOn(false);
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
			 //+ gyroSum[1] + " yGyro = " + gyro[0] + " yGyroSum = " +
			 //gyroSum[0]);
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
				gyroSum[0] = 0;
			} else if (Math.abs(gyroSum[0]) > NOD_TRIGGER_SUM && gyroSum[0] > 0) {
				Log.i(LOG_TAG, "head down");
				newHeadMotion = HeadMotion.DOWN;
				gyroSum[0] = 0;

				// Shake head left and right
			} else if (Math.abs(gyroSum[1]) > SHAKE_TRIGGER_SUM && gyroSum[1] < 0) {
				Log.i(LOG_TAG, "head left");
				newHeadMotion = HeadMotion.LEFT;
				gyroSum[1] = 0;
			} else if (Math.abs(gyroSum[1]) > SHAKE_TRIGGER_SUM && gyroSum[1] > 0) {
				Log.i(LOG_TAG, "head right");
				newHeadMotion = HeadMotion.RIGHT;
				gyroSum[1] = 0;
			}

			// If we did, clear the movement sum
			if (newHeadMotion == null) {
				return;
			}

			Log.i(LOG_TAG, "lastVerticalHeadMotion = "
					+ lastVerticalHeadMotion 
					+ "lastHorizontalHeadMotion = "
							+ lastHorizontalHeadMotion 
					+ ", new = " + newHeadMotion);

			// If we just did the opposite head motion, trigger a nod or shake
			if (HeadMotion.LEFT == lastHorizontalHeadMotion &&
					HeadMotion.RIGHT == newHeadMotion) {
				Log.i(LOG_TAG, "left right");
				lastHorizontalHeadMotion = null;
				onDontUnderstand();
				return;
			} else if (HeadMotion.RIGHT == lastHorizontalHeadMotion &&
					HeadMotion.LEFT == newHeadMotion) {
				Log.i(LOG_TAG, "right left");
				lastHorizontalHeadMotion = null;
				onDontUnderstand();
				return;
				
			} else if (HeadMotion.UP == lastVerticalHeadMotion &&
					HeadMotion.DOWN == newHeadMotion) {
				Log.i(LOG_TAG, "up down");
				lastVerticalHeadMotion = null;
				onUnderstand();
				return;
			} else if (HeadMotion.DOWN == lastVerticalHeadMotion &&
					HeadMotion.UP == newHeadMotion) {
				Log.i(LOG_TAG, "down left");
				lastVerticalHeadMotion = null;
				onUnderstand();
				return;
			}

			// Log.i(LOG_TAG, "saving motion to check against next");
			if (HeadMotion.LEFT == newHeadMotion || HeadMotion.RIGHT == newHeadMotion) {
				lastHorizontalHeadMotion = newHeadMotion;
			} else {
				lastVerticalHeadMotion = newHeadMotion;				
			}
		}
	};

	private Runnable resetTriggered = new Runnable() {
		@Override
		public void run() {
			recentlyTriggered = false;
			
			shakeInstruction.setAlpha(1f);
			nodInstruction.setAlpha(1f);
			
			enableButtons();
			minusOnePleaseWait.setVisibility(View.GONE);
			plusOnePleaseWait.setVisibility(View.GONE);
			if (null != orientation) {
				orientation.onResume();
			}
		}
	};

	private void toggleDisplayingDebugReading() {
		Log.i(LOG_TAG, "toggleDisplayingDebugReading");

		audioManager.playSoundEffect(Sounds.TAP);

		if (!isShowingDebugReadings) {
			debugReadings.setAlpha(0.5f);
			isShowingDebugReadings = true;
		} else {
			debugReadings.setAlpha(0f);
			isShowingDebugReadings = false;
		}
	}

	private void onUnderstand() {
		Log.i(LOG_TAG, "onUnderstand");
		if (recentlyTriggered) {
			audioManager.playSoundEffect(Sounds.ERROR);
			return;
		}

		shakeInstruction.setAlpha(0f);
		nodInstruction.setAlpha(0f);

		// Wake up screen
		screenWaker.onResume();
		// But let screen turn off, immersion will keep our activity up
		// the user can look up to go back to as needed
		screenWaker.onPause();

		recentlyTriggered = true;
		if ( null != handler ) {
			handler.postDelayed(resetTriggered, TRIGGER_BREAK_MS);
		}
		plusOnePleaseWait.setVisibility(View.VISIBLE);
		disableButtons();

		AlphaAnimation animation = new AlphaAnimation(1.0f, 0.0f);
		animation.setDuration(TRIGGER_BREAK_MS);
		if (null != orientation) {
			orientation.onPause();
		}
		plusOnePleaseWait.startAnimation(animation);

		audioManager.playSoundEffect(Sounds.SUCCESS);

		understandCount++;
		client.sendToServer(understandCount, dontUnderstandCount, location, email, className);

		// OLD CODE. MANUAL UPDATE OF GRAPH FROM LOCAL USER.
		// ackLineGraph.refreshGraph(this, true); // A "YAY/I UNDERSTAND"
		// response.

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

		shakeInstruction.setAlpha(0f);
		nodInstruction.setAlpha(0f);
		
		// Wake up screen
		screenWaker.onResume();
		// But let screen turn off, immersion will keep our activity up
		// the user can look up to go back to as needed
		screenWaker.onPause();

		recentlyTriggered = true;
		if ( null != handler ) {
			handler.postDelayed(resetTriggered, TRIGGER_BREAK_MS);
		}

		if (null != orientation) {
			orientation.onPause();
		}

		disableButtons();

		minusOnePleaseWait.setVisibility(View.VISIBLE);

		AlphaAnimation animation = new AlphaAnimation(1.0f, 0.0f);
		animation.setDuration(TRIGGER_BREAK_MS);
		minusOnePleaseWait.startAnimation(animation);

		audioManager.playSoundEffect(Sounds.ERROR);

		dontUnderstandCount++;
		client.sendToServer(understandCount, dontUnderstandCount, location, email, className);

		// OLD CODE. SHOWS DATA FROM LOCAL USER.
		// ackLineGraph.refreshGraph(this, false); // A "NAY/DON'T UNDERSTAND"
		// response.

		updateUi();
	}
	
	final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				orientation.onPause();
				orientation.onResume();
			}
		}
	};

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
			intentExtras = intent.getExtras();
			if (null != intentExtras) {
				if (intentExtras.containsKey(RecognizerIntent.EXTRA_RESULTS)) {
					final ArrayList<String> voiceResults =
							intentExtras.getStringArrayList(RecognizerIntent.EXTRA_RESULTS);
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

		if (Build.MODEL.toUpperCase().contains("GLASS")) {
			orientation = new FilteredOrientationTracker(this, trackerListener);
			orientation.onResume();

			// Try to keep using sensors after screen off via re-registering
			final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
			//registerReceiver(receiver,  filter);
		}

		updateUi();

		if (Build.MODEL.toUpperCase().contains("GLASS")) {
			Log.d(LOG_TAG, "Glass detected, tracking side touch pad events...");
			swipes = GlassSetup.setup(detectorListener, this);
			androidButtons.setVisibility(View.GONE);
			titleBar.setVisibility(View.GONE);
		} else {
			Log.d(LOG_TAG, "Not Glass: " + Build.MODEL);
			glassInstructions.setVisibility(View.GONE);
			infoBar.setBackgroundColor(Color.parseColor("#007e7a"));
		}

		// Initialize the graph.
		ackLineGraph.setUpGraph(this);
		LinearLayout layout = (LinearLayout) findViewById(R.id.graph_container);
		layout.addView(ackLineGraph.graphView);

		if (intentExtras != null
				&& intentExtras.containsKey("className")
				&& intentExtras.containsKey("totalStudents")) {
			classTitle.setText(intentExtras.getString("className"));
			userCountView.setText(Integer.toString(intentExtras.getInt("totalStudents")));
		}
		
		if (null == location) {
			location = new LocationTracker(this);
			location.startAccquiringLocationData();
		}

		handler.post(pollServer);
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		Log.i(LOG_TAG, "onGenericMotionEvent");
		if (null != swipes) {
			swipes.onGenericMotionEvent(event);
		}
		return super.onGenericMotionEvent(event);
	}

	private void updateUi() {
		Log.i(LOG_TAG, "updateUi");

		understandCountView.setText("Understand: " + understandCount);
		dontUnderstandCountView.setText("Don't understand: " + dontUnderstandCount);

		if (null == graph || null == graph.graph || graph.graph.isEmpty()) {
			Log.i(LOG_TAG, "updateUi - no graph data");
			return;
		}
		final ClassStatus classStatus = graph.graph.get(graph.graph.size() - 1);
		if (null != classStatus) {
			understandCountTotalView.setText("Total understand: " + classStatus.totalUnderstand);
			dontUnderstandCountTotalView.setText("Total don't: " + classStatus.totalDontUnderstand);
			userCountView.setText(Integer.toString(classStatus.totalStudents));

			if (classStatus.totalDontUnderstand > classStatus.totalUnderstand) {
				final int difference = classStatus.totalDontUnderstand - classStatus.totalUnderstand;

				Log.i(LOG_TAG, "updateUi doing bad - " + difference);

				// Full visible bad color if over 20 don't understand more than
				// understand.
				if (difference >= 20) {
					Log.i(LOG_TAG, "updateUi - doing full bad");
					glassInstructions.setBackgroundColor(DOING_BAD_BAR_COLOR);
				} else {
					// As approach 20, more and more visibile
					final float ratio = difference / 20f;
					Log.i(LOG_TAG, "updateUi - doing bad ratio - " + ratio);
					final int alpha = (int) (255 * ratio);
					Log.i(LOG_TAG, "updateUi - doing bad alpha - " + alpha);
					final int color = Color.argb(
							alpha,
							Color.red(DOING_BAD_BAR_COLOR),
							Color.green(DOING_BAD_BAR_COLOR),
							Color.blue(DOING_BAD_BAR_COLOR));
					glassInstructions.setBackgroundColor(color);
				}
			} else {
				Log.i(LOG_TAG, "updateUi - doing good");
				glassInstructions.setBackgroundColor(DOING_GOOD_BAR_COLOR);
			}
		}

		// GRAPH UPDATE
		ackLineGraph.refreshLiveGraph(this, classStatus.totalUnderstand, classStatus.totalDontUnderstand); // Refresh
																											// the
																											// graph.
	}

	@Override
	protected void onResume() {
		Log.i(LOG_TAG, "onResume hasFocus = " + hasWindowFocus());

		super.onResume();

		// Wake up screen
		screenWaker.onResume();
		// But let screen turn off, immersion will keep our activity up
		// the user can look up to go back to as needed
		screenWaker.onPause();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		Log.i(LOG_TAG, "onWindowFocusChanged hasFocus = " + hasFocus);

		super.onWindowFocusChanged(hasFocus);
	}

	@Override
	protected void onPause() {
		Log.i(LOG_TAG, "onPause hasFocus = " + hasWindowFocus());
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		if (null != orientation) {
			orientation.onPause();
			orientation = null;
		}
		if (null != location) {
			location.stopListeningForLocations();
			location = null;
		}
		if (null != handler) {
			handler.removeCallbacksAndMessages(null);
			handler = null;
		}				
		super.onDestroy();
	}
}
