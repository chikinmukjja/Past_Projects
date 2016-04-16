package com.example.socketclient;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class activity_log_DB_list extends Activity{
	ListView mListView;
	LogDBcursorAdapter mAdapter;
	LogDBManager mDbManager;
	Cursor cursor;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log_db_list);	
	}
	
	protected void onStart()
	{
		super.onStart();
		/* 데이터 베이스의 커서 객체를 가져온다 */
		mDbManager = LogDBManager.getInstance(this);
		String[] columns = new String[]{"_id", "time", "image"};
		cursor = mDbManager.query(columns, null, null, null, null, null);
		/* 어댑터뷰 생성 */
		mAdapter = new LogDBcursorAdapter(this, cursor, 0);
		/* 리스트뷰와 어댑터뷰를 연결*/
		mListView = (ListView)findViewById(R.id.log_db_data_list_activity_listView);
		mListView.setAdapter(mAdapter);
		
		mListView.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				cursor.moveToPosition(position);
				
				String time 	= cursor.getString(cursor.getColumnIndex("time"));
				int    _id      = cursor.getInt(cursor.getColumnIndex("_id"));
				int    pos 		= position;
				
				Intent intent = new Intent(getApplicationContext(), activity_log_DB_list_elem.class);
				
				Bundle bundleData = new Bundle();
				bundleData.putInt("POSITION", pos);
				bundleData.putString("TIME" , time);
				bundleData.putInt("_ID", _id);
				
				intent.putExtra("Data", bundleData);
				startActivity(intent);
			}			
		});
		
	}   //onStart 함수 끝
}
