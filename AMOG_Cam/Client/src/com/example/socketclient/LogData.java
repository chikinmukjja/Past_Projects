package com.example.socketclient;
import java.sql.Date;
import java.text.SimpleDateFormat;
import android.graphics.Bitmap;

public class LogData {
	
	private Bitmap Image;
	private String time;
	private String timeWithoutDate;
	
	public LogData(Bitmap image)
	{
		Image 		  			    = image;
		Date date	   		 		= new Date(System.currentTimeMillis());
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd, hh:mm:ss a"); 
		time 				        = dateFormat.format(date).toString();
		
		String[] dateArray = time.split(",");
		timeWithoutDate    = dateArray[1];
	}
	public Bitmap getImage() {
		return Image;
	}
	public String getTime() {
		return time;
	}
	public String getTimeWithoutDate() {
		return timeWithoutDate;
	}
}
