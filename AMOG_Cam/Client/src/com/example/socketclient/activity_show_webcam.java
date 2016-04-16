

package com.example.socketclient;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.Toast;

public class activity_show_webcam extends Activity{
	
	SimpleSocket socket = activity_connect_to_ipAddress.socket;
	
	int Id;
	int recording;

	ImageView webcamImage;
	ImageView disconnectImage;
	Button sendBtn;
	
	Thread receiveThread;
	Bitmap bitmap;
	Handler mHandler = new Handler();
	
	Notification.Builder builder;     // 알림
	
	boolean progressRecording;        // 녹화 중인 경우 true, 그렇지 않은 경우는 false
	
	private long mExitModeTime = 0L;  // 이전 키 두 번 눌렀을 때의 시간차이를 계산
	Toast mToast;						
	/* 위젯 아이디와 위젯 생성여부를 결정하는 변수 */
	static boolean   widgetCreated	    = false;		// 위젯 생성 시 값이 true
	static int       widgetId           = 0;			// widget의 고유 ID
	AppWidgetManager appWidgetManager;
	RemoteViews    	 remoteView;
	
	/* 서버로 부터 데이터를 받는 Thread */
	Runnable receiveTask = new Runnable()
	{
		@Override
		public void run() {
			Context context = getApplicationContext();
			/* 서버로 부터 데이터를 받는다. */
			while(!Thread.currentThread().isInterrupted())
			{	
				/* 서버로 부터 비트맵 데이터를 받는다 */
				bitmap = socket.receiveImage(context);
				
				if(bitmap == null && socket.getisConnected() == false){
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(getApplicationContext(), "서버와의 연결이 끊켰습니다.", Toast.LENGTH_SHORT).show();
							disconnectImage.setVisibility(View.VISIBLE);
						}
					});
					break;
				}

