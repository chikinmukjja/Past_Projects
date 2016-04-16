package com.example.socketclient;

import java.io.ByteArrayOutputStream;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class activity_show_log_list_elem extends Activity{
		
		int         position;
		Bitmap      image;
		String      time;
		
		ActionBar   actionBar;
		ViewFlipper fliper;
		float       xAtDown;
		float       xAtUp;
		
		ImageView      imageView;
		LogDataManager mLogDataManager;

		
		protected void onCreate(Bundle savedInstanceState) {
		
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_show_log_list_elem);
			
			/* 전체 화면으로 설정 */
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			/* 변수 초기화*/
			fliper              = (ViewFlipper)findViewById(R.id.flipper);			
			imageView 			= (ImageView)  findViewById(R.id.show_log_imageView);
			actionBar 	        = getActionBar();
			mLogDataManager     = LogDataManager.getInstance(this);
			/* 인텐트 정보 받기 */
			Intent    intent    = getIntent();              
			position            = intent.getIntExtra("POSITION", 11);
			
			image = mLogDataManager.getBitmapImage(position);
			time  = mLogDataManager.getTime(position);			
			
			/* actionBar 설정 */
			actionBar.setTitle(mLogDataManager.getTimeWithoutDate(position));
			actionBar.hide();
			
			/* ImageView Layout 설정 */
			Point winSize   = new Point();
			this.getWindowManager().getDefaultDisplay().getRealSize(winSize);
			FrameLayout.LayoutParams imageViewLayout = new FrameLayout.LayoutParams(winSize.x, winSize.x);
			imageViewLayout.setMargins(0, (winSize.y - winSize.x)/2, 0, 0);
			imageView.setLayoutParams (imageViewLayout);
			
			/* flipper TouchListener 설정 */
			fliper.setOnTouchListener(fliperTouchListenr);
			if(image != null)
				imageView.setImageBitmap(image);
		}
		
		/* 액티비티 화면 터치 이벤트 */
		public boolean onTouchEvent(MotionEvent ev)
		{
			switch(ev.getAction()){
				case MotionEvent.ACTION_UP: 
				{
					if(actionBar.isShowing())
						actionBar.hide();
					else
						actionBar.show();
					break;
				}
			}
			return super.onTouchEvent(ev);
		}
		
		/* Flipper onTouchListener */
		View.OnTouchListener fliperTouchListenr = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(v.getId() == R.id.flipper)
				{
					switch(event.getAction())
					{
						case MotionEvent.ACTION_DOWN:
						{
							xAtDown = event.getX();
							break;
						}
						case MotionEvent.ACTION_UP:
						{
							xAtUp = event.getX();
							/* 왼쪽 애니메이션 */
							if(xAtUp - xAtDown >= 30)
							{
								int privious = position -1;
								if(privious >= 0 && privious < mLogDataManager.getArrayListSize()){
									/* 이미지 뷰와 액션바의 제목 바꾸기 */
									image = mLogDataManager.getBitmapImage(privious);
									time  = mLogDataManager.getTime(privious);
									imageView.setImageBitmap(image);
									actionBar.setTitle		(mLogDataManager.getTimeWithoutDate(privious));
									position = privious;
									fliper.setInAnimation (AnimationUtils.loadAnimation(getApplicationContext(), R.anim.push_left_in));
									fliper.setOutAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.push_left_out));
									fliper.showNext();
								}

							}
							/* 오른쪽 애니메이션 */
							else if(xAtDown - xAtUp >= 30)
							{
								int Next = position + 1;
								if(Next >= 0 && Next < mLogDataManager.getArrayListSize()){
									image = mLogDataManager.getBitmapImage(Next);
									time  = mLogDataManager.getTime(Next);
									imageView.setImageBitmap(image);
									actionBar.setTitle	    (mLogDataManager.getTimeWithoutDate(Next));
									position = Next;
								}
								else{
									if(image == mLogDataManager.getBitmapImage(position)){
										break;
									}
									image = mLogDataManager.getBitmapImage(position);
									time  = mLogDataManager.getTime(position);
									imageView.setImageBitmap(image);
									actionBar.setTitle	    (mLogDataManager.getTimeWithoutDate(position));
								}
								/* flipper 효과 적용*/
								fliper.setInAnimation (AnimationUtils.loadAnimation(getApplicationContext(), R.anim.push_right_in));
								fliper.setOutAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.push_right_out));
								fliper.showNext();
							}
							else
								return false;
							break;
						}
					}
					return true;
				}
				else
					return false;
			}
		};
		/* 액션바 설정 */
		public boolean onCreateOptionsMenu(Menu menu) {
		    getMenuInflater().inflate(R.menu.activity_show_log_list_elem_menu, menu);
		    return true;
		}
		
		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			
		    switch(item.getItemId())
		    {
		    	/* delete 버튼을 눌렀을 때 */     
		    	case R.id.activity_show_log_list_elem_delete_btn:
		    	{
		    		LogDataManager mLogDataManager = LogDataManager.getInstance(this);
		    		mLogDataManager.removeLogData(position);
		    		finish();
		    		break;
		    	}
		    	/* save 버튼을 눌렀을 때 */
		    	case R.id.activity_show_log_list_elem_sava_btn:
		    	{		    		
		    		LogDBManager mDBManager = LogDBManager.getInstance(getApplicationContext());
					
					/* Bitmap이미지를 byteArray로 변환 */
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
					byte[] imageByte = stream.toByteArray();
					
					/* ContentValue 생성 */
					ContentValues addRowValue = new ContentValues();
					addRowValue.put("time" , time);
					addRowValue.put("image", imageByte);
					
					/* 데이터 베이스에 추가 */
					if(mDBManager.insert(addRowValue) >= 0)
						Toast.makeText(getApplicationContext(),	"저장" , Toast.LENGTH_SHORT).show();
					else
						Toast.makeText(getApplicationContext(), "FAIL", Toast.LENGTH_SHORT).show();
					
					break;
		    	}
		    	default:
		    		return false;	
		    }
		    return true;
		}
}
