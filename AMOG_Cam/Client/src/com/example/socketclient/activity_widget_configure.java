
package com.example.socketclient;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class activity_widget_configure extends Activity{
	
	SimpleSocket socket;
	private TextView 	 IPaddress_textView;
	private Button   	 connect_btn;
	private int          mAppWidgetId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_widget_configure);
		
		socket 			   = activity_connect_to_ipAddress.socket;
		IPaddress_textView = (TextView)findViewById(R.id.activity_widget_configure_ipAddressTV);
		connect_btn		   = (Button)  findViewById(R.id.activity_widget_configure_connectBtn);
		
		/* 위젯의 아이디를 가져온다 */
		Bundle mExtras 	   = getIntent().getExtras();
		if (mExtras != null) {
		    mAppWidgetId = mExtras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		
		if(socket != null && socket.getisConnected())
		{
			if(activity_show_webcam.widgetCreated == false){
				IPaddress_textView.setText(socket.getIPAddress());
				connect_btn.setVisibility(View.VISIBLE);
			}
			else
			{
				IPaddress_textView.setText("위젯이 이미 생성 되었습니다.");
			}
		}
		else
		{
			IPaddress_textView.setText("연결 중이 아닙니다. 연결 후 추가해 주세요");
		}
	}
	
	public void onClick(View v)
	{
		if(v.getId() == R.id.activity_widget_configure_connectBtn)
		{
			/* 위젯 아이디를 set */
			activity_show_webcam.setWidgetId(mAppWidgetId);
			/* 위젯 아이디를 결과값으로 리턴*/
			Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
			setResult(RESULT_OK, resultValue);
			finish();
		}
	}
	
}
