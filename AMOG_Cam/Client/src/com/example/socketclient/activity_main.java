
package com.example.socketclient;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;

public class activity_main extends Activity {

	EditText addressET;
	EditText portET;
	public IPAddressDBManager mDbManager = null;
	ListView        mListView            = null;
	myCursorAdapter mAdapter             = null;
	Cursor          data                 = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startActivity(new Intent(this,activity_loading.class));
		setContentView(R.layout.activity_main);
	}
	protected void onResume()
	{
		super.onResume();
		mDbManager = IPAddressDBManager.getInstance(this);
		String[] columns = new String[]{"_id","name","ipAddress","recording"};
		data = mDbManager.query(columns, null, null, null, null, null);
		/* 어댑터를 생성하고 데이터를 설정 */
		mAdapter = new myCursorAdapter(this, data, 0);
		if(data == null)
			return;
		/* 리스트뷰에 어댑터 설정 */
		mListView = (ListView)findViewById(R.id.ipAddress_listView);
		mListView.setAdapter(mAdapter);
		
		/* 리스트뷰의 click Listener 설정 */
		mListView.setOnItemClickListener(new OnItemClickListener(){
			
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				/* Database에서 Position에 해당하는 데이터를 가져온다  */
				data.moveToPosition(position);
				String name      = data.getString(data.getColumnIndex("name"));
				String ipAddress = data.getString(data.getColumnIndex("ipAddress"));
				int    Id        = data.getInt   (data.getColumnIndex("_id"));
				int    recording = data.getInt	 (data.getColumnIndex("recording"));
				
				Intent intent = new Intent(getApplicationContext(),activity_connect_to_ipAddress.class);
				/* intent로 데이터를 전달한다 */
				Bundle bundleData = new Bundle();
				bundleData.putInt	("_ID", Id);
				bundleData.putString("NAME", name);
				bundleData.putString("IP_ADDRESS", ipAddress);
				bundleData.putInt	("RECORDING", recording);
				intent.putExtra		("DATA", bundleData);
				startActivity(intent);
			}
		});
	}
	
	/* 액션바 설정 */
	public boolean onCreateOptionsMenu(Menu menu) {
	    getMenuInflater().inflate(R.menu.activity_main_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
	    switch(item.getItemId())
	    {
	    	/* Add 버튼을 눌렀을 때 */     
	    	case R.id.activity_main_menu_add:
	    	{
	    		Intent intent = new Intent(this, activity_add_ipAddress.class);
				startActivity(intent);
				break;
	    	}
			/* 로그 버튼을 눌렀을 떄 */
	    	case R.id.activity_main_menu_log:
	    	{
				Intent intent = new Intent(this, activity_log_DB_list.class);
				startActivity(intent);
				break;
			}
	    	default:
	    		return false;
	    		
	    }
	    return true;
	}
	
}
