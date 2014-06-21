package us.gpop.classpulse;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

public class SwipeDetector implements Detector {

	private static final String TAG = SwipeDetector.class.getSimpleName();

	private DetectorListener mListener;

	private GestureDetector mGestureDetector;
	
	public SwipeDetector(final DetectorListener listener, final Context context) {
		mListener = listener;
		mGestureDetector = createGestureDetector(context);
	}
	
	public boolean onGenericMotionEvent(MotionEvent event) {
		Log.d(TAG, "onGenericMotionEvent ev = " + event);
		if (mGestureDetector != null) {
			return mGestureDetector.onMotionEvent(event);
		}
		return false;
	}
	
	private GestureDetector createGestureDetector(Context context) {
		GestureDetector gestureDetector = new GestureDetector(context);
		// Create a base listener for generic gestures
		gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
			@Override
			public boolean onGesture(Gesture gesture) {
				if (gesture == Gesture.TAP) {
					Log.d(TAG, "gesture == Gesture.TAP");
					// do something on tap
					return true;
				} else if (gesture == Gesture.TWO_TAP) {
					Log.d(TAG, "gesture == Gesture.TWO_TAP");
					// do something on two finger tap
					return true;
				} else if (gesture == Gesture.SWIPE_RIGHT) {
					Log.d(TAG, "gesture == Gesture.SWIPE_RIGHT");
					// do something on right (forward) swipe
					mListener.onSwipeForwardOrVolumeUp();
					return true;
				} else if (gesture == Gesture.SWIPE_LEFT) {
					Log.d(TAG, "gesture == Gesture.SWIPE_LEFT");
					// do something on left (backwards) swipe
					mListener.onSwipeBackOrVolumeDown();
					return true;
				}
				return false;
			}
		});
		gestureDetector.setFingerListener(new GestureDetector.FingerListener() {
			@Override
			public void onFingerCountChanged(int previousCount, int currentCount) {
				Log.d(TAG, "onFingerCountChanged");
				// do something on finger count changes
			}
		});
		gestureDetector.setScrollListener(new GestureDetector.ScrollListener() {
			@Override
			public boolean onScroll(float displacement, float delta, float velocity) {
				Log.d(TAG, "onScroll");
				// do something on scrolling
				return false;
			}
		});
		return gestureDetector;
	}

}
