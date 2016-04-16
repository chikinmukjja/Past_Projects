package com.example.socketclient;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;

public class LogDataManager {
	/* 상수 */
	static final int MAX_COUNT = 12;
	/* 싱글톤 객체 */
	private static LogDataManager mLDManager;
	/* 멤버 변수 */
	private ArrayList<LogData>  LogDataArrayList;
	private LogDataBaseAdapter  mAdapter;
	
	public static void resetInstance()
	{
		if(mLDManager != null){
			mLDManager = null;
		}
	}
	
	public static LogDataManager getInstance(Context context)
	{
		if(mLDManager == null)
		{
			mLDManager = new LogDataManager(context);
		}
		return mLDManager;
	}
	private LogDataManager(Context context)
	{
		LogDataArrayList = new ArrayList<LogData>();
		mAdapter         = new LogDataBaseAdapter(context, LogDataArrayList);
	}
	/* LogData를 추가 하는 함수 */
	public void addLogData(Bitmap image)
	{
		LogData newData 	= new LogData(image);
		/* LogDataArrayList의 크기가 MAX_COUNT인 경우 ArrayList의 첫번째를 제거하고 추가한다 */
		if(LogDataArrayList.size() == MAX_COUNT)
		{
			mAdapter.delete(0);
		}
		mAdapter.add(LogDataArrayList.size(), newData);
	}
	
	public void clear()
	{
		mAdapter.clear();
	}
	/* LogData를 삭제하는 함수 */
	public boolean removeLogData(int position)
	{
		if(position >= 0 && position < LogDataArrayList.size()){
			mAdapter.delete(position);
			return true;
		}
		else
			return false;
	}
	
	/* position에 해당하는 bitmapImage를 return */
	public Bitmap getBitmapImage(int position)
	{
		if(position >= 0 && position < LogDataArrayList.size())
			return LogDataArrayList.get(position).getImage();
		else
			return null;
	}
	/* position에 해당하는 time를 return */
	public String getTime(int position)
	{
		if(position >= 0 && position < LogDataArrayList.size())
			return LogDataArrayList.get(position).getTime();
		else
			return null;
	}
	
	/* position에 해당하는 time를 return */
	public String getTimeWithoutDate(int position)
	{
		if(position >= 0 && position < LogDataArrayList.size())
			return LogDataArrayList.get(position).getTimeWithoutDate();
		else
			return null;
	}
	
	
	public LogDataBaseAdapter getAdapter()
	{
		return mAdapter;
	}
	
	public int getArrayListSize()
	{
		return LogDataArrayList.size();
	}
}
