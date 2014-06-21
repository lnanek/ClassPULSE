package us.gpop.classpulse.device;

import android.view.MotionEvent;

public interface Detector {
	
	public boolean onGenericMotionEvent(MotionEvent event);

}
