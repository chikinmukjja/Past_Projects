package com.example.socketclient;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


public class LogDBManager {
	
	static final String DB_LOG    = "Log.db";
	static final String TABLE_LOG = "Log";
	static final int DB_VERSION   = 1;
	
	Context mContext = null;
	
	private static LogDBManager 	  mDbManager = null;
	private 	   SQLiteDatabase     mDatabase  = null;
	
	public static LogDBManager getInstance(Context context)
	{
		if(mDbManager == null)
		{
			mDbManager = new LogDBManager(context);
		}
		return mDbManager;
	}
	
	private LogDBManager(Context context)
	{
		mContext = context;
		/* DB Manager를 생성 할 때 DB를 생성하고 연다. */
		mDatabase = context.openOrCreateDatabase(DB_LOG,Context.MODE_PRIVATE, null);
		/* 테이블이 존재 하지 않는다면 테이블을 생성한다. */
		mDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_LOG +
				          "(" + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "+
				          "time          TEXT, "+
				          "image 	     BLOC ); ");
	}
	
	/* 새로운 ipAddress 추가 함수 */
	public long insert(ContentValues addRowValue)
	{
		return mDatabase.insert(TABLE_LOG, null, addRowValue);
	}
	
	/* Cursor 객체 반환 */
	public Cursor query(String[] columns, String selection, String[] selectionArgs, 
						String groupBy, String having, String orderBy)
	{
		return mDatabase.query(TABLE_LOG, columns, selection, selectionArgs, groupBy, having, orderBy);
	}
	
	/* 삭제 */
	public int delete(String whereClause, String[] whereArgs)
	{
		return mDatabase.delete(TABLE_LOG, whereClause, whereArgs);
	}
	/* position row에 해당하는 이미지를 데이터 베이스에서 가져온다. */
	public Bitmap getImageByPosition(int position)
	{
		String[] columns = new String[]{"_id", "time", "image"};
		Cursor cursor = this.query(columns, null, null, null, null, null);
		
		cursor.moveToPosition(position);
		byte[] byteArray = cursor.getBlob(cursor.getColumnIndex("image"));
		
		Bitmap image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
		
		cursor.close();
		return image;
	}
}
