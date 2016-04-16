package com.example.socketclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;


public class activity_show_log_list extends Activity{
	
	private int Position;
	private LogDataManager mLogDataManager;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_log_list);
		
		mLogDataManager = LogDataManager.getInstance(this);
	 
		/* 리스트뷰에 어댑터 설정 */
		GridView mListView =(GridView)findViewById(R.id.log_data_GridView);
		mListView.setAdapter(mLogDataManager.getAdapter());
		
		/* ListView 아이템 리스너 */
		mListView.setOnItemClickListener(new OnItemClickListener(){
	
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent(getApplicationContext(),activity_show_log_list_elem.class);
				intent.putExtra("POSITION", position);
				startActivity(intent);
			}		
		});
		/* ListView LongClick Listener */
		mListView.setOnItemLongClickListener(new OnItemLongClickListener(){

			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				Position = position;
				/* AlertDialog 생성 */
				AlertDialog.Builder alert_confirm = new AlertDialog.Builder(activity_show_log_list.this);
				alert_confirm.setMessage("로그를 삭제하시겠습니까?").setCancelable(false).setPositiveButton("확인",
				new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				    	/* 확인 버튼을 누르면 해당 로그파일 삭제 */
				    	mLogDataManager.removeLogData(Position);
				    }
				}).setNegativeButton("취소",
				new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				    /* 취소 */
				    return;
				    }
				});
				AlertDialog alert = alert_confirm.create();
				alert.show();
				return true;
			}
		});
		
	} // onCreate 함수 끝
	
	/* onNewIntent 함수 */
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		/* Adapter생성 및 데이터 설정 */
		
		/* 리스트뷰에 어댑터 설정 */
		GridView mListView =(GridView)findViewById(R.id.log_data_GridView);
		mListView.setAdapter(mLogDataManager.getAdapter());
	}
	
	/* 액션바 설정 */
	public boolean onCreateOptionsMenu(Menu menu) {
	    getMenuInflater().inflate(R.menu.activity_show_log_list_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
	    switch(item.getItemId())
	    {
	    	/* DEL 버튼을 눌렀을 때 */     
	    	case R.id.activity_show_log_list_delete_btn:
	    	{
	    		AlertDialog.Builder alert_confirm = new AlertDialog.Builder(activity_show_log_list.this);
				alert_confirm.setMessage("로그를 전부 삭제하시겠습니까?").setCancelable(false).setPositiveButton("확인",
				new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {			    	
				    	/* 확인 버튼을 누르면 해당 전체 로그 파일 삭제 */
				    	mLogDataManager.clear();
				    }
				}).setNegativeButton("취소",
				new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				    /* 취소 */
				    return;
				    }
				});
				AlertDialog alert = alert_confirm.create();
				alert.show();
	    		break;
	    	}
	    	default:
	    		return false;	
	    }
	    return true;
	}
}
