package us.gpop.classpulse;

import java.util.ArrayList;
import java.util.List;

import us.gpop.classpulse.adapters.ClassAdapter;
import us.gpop.classpulse.network.ApiClient;
import us.gpop.classpulse.network.ApiClient.ApiClientListener;
import us.gpop.classpulse.network.ClassStatus;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class MainActivity extends BaseActivity {

	public static final String LOG_TAG = MainActivity.class.getSimpleName();
	private ClassAdapter adapter;
	private ListView listView;

	@Override
	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isGlass()) {
			startActivity(new Intent(this, ClassCardScrollActivity.class));
			finish();
			return;
		}
		setContentView(R.layout.activity_main);
		listView = (ListView) findViewById(R.id.class_list);
		

		client.getClassList();	
	};
	
	@Override
	protected void onResume() {
		super.onResume();
			
	}

	private ApiClientListener classListListener = new ApiClientListener() {

		@SuppressWarnings("unchecked")
		@Override
		public void onSendSuccess(Object result) {
			List<ClassStatus> classModelList = (List<ClassStatus>) result;
			Log.i(LOG_TAG, "onSendSuccess = " + classModelList);

			findViewById(R.id.progress).setVisibility(View.GONE);
			
			final ArrayList<ClassStatus> array = new ArrayList<ClassStatus>();
			array.addAll(classModelList);
			array.add(new ClassStatus());
			
			adapter = new ClassAdapter(MainActivity.this, array);
			
			//for (int i = 0; i < classModelList.size(); i++) {
				//Log.i(LOG_TAG, "adapter.addAll <= " + classModelList);
				//adapter.addAll(classModelList);
			//}
			adapter.notifyDataSetChanged();
			listView.setAdapter(adapter);
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
