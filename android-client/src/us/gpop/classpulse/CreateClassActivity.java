package us.gpop.classpulse;

import us.gpop.classpulse.device.DeviceEmail;
import us.gpop.classpulse.network.ApiClient;
import us.gpop.classpulse.network.ApiClient.ApiClientListener;
import us.gpop.classpulse.sensors.LocationTracker;
import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class CreateClassActivity extends Activity {

	private Context context;

	@Override
	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_class);
		context = this;
		final EditText newClassName = (EditText) findViewById(R.id.new_class_name_edittext);
		TextView createClassButton = (TextView) findViewById(R.id.create_class_button);
		createClassButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String newClassNameText = newClassName.getText().toString();
				if (newClassNameText.length() > 0) {
					client.sendToServer(0, 0, new LocationTracker(context),
							DeviceEmail.get(context), newClassNameText);
				} else {
					Toast.makeText(getApplicationContext(),
							"Please enter a class name.", Toast.LENGTH_LONG)
							.show();
				}
			}
		});
	}

	private ApiClientListener clientListener = new ApiClientListener() {

		@Override
		public void onSendSuccess(final Object resultObject) {
			Toast.makeText(getApplicationContext(),
					"Your class was successfully created.", Toast.LENGTH_LONG)
					.show();
			finish();
		}

		@Override
		public void onSendFail() {
			Toast.makeText(getApplicationContext(),
					"There was a problem creating your class.",
					Toast.LENGTH_LONG).show();
			finish();
		}
	};

	private ApiClient client = new ApiClient(clientListener);

}
