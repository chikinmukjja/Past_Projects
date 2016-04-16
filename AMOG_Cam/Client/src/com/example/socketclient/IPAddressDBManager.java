package com.example.socketclient;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;


public class IPAddressDBManager extends SQLiteOpenHelper{
	
	static final String DB_IPADDRESS    = "IpAddress.db";
	static final String TABLE_IPADDRESS = "IpAddress";
	static final int DB_VERSION 		= 2;
	
	Context mContext = null;
	
	private static IPAddressDBManager mDbManager = null;
	
	public static IPAddressDBManager getInstance(Context context)
	{
		if(mDbManager == null)
		{
			mDbManager = new IPAddressDBManager(context, DB_IPADDRESS, null, DB_VERSION);
		}
		return mDbManager;
	}
	
	private IPAddressDBManager(Context context, String dbName, CursorFactory factory, int version)
	{
		super(context, dbName, factory, version);
		mContext = context;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_IPADDRESS +
		          "(" + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "+
		          "name          TEXT, "+
		          "ipAddress     TEXT, "+
		          "recording     INTEGER ); ");
	}
	
	public void onOpen(SQLiteDatabase db)
	{
		super.onOpen(db);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		if(oldVersion < newVersion)
		{
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_IPADDRESS);
			onCreate(db);
		}
	}
	
	/* 새로운 ipAddress 추가 함수 */
	public long insert(ContentValues addRowValue)
	{
		return getWritableDatabase().insert(TABLE_IPADDRESS, null, addRowValue);
	}
	
	/* Cursor 객체 반환 */
	public Cursor query(String[] columns, String selection, String[] selectionArgs, 
						String groupBy, String having, String orderBy)
	{
		return getReadableDatabase().query(TABLE_IPADDRESS, columns, selection, selectionArgs, groupBy, having, orderBy);
	}
	
	/* 삭제 */
	public int delete(String whereClause, String[] whereArgs)
	{
		return getWritableDatabase().delete(TABLE_IPADDRESS, whereClause, whereArgs);
	}
	
	/* 갱신 */
	public int update(ContentValues updateRowValue, String whereClause, String[] whereArgs)
	{
		return getWritableDatabase().update(TABLE_IPADDRESS, updateRowValue, whereClause, whereArgs);
	}
}

