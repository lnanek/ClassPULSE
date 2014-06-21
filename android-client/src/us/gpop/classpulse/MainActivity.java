package us.gpop.classpulse;

import java.util.ArrayList;
import java.util.List;

import us.gpop.classpulse.adapters.ClassAdapter;
import us.gpop.classpulse.network.ApiClient;
import us.gpop.classpulse.network.ApiClient.ApiClientListener;
import us.gpop.classpulse.network.ClassModel;
import us.gpop.classpulse.network.ClassStatus;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.ListView;

public class MainActivity extends Activity {

	public static final String LOG_TAG = MainActivity.class.getSimpleName();
	private ClassAdapter adapter;

	@Override
	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isGlass()) {
			startActivity(new Intent(this, GraphActivity.class));
			return;
		}
		setContentView(R.layout.activity_main);
		client.getClassList();
		adapter = new ClassAdapter(this, new ArrayList<ClassStatus>());
		ListView listView = (ListView) findViewById(R.id.class_list);
		listView.setAdapter(adapter);
	};

	private ApiClientListener classListListener = new ApiClientListener() {

		@SuppressWarnings("unchecked")
		@Override
		public void onSendSuccess(Object result) {
			List<ClassStatus> classModelList = (List<ClassStatus>) result;
			for (int i = 0; i < classModelList.size(); i++) {
				adapter.addAll(classModelList);
			}
		}

		@Override
		public void onSendFail() {
			Log.i(LOG_TAG, "onSendFail");
		}
	};

	private ApiClient client = new ApiClient(classListListener);

	public static boolean isGlass() {
		if(Build.MODEL.toUpperCase().contains("GLASS")) {
			return true;
		}
		return false;
	}
}
