package us.gpop.classpulse;

import java.util.ArrayList;
import java.util.List;

import us.gpop.classpulse.network.ApiClient;
import us.gpop.classpulse.network.ClassStatus;
import us.gpop.classpulse.network.ApiClient.ApiClientListener;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.google.android.glass.app.Card;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

public class ClassCardScrollActivity extends Activity implements
		AdapterView.OnItemClickListener {

	public static final String LOG_TAG = ClassCardScrollActivity.class
			.getSimpleName();
	private List<Card> mCards;
	private CardScrollView mCardScrollView;
	private ClassCardScrollAdapter adapter;
	private List<ClassStatus> classList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		client.getClassList();
		mCardScrollView = new CardScrollView(this);
		adapter = new ClassCardScrollAdapter();
		setContentView(R.layout.card_loader);
	}

	private class ClassCardScrollAdapter extends CardScrollAdapter {

		@Override
		public int getPosition(Object item) {
			return mCards.indexOf(item);
		}

		@Override
		public int getCount() {
			return mCards.size();
		}

		@Override
		public Object getItem(int position) {
			return mCards.get(position);
		}

		@Override
		public int getViewTypeCount() {
			return Card.getViewTypeCount();
		}

		@Override
		public int getItemViewType(int position) {
			return mCards.get(position).getItemViewType();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return mCards.get(position).getView(convertView, parent);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		ClassStatus classObj = classList.get(position);
		Intent intent = new Intent(this, GraphActivity.class);
		intent.putExtra("className", classObj.className);
		intent.putExtra("totalStudents", classObj.totalStudents);
		intent.putExtra("totalUnderstand", classObj.totalUnderstand);
		intent.putExtra("totalDontUnderstand", classObj.totalDontUnderstand);
		startActivity(intent);
	}

	private ApiClientListener classListListener = new ApiClientListener() {

		@SuppressWarnings("unchecked")
		@Override
		public void onSendSuccess(Object result) {
			mCards = new ArrayList<Card>();
			Card card;
			classList = (List<ClassStatus>) result;
			for (int i = 0; i < classList.size(); i++) {
				ClassStatus classObj = classList.get(i);
				card = new Card(getApplicationContext());
				card.setText(classObj.className);
				card.setFootnote("Tap to Join");
				mCards.add(card);
			}
			setContentView(mCardScrollView);
			mCardScrollView.setAdapter(adapter);
			mCardScrollView
					.setOnItemClickListener(ClassCardScrollActivity.this);
			mCardScrollView.activate();
		}

		@Override
		public void onSendFail() {
			Log.i(LOG_TAG, "onSendFail");
		}
	};

	private ApiClient client = new ApiClient(classListListener);
}
