package us.gpop.classpulse.adapters;

import java.util.ArrayList;

import us.gpop.classpulse.GraphActivity;
import us.gpop.classpulse.R;
import us.gpop.classpulse.network.ClassModel;
import us.gpop.classpulse.network.ClassStatus;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ClassAdapter extends ArrayAdapter<ClassStatus> {

	private Context context;
	private ArrayList<ClassStatus> classList;

	private static class ViewHolder {
		TextView className;
		ImageView classIcon;
	}

	public ClassAdapter(Context context, ArrayList<ClassStatus> classList) {
		super(context, R.layout.class_row, classList);
		this.context = context;
		this.classList = classList;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ClassStatus classObj = getItem(position);
		final ViewHolder viewHolder;
		if (convertView == null) {
			viewHolder = new ViewHolder();
			LayoutInflater inflater = LayoutInflater.from(getContext());
			convertView = inflater.inflate(R.layout.class_row, parent, false);
			viewHolder.className = (TextView) convertView
					.findViewById(R.id.class_name);
			viewHolder.classIcon = (ImageView) convertView
					.findViewById(R.id.class_icon);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		if (position == classList.size() - 1) {
			viewHolder.className.setText("Create New");
			viewHolder.classIcon.setBackgroundResource(R.drawable.icon_plus);

			convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO: implement screen to add a new class
					Toast.makeText(getContext(), "Coming soon!",
							Toast.LENGTH_SHORT).show();
				}
			});
		} else {
			viewHolder.className.setText(classObj.className);
			convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					ClassStatus classObj = classList.get(position);
					Intent intent = new Intent(context, GraphActivity.class);
					intent.putExtra("className", classObj.className);
					intent.putExtra("totalStudents", classObj.totalStudents);
					intent.putExtra("totalUnderstand", classObj.totalUnderstand);
					intent.putExtra("totalDontUnderstand",
							classObj.totalDontUnderstand);
					context.startActivity(intent);
				}
			});
		}

		return convertView;
	}
}