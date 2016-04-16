package com.example.socketclient;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;


public class LogDataBaseAdapter extends BaseAdapter{
	
	Context 		   mContext        = null;
	ArrayList<LogData> mData  		   = null;
	LayoutInflater     mLayoutInflater = null;
	Handler 		   mHandler 	   = null;
	int     		   cellSize;    
	static final int   numColumns      = 2;
	
	public LogDataBaseAdapter(Context context, ArrayList<LogData> data)
	{
		mContext 	    = context;
		mData   	    = data;
		mLayoutInflater = LayoutInflater.from(context);
		mHandler        = new Handler(Looper.getMainLooper());
		Point winSize   = new Point();
		((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealSize(winSize);
		cellSize 	    = winSize.x / numColumns;
	}

	@Override
	public int getCount() {
		return mData.size();
	}

	@Override
	public Object getItem(int position) {
		return mData.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View itemLayout = convertView;
		
		if(itemLayout == null)
		{
			itemLayout = mLayoutInflater.inflate(R.layout.log_list_view_item_layout, null);
			itemLayout.setLayoutParams(new AbsListView.LayoutParams(cellSize, cellSize));
			itemLayout.setPadding(2, 2, 2, 2);
		}
		
		ImageView dataTextView = (ImageView)itemLayout.findViewById(R.id.log_list_view_item_layout_imageView);
		
		dataTextView.setImageBitmap(mData.get(position).getImage());
		
		return itemLayout;
	}
	
	public void add(int index, LogData addData)
	{
		mData.add(index, addData);
		mHandler.postAtFrontOfQueue(new Runnable() {
			public void run() {
				notifyDataSetChanged();
			}
		});
	}
	
	public void delete(int index)
	{
		mData.remove(index);
		mHandler.postAtFrontOfQueue(new Runnable() {
			public void run() {
				notifyDataSetChanged();
			}
		});
	}
	
	public void clear()
	{
		mData.clear();
		notifyDataSetChanged();
	}
	
}
