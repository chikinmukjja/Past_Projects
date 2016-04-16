package com.example.socketclient;

import android.app.Activity;
import android.content.ContentValues;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class activity_add_ipAddress extends Activity{
	EditText ipAddressET;
	EditText nameET;
	public IPAddressDBManager mDbManager;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_ip_address);
		
		ipAddressET = (EditText)findViewById(R.id.address);
		nameET      = (EditText)findViewById(R.id.name);
		mDbManager  = IPAddressDBManager.getInstance(this);
	}
	
	public void onClick(View v)
	{
		switch(v.getId())
		{
			/* 추가 버튼 */
			case R.id.insert:
			{
				String ipAddress   = ipAddressET.getText().toString();
				String name        = nameET     .getText().toString();
				if(ipAddress.equals("")==true)
				{
					ipAddressET.requestFocus();
					Toast.makeText(this, "IP 주소를 입력하세요", Toast.LENGTH_SHORT).show();
				}
				else if(name.equals("")==true)
				{
					nameET.requestFocus();
					Toast.makeText(this, "name을 입력하세요", Toast.LENGTH_SHORT).show();
				}
				else{
					int 	 i;
					String[] ipAddressArray = ipAddress.split("\\.");
					if(ipAddressArray.length == 1)
					{
						ipAddressET.setText("");
						ipAddressET.requestFocus();
						Toast.makeText(this, "IP 주소 형식이 올바르지 않습니다", Toast.LENGTH_SHORT).show();
						break;
					}
					for(i = 0; i < ipAddressArray.length; i++)
					{
						try{
							Integer.parseInt(ipAddressArray[i]);
						}catch(NumberFormatException e)
						{
							ipAddressET.setText("");
							ipAddressET.requestFocus();
							Toast.makeText(this, "IP 주소 형식이 올바르지 않습니다", Toast.LENGTH_SHORT).show();
							break;
						}
					}
					if(i == ipAddressArray.length){
						/* Database에 정보 추가 */
						ContentValues insertRowValue = new ContentValues();
						insertRowValue.put("name",name);
						insertRowValue.put("ipAddress", ipAddress);
						insertRowValue.put("recording", 0);
						if(mDbManager.insert(insertRowValue) == -1)
						{
							/* 데이터베이스 추가 실패 */
							Toast.makeText(getApplicationContext(), "FAIL", Toast.LENGTH_SHORT).show();
						}
						/* 액티비티 종료 */
						finish();
					}
				}
				break;
			}
		}
	}
	
}
