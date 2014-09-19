package no.nordicsemi.android.log.example;

import java.util.Calendar;

import no.nordicsemi.android.log.LogContract.Log.Level;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class LogAdapter extends CursorAdapter {
	private static final SparseIntArray mColors = new SparseIntArray();

	static {
		mColors.put(Level.DEBUG, 0xFF009CDE);
		mColors.put(Level.VERBOSE, 0xFFB8B056);
		mColors.put(Level.INFO, Color.BLACK);
		mColors.put(Level.APPLICATION, 0xFF238C0F);
		mColors.put(Level.WARNING, 0xFFD77926);
		mColors.put(Level.ERROR, Color.RED);
	}

	public LogAdapter(Context context) {
		super(context, null, 0);
	}

	@Override
	public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
		final View view = LayoutInflater.from(context).inflate(R.layout.log_item, parent, false);

		final ViewHolder holder = new ViewHolder();
		holder.time = (TextView) view.findViewById(R.id.time);
		holder.data = (TextView) view.findViewById(R.id.data);
		view.setTag(holder);
		return view;
	}

	@Override
	public void bindView(final View view, final Context context, final Cursor cursor) {
		final ViewHolder holder = (ViewHolder) view.getTag();
		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(cursor.getLong(1 /* TIME */));
		holder.time.setText(context.getString(R.string.log, calendar));

		final int level = cursor.getInt(2 /* LEVEL */);
		holder.data.setText(cursor.getString(3 /* DATA */));
		holder.data.setTextColor(mColors.get(level));
	}

	@Override
	public boolean isEnabled(int position) {
		return false;
	}

	private class ViewHolder {
		private TextView time;
		private TextView data;
	}

}
