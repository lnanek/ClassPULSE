package us.gpop.classpulse;

public interface DetectorListener {
	void onSwipeDownOrBack();

	void onSwipeForwardOrVolumeUp();

	void onSwipeBackOrVolumeDown();

	void onTap();
}