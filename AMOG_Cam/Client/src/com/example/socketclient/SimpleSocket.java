
package com.example.socketclient;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class SimpleSocket extends Thread{
	
	private Socket 		  socket;
	private SocketAddress socketAddress;
	private String 		  IPAddress;
	
	private boolean isConnected;		  // 서버와 연결되어 있는 경우 값이 true이다.	
	private boolean isAlarmed;            // 움직임이 포착 되었을 때 값이 true로 바뀐다. 
	private boolean motionDetection;      // Alarm을 울리도록 설정 했을 때 값이 true이다.
	private boolean isCaptured;           // capture 버튼을 눌렀을 때 값이 true로 바뀐다.
	private boolean isActivityRunning;    // capture 화면을 보여주는 Activity가 실행중인 경우
	private InputStream 	 in;	
	private OutputStream 	 out;
	private DataOutputStream dout;

	/* SimpleSocket class 생성자 */
	public SimpleSocket(String addr, int portNum)
	{
		isConnected       = false;
		motionDetection   = false;
		isCaptured        = false;
		isAlarmed         = false;
		isActivityRunning = false;
		
		IPAddress       = addr;
		socketAddress   = new InetSocketAddress(addr, portNum);
	}
	/* 서버와 연결하는 method */
	public boolean connect()
	{
		try {
			socket = new Socket();
			socket.connect(socketAddress, 1500);
			in     = socket.getInputStream();			
			out    = socket.getOutputStream();
			dout   = new DataOutputStream(out);
		}catch (IOException e) {
			e.printStackTrace();
			isConnected = false;
			return false;
		}
		isConnected = true;
		return true;
	}
	public void run()
	{
		/* 연결이 되지 않는 경우 */
		if(connect() == false)
		{
			return;
		}
	}
	/* server와의 연결  되어 있으면 true를 return, 아닌 경우에는 false를 return */
	public boolean getisConnected()
	{
		return isConnected;
	}
	
	/* size만큼 inputStream에서 데이터를 읽는 함수 */
	private byte[] readExactly(InputStream input, int size) throws IOException
	{
	    byte[] data = new byte[size];
	    int index = 0;
	    while (index < size)
	    {
	    	if(input == null)
	    		return null;
	        int bytesRead = input.read(data, index, size - index);
	        if (bytesRead < 0)
	        {
	            throw new IOException("Insufficient data in stream");
	        }
	        index += bytesRead;
	    }
	    return data;
	}
	
	/* receive Image byte Array and change byte Array to Bitmap */
	public Bitmap receiveImage(Context context)
	{
		int 	fileSize;
		byte[]  imageByte;
		
		/* get ImageSize */
		try {
			fileSize = receiveImageSize();
		} catch (IOException e1) {
			e1.printStackTrace();
			this.reset();
			return null;
		}
		/* If fail to get a file size */
		if(fileSize == -1)
			return null;
		/* get image byte */
		try {
			imageByte = readExactly(in, fileSize);
		} catch (IOException e) {
			e.printStackTrace();
			this.reset();
			return null;
		}
		
		if(imageByte == null || (isActivityRunning == false && isAlarmed == false && isCaptured == false && activity_show_webcam.widgetCreated == false) )		
			return null;
		
		/* get bitmap */
		Bitmap bitmap = BitmapFactory.decodeByteArray(imageByte, 0, imageByte.length);
		
		/* If isAlaremd is true, save bitmap Image */
		if(isAlarmed == true || isCaptured == true)
		{
			/* save bitmap Image */
			LogDataManager mLDManager = LogDataManager.getInstance(context);
			mLDManager.addLogData(bitmap);
			/* set isAlarmed false */
			if(motionDetection == false)
				isAlarmed = false;
			/* set isCaptured false */
			isCaptured = false;
		}
		
		return bitmap;
	}
	
	/* receive Image size */
	private int receiveImageSize() throws IOException
	{
		byte[] b = new byte[10];
		int ImageSize = 0;
		in.read(b, 0, 10);
		String Header 		  = new String(b, 0, 10);
		String imageSize      = Header.substring(1, 10);
		String storeImageByte = Header.substring(0, 1);
		if(storeImageByte.equals("1")){
			isAlarmed = true;
		}
		try{
			ImageSize = Integer.parseInt(imageSize);
		}catch(NumberFormatException e)
		{
			e.printStackTrace();
			return -1;
		}
		return ImageSize;
	}
	
	/* send Message */
	public void sendMessage(String msg)
	{
		if(msg != null){
			try {
				dout.writeUTF(msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}	
	
	/* 객체를 초기화 시켜준다 */
	public void reset() 
	{
		isAlarmed   	  = false;
		isCaptured  	  = false;
		isConnected		  = false;
		isActivityRunning = false;
		/* OutputStream, DataOutputStream close */
		if(dout != null){
			try {
				dout.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(out  != null){
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		/* InputStream, Socket close */
		if(in != null){
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(socket != null){
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}	

	public boolean getMotionDetection() {
		return motionDetection;
	}
	public void setMotionDetection(boolean motionDetection) {
		this.motionDetection = motionDetection;
	}
	
	public boolean getisAlarmed()
	{
		return isAlarmed;
	}
	public void setisAlarmed(boolean isAlarmed)
	{
		this.isAlarmed = isAlarmed;
	}
	
	public boolean isCaptured() {
		return isCaptured;
	}
	public void setCaptured(boolean isCaptured) {
		this.isCaptured = isCaptured;
	}
	public String getIPAddress() {
		return IPAddress;
	}
	public boolean isActivityRunning() {
		return isActivityRunning;
	}
	public void setActivityRunning(boolean isActivityRunning) {
		this.isActivityRunning = isActivityRunning;
	}	
}