				/* 위젯에 bitmap이미지를 설정 */
				if(widgetCreated == true)
				{
					remoteView 			   = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							if(bitmap != null){
								remoteView.setImageViewBitmap(R.id.widget_layout_imageView, bitmap);
								appWidgetManager.updateAppWidget(widgetId, remoteView);
							}
						}
					});
				}
				/* 메시지 전달 bitmap이미지를 ImageView에 setting */
				mHandler.post(new Runnable(){
					@Override
					public void run() {
						if(bitmap != null)
							webcamImage.setImageBitmap(bitmap);
					}					
				});	
				/* 움직임이 포착되고 현재 알람 설정이 ON인 경우 */
				if(socket.getMotionDetection() && socket.getisAlarmed())
				{
					socket.setisAlarmed(false);
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							// 고유ID로 알림을 생성.
					        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					        nm.notify(123456, builder.build());					        
						}
					});
				}
			}	// while loop 중료 	
		}
	};
	
	/* disconnectImage의 onClickListner */
	OnClickListener disconnectOnClickListener = new View.OnClickListener() {

		public void onClick(View v) {
			
			String addr = socket.getIPAddress();
			boolean motionDetection = socket.getMotionDetection();
			/* 새로운 SimpleSocket 객체 생성 */
			socket = new SimpleSocket(addr, 8111); 
			socket.setMotionDetection(motionDetection);
			
			socket.start();
			try {
				socket.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
			/* 서버와 연결이 성공했다. */
			if(socket.getisConnected() == true){
				socket.setActivityRunning(true);
				/* 이미지를 받는 쓰레드 생성 및 실행 */
				receiveThread = new Thread(receiveTask);
				receiveThread.start();
				disconnectImage.setVisibility(View.INVISIBLE);
			}
			else
			{
				Toast.makeText(getApplicationContext(), "서버 연결 실패!", Toast.LENGTH_SHORT).show();
			}
		}
	};

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_webcam);
		
		socket.setActivityRunning(true);
		
		webcamImage 	= (ImageView)findViewById(R.id.imageView);
		disconnectImage = (ImageView)findViewById(R.id.disconnectView);
		sendBtn     	= (Button)	 findViewById(R.id.send_btn);
		
		/* 위젯 매니저 생성 */
		appWidgetManager 	   = AppWidgetManager.getInstance(this);
		
		/* ImageView 설정 */
		Point winSize   = new Point();
		this.getWindowManager().getDefaultDisplay().getRealSize(winSize);
		FrameLayout.LayoutParams imageViewLayout = new FrameLayout.LayoutParams(winSize.x, winSize.x);
		imageViewLayout.setMargins(0, (winSize.y - winSize.x)/2, 0, 0);
		
		webcamImage    .setLayoutParams(imageViewLayout);
		disconnectImage.setLayoutParams(imageViewLayout);
		disconnectImage.setOnClickListener(disconnectOnClickListener);
		disconnectImage.getBackground().setAlpha(160);
		
		/* 알람 창 생성 */
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, activity_show_log_list.class), PendingIntent.FLAG_UPDATE_CURRENT);	 
        builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.app_launcher);
        builder.setTicker("움직임이 포착 되었습니다.");
        builder.setWhen(System.currentTimeMillis());
        builder.setContentTitle("C A M");
        builder.setContentText("움직임이 포착 되었습니다.");
        builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
		
        /* id, recording 값을 받는다 */
		Intent intent 	  = getIntent();
		Bundle bundleData = intent.getBundleExtra("DATA");
		
		if(bundleData != null){
			Id            = bundleData.getInt("_ID");
			recording     = bundleData.getInt("RECORDING");
		}
		/* Record Button */
		if(recording == 0){
			sendBtn.setBackground(this.getResources().getDrawable(R.drawable.start_btn));
		    progressRecording = false;
		}
		else if(recording == 1)
		{
			sendBtn.setBackground(this.getResources().getDrawable(R.drawable.end_btn));
			progressRecording = true;
		}
		/* 이미지를 받는 쓰레드 생성 및 실행 */
		receiveThread = new Thread(receiveTask);
		receiveThread.start();
	}
	
	protected void onResume()
	{
		super.onResume();
		if(socket != null)
			socket.setActivityRunning(true);
	}
	
	protected void onPause()
	{
		super.onPause();
		socket.setActivityRunning(false);
		/* 녹화 중이면 Database의 recording값 1 아닌 경우는 0 */
		if(progressRecording == true)
		{
			IPAddressDBManager mDBManager      = IPAddressDBManager.getInstance(this);
			String			   whereClause     = "_id="+Id;
			ContentValues      updateRowValue  = new ContentValues();
			
			updateRowValue.put("recording", 1);
			
			mDBManager.update(updateRowValue, whereClause, null);
		}
		else
		{
			IPAddressDBManager mDBManager      = IPAddressDBManager.getInstance(this);
			String			   whereClause     = "_id="+Id;
			ContentValues      updateRowValue  = new ContentValues();
			
			updateRowValue.put("recording", 0);
			
			mDBManager.update(updateRowValue, whereClause, null);
		}
	}
	
	/* Activity 종료 시 socket server와 연결 종료 */
	protected void onDestroy()
	{
		super.onDestroy();
		/* 서버 데이터를 받는 쓰레드 종료 */
		receiveThread.interrupt();
		/* LogDataManager 초기화 */
		LogDataManager.resetInstance();
		/* 소켓을 닫고 InputStream, OutputStream를 close 한다 */
		socket.reset();		
	} // onDestory 메소드 종료	
	
	/* onClick 함수 */
	public void onClick(View v)
	{
		switch(v.getId())
		{
			/* Message 전송 */
			case R.id.send_btn:
			{
				if(socket.getisConnected() == true){
					/* send 'start' message */
					if(progressRecording == false)
					{
						socket.sendMessage("start");
						sendBtn.setBackground(this.getResources().getDrawable(R.drawable.end_btn));
						progressRecording = true;
					}
					/* send 'finish message'*/
					else if(progressRecording == true)
					{
						socket.sendMessage("finish");
						sendBtn.setBackground(this.getResources().getDrawable(R.drawable.start_btn));
						progressRecording = false;
					}
				}
				else
				{
					Toast.makeText(this, "서버와 연결하세요", Toast.LENGTH_SHORT).show();
				}
				break;
			}
		}
	}
	
	/* Activity 회전 시 */
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
	}
	
	/* onBackPressed 함수 재정의 */
	public void onBackPressed()
	{
		if(mExitModeTime != 0 && SystemClock.uptimeMillis() - mExitModeTime < 3000)
		{
			mToast.cancel();
			finish();
		}
		else
		{
			mToast = Toast.makeText(this, "이전키를 한 번 더 누르면 서버와 연결이 종료됩니다.", Toast.LENGTH_LONG);
			mToast.show();
			mExitModeTime = SystemClock.uptimeMillis();
		}
	}
	/* 액션바 설정 */
	public boolean onCreateOptionsMenu(Menu menu) {
	    getMenuInflater().inflate(R.menu.activity_show_webcam_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
	    switch(item.getItemId())
	    {
	    	/* on 버튼을 눌렀을 때 */     
	    	case R.id.activity_show_webcam_on:
	    	{
	    		if(socket.getMotionDetection() == false){
	    			socket.setMotionDetection(true);
	    			item.setTitle("ON");
	    		}
	    		else {
	    			socket.setMotionDetection(false);
	    			item.setTitle("OFF");
	    		}
				break;
	    	}
	    	
			/* 로그 버튼을 눌렀을 떄 */
	    	case R.id.activity_show_webcam_log:
	    	{
	    		startActivity(new Intent(this,activity_show_log_list.class));
				break;
			}
	    	
	    	/* capture 버튼을 눌렀을 떄 이미지 저장 */
	    	case R.id.activity_show_webcam_capture:
	    	{
	    		socket.setCaptured(true);
	    		break;
	    	}
	    	default:
	    		return false;    		
	    }
	    return true;
	}
	
	/* 위젯 아이디를 설정한다 */
	public static void setWidgetId(int WidgetId)
	{
		widgetId 		 = WidgetId;
		widgetCreated	 = true;
	}
	
} // activity_show_webcam 클래스 종료 
