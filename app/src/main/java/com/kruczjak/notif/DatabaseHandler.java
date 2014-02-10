package com.kruczjak.notif;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Database to store informations about notifications which had been showed.
 *
 * @author Jakub Kruczek
 */
public class DatabaseHandler extends SQLiteOpenHelper {
    //version
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "notificator.db";
    //table name
    private static final String TABLE_MAIN = "main";
    private static final String NOTIFICATION_ID = "_id";
    private static final String UPDATED_TIME = "upTime";
    private static final String TITLE_TEXT = "title";
    private static final String HREF = "href";
    private static final String IS_UNREAD = "unr";
    private static final String OBJECT_TYPE = "type";


    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //create table
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MAIN_TABLE = "CREATE TABLE " + TABLE_MAIN + "("
                + NOTIFICATION_ID + " integer primary key, " + UPDATED_TIME + " integer, " + TITLE_TEXT + " text, "
                + HREF + " text, " + IS_UNREAD + " TINYINT, " + OBJECT_TYPE + " text" + ")";
        db.execSQL(CREATE_MAIN_TABLE);
    }

    //upgrade table
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MAIN);
        // Create tables again
        onCreate(db);
    }

    /**
     * Add to database notification_id and updated_time.
     * @param notification_id
     * @param updated_time
     */
     /*public void addnotification(int notification_id, int updated_time) {
         SQLiteDatabase db = this.getWritableDatabase();
		 ContentValues values = new ContentValues();
		 Log.i(DATABASE_NAME, "TEST");	 
		 values.put(NOTIFICATION_ID, notification_id);
		 values.put(UPDATED_TIME, updated_time);
		 db.insert(TABLE_NOTREADED, null, values);
		 Log.i(DATABASE_NAME, "Succesful added id");
	 } */

    /**
     * Add to Main database
     *
     * @param notification_id
     * @param updated_time
     * @param title_text
     * @param href
     * @param is_unread
     * @param object_type
     */
    public void addMainnotification(int notification_id, int updated_time, String title_text, String href, int is_unread, String object_type) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        Log.i(DATABASE_NAME, "SAVE TO MAIN");
        values.put(NOTIFICATION_ID, notification_id);
        values.put(UPDATED_TIME, updated_time);
        values.put(TITLE_TEXT, title_text);
        values.put(HREF, href);
        values.put(IS_UNREAD, is_unread);
        values.put(OBJECT_TYPE, object_type);
        db.insert(TABLE_MAIN, null, values);
        Log.i(TABLE_MAIN, "Succesful added id");
    }

    /**
     * Update notification record with new updated_time
     * @param updated_time
     */
     /*public void updatenotification(int notification_id, int updated_time) {
		 SQLiteDatabase db = this.getWritableDatabase();
		 ContentValues values = new ContentValues();
		 values.put(UPDATED_TIME, updated_time);
		 db.update(TABLE_NOTREADED, values , NOTIFICATION_ID + " = "+ notification_id,null );
		 Log.i(DATABASE_NAME, "Succesful update id");
	 }*/

    /**
     * Update notificationMain record with new things
     *
     * @param notification_id
     * @param updated_time
     * @param is_unread
     * @param title_text
     */
    public void updateMainnotification(int notification_id, int updated_time, int is_unread, String title_text) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UPDATED_TIME, updated_time);
        values.put(TITLE_TEXT, title_text);
        values.put(IS_UNREAD, is_unread);
        db.update(TABLE_MAIN, values, NOTIFICATION_ID + " = " + notification_id, null);
        Log.i(TABLE_MAIN, "Succesful update id");
    }

    /**
     * Tests if notification_id and updated_time is in the base.
     * @param notification_id
     * @param updated_time
     * @return true if exists, false if different or none
     */
	 /*public boolean getnotification(int notification_id, int updated_time) {
		 boolean wynik;
		 SQLiteDatabase db = this.getReadableDatabase();
		 Cursor cursor = db.query(TABLE_NOTREADED, new String [] {NOTIFICATION_ID,UPDATED_TIME}, NOTIFICATION_ID + " = "+ notification_id , null, null, null, null);
		 cursor.moveToFirst();
		 if (cursor.isAfterLast()){
		        wynik=false;
		        Log.d(DATABASE_NAME,"nie ma");
				addnotification(notification_id,updated_time);
		 }
		 else {
			 Log.d(DATABASE_NAME,"jest");
			 
			 if (cursor.getString(cursor.getColumnIndex(UPDATED_TIME))==Integer.toString(updated_time)) { //TODO wtf?
				 Log.d(DATABASE_NAME,"inny czas");
				 Log.d(DATABASE_NAME, cursor.getString(cursor.getColumnIndex(UPDATED_TIME)));
				 Log.d(DATABASE_NAME,Integer.toString(updated_time)); 
			 	 wynik=false;
			 	 updatenotification(notification_id, updated_time);
			 } else {
				 Log.d(DATABASE_NAME,"jest taki sam");
				 Log.d(DATABASE_NAME, cursor.getString(cursor.getColumnIndex(UPDATED_TIME)));
				 Log.d(DATABASE_NAME,Integer.toString(updated_time)); 
				 wynik=true;
			 } 
		 }
		 cursor.close();
		 return wynik;
	 }*/

    /**
     * Check for updated in Starter listview.
     *
     * @param notification_id
     * @param updated_time
     * @param title_text
     * @param href
     * @param is_unread
     * @param object_type
     */
    public void checkandgo(int notification_id, int updated_time, String title_text, String href, int is_unread, String object_type) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MAIN, new String[]{UPDATED_TIME, IS_UNREAD}, NOTIFICATION_ID + " = " + notification_id, null, null, null, null);
        cursor.moveToFirst();
        if (cursor.isAfterLast()) {
            Log.d(TABLE_MAIN, "nie ma");
            addMainnotification(notification_id, updated_time, title_text, href, is_unread, object_type);    //maybe not here
        } else {
            Log.d(TABLE_MAIN, "jest");
            if (cursor.getInt(cursor.getColumnIndex(UPDATED_TIME)) == updated_time && cursor.getInt(cursor.getColumnIndex(IS_UNREAD)) == is_unread) {
                Log.d(TABLE_MAIN, "jest dok�adnie taki sam");
            } else {
                Log.d(TABLE_MAIN, "inne");
                Log.d(TABLE_MAIN, Integer.toString(updated_time));
                Log.d(TABLE_MAIN, Integer.toString(cursor.getInt(cursor.getColumnIndex(UPDATED_TIME))));
                Log.d(TABLE_MAIN, Integer.toString(is_unread));
                Log.d(TABLE_MAIN, Integer.toString(cursor.getInt(cursor.getColumnIndex(IS_UNREAD))));
                updateMainnotification(notification_id, updated_time, is_unread, title_text);
            }
        }
        cursor.close();
    }

    public Cursor getMainall() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_MAIN, null, null, null, null, null, UPDATED_TIME + " DESC"); //maybe add some limit
    }

    public String getLastUpdated() {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT MAX(" + UPDATED_TIME + ") FROM " + TABLE_MAIN;
        Cursor cursor = db.rawQuery(sql, null);
        String wynik;
        cursor.moveToFirst();
        if (cursor.isAfterLast()) {
            wynik = null;
        } else {
            wynik = cursor.getString(0);
        }
        cursor.close();
        Log.i("notifications.db", wynik + "first");
        return wynik;

    }

    public void deleteAndCreateDB() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MAIN);
        onCreate(db);
    }
}
