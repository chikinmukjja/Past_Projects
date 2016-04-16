package com.example.socketclient;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class activity_connect_to_ipAddress extends Activity{
	
	public static SimpleSocket socket = null;
	
	int      Id;
	int      recording;
	String   name;
	String   ipAddress;
	
	TextView nameTV;
	TextView ipAddressTV;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_connect_to_ip_address);
		
		nameTV	    = (TextView)findViewById(R.id.conntect_activity_name);
		ipAddressTV = (TextView)findViewById(R.id.conntect_activity_ipAddress);
		
		Intent intent = getIntent();
		Bundle bundleData = intent.getBundleExtra("DATA");
		
		if(bundleData == null)
			return;
		/* 번들 데이터를 가져오고 TextView에 String을 set 한다. */
		Id               = bundleData.getInt("_ID");
		name 		     = bundleData.getString("NAME");
		ipAddress 		 = bundleData.getString("IP_ADDRESS");
		recording        = bundleData.getInt("RECORDING");
		
		nameTV.setText(name);
		ipAddressTV.setText(ipAddress);
		
	}
	public void onClick(View v)
	{
		switch(v.getId())
		{
			/* 해당 IP 주소에 연결한다 */
			case R.id.connect_activity_connect_btn:
			{
				socket = new SimpleSocket(ipAddress, 8111);
				socket.start();
				/* socket Thread가 끝날 때 까지 기다린다 */
				try {
					socket.join();
					} catch (InterruptedException e) {
					break;
				}
				/* 서버에 연결 성공 */
				if(socket.getisConnected() == true)
				{
					/* activity_show_webcam 실행 */
					Intent intent = new Intent(this, activity_show_webcam.class);
					/* id, recording 값을 전달 */
					Bundle bundleData = new Bundle();
					bundleData.putInt("_ID"      , Id);
					bundleData.putInt("RECORDING", recording);
					intent.putExtra  ("DATA", bundleData);
					startActivity(intent);
				}
				/* 서버에 연결 실패 */
				else
				{
					Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show();
					socket = null;
				}
				break;
			}
			/* 해당 IP 주소를 삭제한다 */
			case R.id.connect_activity_delete_btn:
			{
				String whereClause = "_id='"+Id+"'";
				IPAddressDBManager mDbManager= IPAddressDBManager.getInstance(this);
				mDbManager.delete(whereClause, null);
				finish();
				break;
			}
			
		}
		
	} // onClick 함수 끝

}
