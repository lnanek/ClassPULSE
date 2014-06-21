package us.gpop.classpulse.device;

public interface DetectorListener {
	void onSwipeDownOrBack();

	void onSwipeForwardOrVolumeUp();

	void onSwipeBackOrVolumeDown();

	void onTap();
}