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

	public ScreenWaker(final Activity activity) {
		power = (PowerManager) activity.getSystemService(Activity.POWER_SERVICE);
		lock = power.newWakeLock(
				PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
				LOG_TAG);
		onCreate(activity);
	}
	
	public void onCreate(final Activity activity) {
		Window window = activity.getWindow();
		window.addFlags(LayoutParams.FLAG_TURN_SCREEN_ON);
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		acquireLockAndSimulateActivity();		
	}

	public void onResume() {
		acquireLockAndSimulateActivity();
	}

	public void onPause() {
		if (lock.isHeld()) {
			lock.release();
		}
	}

	private void acquireLockAndSimulateActivity() {
		if (!lock.isHeld()) {
			lock.acquire();
		}
		long l = SystemClock.uptimeMillis();
		// false will bring the screen back as bright as it was, true - will dim
		// it
		power.userActivity(l, false);
	}

}
