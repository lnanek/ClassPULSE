package us.gpop.classpulse.device;

import android.app.Activity;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

public class ScreenWaker {
	private static final String LOG_TAG = ScreenWaker.class.getSimpleName();
	private WakeLock lock;
	private PowerManager power;
	final Activity activity;

	public ScreenWaker(final Activity activity) {
		this.activity = activity;
		
		power = (PowerManager) activity.getSystemService(Activity.POWER_SERVICE);
		lock = power.newWakeLock(
				PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
				LOG_TAG);
		onCreate(activity);
	}
	
	public void onCreate(final Activity activity) {
		acquireLockAndSimulateActivity();		
	}

	public void onResume() {
		acquireLockAndSimulateActivity();
	}

	public void onPause() {
		
		// Extend screen off to slightly longer, otherwise it turns off almost immediately
		acquireLockAndSimulateActivity();
		
		if (lock.isHeld()) {
			lock.release();
		}
		activity.getWindow().clearFlags(LayoutParams.FLAG_TURN_SCREEN_ON);
		activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	private void acquireLockAndSimulateActivity() {
		Window window = activity.getWindow();
		window.addFlags(LayoutParams.FLAG_TURN_SCREEN_ON);
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		if (!lock.isHeld()) {
			lock.acquire();
		}
		long l = SystemClock.uptimeMillis();
		// false will bring the screen back as bright as it was, true - will dim
		// it
		power.userActivity(l, false);
	}

}
