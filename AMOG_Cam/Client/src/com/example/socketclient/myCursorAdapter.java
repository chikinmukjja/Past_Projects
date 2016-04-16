package com.example.socketclient;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class myCursorAdapter extends CursorAdapter{
	Context mContext			   = null;
	LayoutInflater mLayoutInflater = null;

	public myCursorAdapter(Context context, Cursor c,int flags) {
		super(context, c, flags);
		
		mContext = context;
		mLayoutInflater = LayoutInflater.from(context);
	}
	
	class ViewHolder
	{
		TextView  mNameTv;
		TextView  mIPAddressTv;
		ImageView mRecordingImageView;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		/* 아이템 뷰에 저장된 뷰홀더를 얻어온다. */
		ViewHolder viewHolder = (ViewHolder)view.getTag();
		
		/* 현재 커서 위치의 이름, IP 주소 데이터를 얻어온다 */
		String name     = cursor.getString(cursor.getColumnIndex("name"));
		String ipAdress = cursor.getString(cursor.getColumnIndex("ipAddress"));
		int    recording= cursor.getInt	  (cursor.getColumnIndex("recording"));
		
		/* 이름, IP 데이터를 뷰에 적용한다. */
		viewHolder.mNameTv.setText(name);
		viewHolder.mIPAddressTv.setText(ipAdress);
		
		if(recording == 1)
			viewHolder.mRecordingImageView.setImageResource(R.drawable.recording);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		/* 새로운 아이템 뷰 생성 */
		View itemLayout = mLayoutInflater.inflate(R.layout.ip_address_list_view_item_layout, null);
		
		/* 아이템에 뷰 홀더를 설정한다. */
		ViewHolder viewHolder = new ViewHolder();
		viewHolder.mNameTv    		   = (TextView) itemLayout.findViewById(R.id.list_view_item_layout_name);
		viewHolder.mIPAddressTv 	   = (TextView) itemLayout.findViewById(R.id.list_view_item_layout_ipAddress);		
		viewHolder.mRecordingImageView = (ImageView)itemLayout.findViewById(R.id.list_view_item_layout_recording_image);
		
		itemLayout.setTag(viewHolder);
		
		return itemLayout;
	}

}
