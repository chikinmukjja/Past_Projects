package com.example.socketclient;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class LogDBcursorAdapter extends CursorAdapter{
	Context mContext				= null;	
	LayoutInflater mLayOutInflater  = null;
	
	public LogDBcursorAdapter(Context context, Cursor c, int flags)
	{
		super(context, c, flags);
		
		mContext = context;
		mLayOutInflater = LayoutInflater.from(context);
	}
	
	class ViewHolder
	{
		TextView mTimeTV;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder viewHolder = (ViewHolder) view.getTag();
		
		String time = cursor.getString(cursor.getColumnIndex("time"));
		
		viewHolder.mTimeTV.setText(time);
	}

	@Override
	public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {
		View itemLayout = mLayOutInflater.inflate(R.layout.log_db_list_view_item_layout, null);
		
		ViewHolder viewHolder = new ViewHolder();
		viewHolder.mTimeTV    = (TextView)itemLayout.findViewById(R.id.log_db_data_activity_textView);
		
		itemLayout.setTag(viewHolder);
		
		return itemLayout;
	}
	
}
