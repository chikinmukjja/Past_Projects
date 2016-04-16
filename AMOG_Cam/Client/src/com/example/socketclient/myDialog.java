package com.example.socketclient;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;

public class myDialog extends Dialog{
	
	Button mOkBtn     = null;
	Button mCancelBtn = null;
	
	public myDialog(Context context)
	{
		super(context);
		
		setContentView(R.layout.dialog_layout);
		
		mOkBtn     = (Button)findViewById(R.id.dialog_layout_ok_btn);
		mCancelBtn = (Button)findViewById(R.id.dialog_layout_cancel_btn);
	}
	
	public void setOkClickListener(View.OnClickListener _okListener)
	{
		mOkBtn.setOnClickListener(_okListener);
	}
	
	public void setCancelClickListener(View.OnClickListener _cancelListener)
	{
		mCancelBtn.setOnClickListener(_cancelListener);
	}
	
}
