package com.example.socketclient;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

public class activity_log_DB_list_elem extends Activity{
	TextView  mTimeTV;
	ImageView mImageView;
	Bitmap image;
	String time;
	int    _id;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log_db_list_elem);	
		
		mTimeTV    = (TextView)findViewById(R.id.show_log_db_activity_textView);
		mImageView = (ImageView)findViewById(R.id.show_log_db_activity_imageView);
		
		Intent intent = getIntent();
		Bundle bundleData = intent.getBundleExtra("Data");
		
		if(bundleData == null)
			return;
		
		int position     = bundleData.getInt("POSITION");
		time             = bundleData.getString("TIME");
		_id              = bundleData.getInt("_ID");
		
		image = LogDBManager.getInstance(this).getImageByPosition(position);
		
		mTimeTV.setText(time);
		if(image != null)
			mImageView.setImageBitmap(image);
	}
	
	protected void onDestroy()
	{
		super.onDestroy();
		image.recycle();
	}
	/* 액션바 설정 */
	public boolean onCreateOptionsMenu(Menu menu) {
	    getMenuInflater().inflate(R.menu.activity_show_log_db_list_elem_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
	    switch(item.getItemId())
	    {
	    	/* delete 버튼을 눌렀을 때 */     
	    	case R.id.activity_show_log_db_list_elem_delete_btn:
	    	{
	    		String whereClause = "_id='"+_id+"'";
				LogDBManager mDbManager = LogDBManager.getInstance(getApplicationContext());
				mDbManager.delete(whereClause, null);
				finish();
	    		break;
	    	}
	    	
	    	default:
	    		return false;	
	    }
	    return true;
	}	
}
