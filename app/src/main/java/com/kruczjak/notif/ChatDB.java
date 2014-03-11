package com.kruczjak.notif;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ChatDB extends SQLiteOpenHelper {

    private static final String TAG = "ChatDB";

    private static final String DATABASE_NAME = "chat.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_CONTACTS = "con";
    private static final String KEY_ID = "_id";
    private static final String KEY_FBID = "fbid"; // fb id (for chat start and
    private static final String KEY_PHOTO = "ph";
    private static final String KEY_FAV = "f";
    // incoming messages)
    private static final String KEY_NAME = "nm"; // readable name
    private static final String KEY_TIME = "tm"; // time
    private static final String KEY_NOTREADEDNR = "nrn";
    private static final String KEY_SENDED = "sen";
    // for message tables
    private static final String KEY_TEXT = "txtm"; // message's text
    private static final String KEY_READED = "read"; // if read - 1
    private static final String KEY_DIRECTION = "dirc"; // if 0 -> to me 1->												// from me
    // queue to send
    private static final String TABLE_SEND_IT_LATER = "sender";

    public ChatDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_CONTACTS + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_FBID
                + " TEXT, " + KEY_NAME + " TEXT, " + KEY_TIME + " INTEGER, " + KEY_TEXT + " TEXT, " + KEY_DIRECTION + " TINYINT, " + KEY_NOTREADEDNR
                + " INTEGER, " + KEY_PHOTO + " TEXT, " + KEY_FAV + " INTEGER" + ")";
        db.execSQL(CREATE_CONTACTS_TABLE);
        String SEND_CREATE = "CREATE TABLE " + TABLE_SEND_IT_LATER + "(" + KEY_ID + " INTEGER, " + KEY_FBID + " TEXT, " + KEY_TEXT + " TEXT, "
                + KEY_TIME + " LONG" + ")";
        db.execSQL(SEND_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SEND_IT_LATER);
        onCreate(db);
    }

    /**
     * Add new contact to contacts table and close connection to database.
     *
     * @param fbid to send message
     * @param name to display
     */
    private void addContact(String fbid, String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_FBID, fbid);
        values.put(KEY_NAME, name);
        db.insert(TABLE_CONTACTS, null, values);
        db.close();
    }

    /**
     * First add contacts from FB (more secure than rooster)
     *
     * @param list
     */
    public void addFirstTimeContacts(List<String[]> list) {
        SQLiteDatabase db = this.getWritableDatabase();

        for (String[] data : list) {
            ContentValues values = new ContentValues();
            values.put(KEY_FBID, data[1]);
            values.put(KEY_NAME, data[0]);
//            values.put(KEY_PHOTO, data[2]);
            db.insert(TABLE_CONTACTS, null, values);
        }

        db.close();
    }

    /**
     * Add new contact with time.
     */
    private void addContact(String fbid, String name, int time, int dir, String text, int notreaded) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_FBID, fbid);
        values.put(KEY_NAME, name);
        values.put(KEY_TIME, time);
        values.put(KEY_DIRECTION, dir);
        values.put(KEY_TEXT, text);
        values.put(KEY_NOTREADEDNR, notreaded);
        db.insert(TABLE_CONTACTS, null, values);
    }

    /**
     * @param fbid
     * @param name
     */
    private void updateContact(String fbid, String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name);
        db.update(TABLE_CONTACTS, values, KEY_FBID + " = \"" + fbid + "\"", null);
    }

    /**
     * Returns all contacts.
     *
     * @param online
     * @return
     */
    public Cursor getAllContacts(List<String> online) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_CONTACTS + " ORDER BY " + KEY_NAME + " ASC";

        Cursor cursor = db.rawQuery(selectQuery, null);
        return cursor;
    }

    public Cursor getFav() {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_CONTACTS + " WHERE " + "NOT " + KEY_FAV + " IS NULL" + " ORDER BY " + KEY_FAV;
        return db.rawQuery(selectQuery, null);
    }

    public Cursor getOnlineContacts(List<String> online) {
        if (online == null || online.size() == 0) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_CONTACTS + " WHERE " + KEY_FAV + " IS NULL AND (";
        for (int i = 0; i < online.size() - 1; i++) {
            selectQuery += KEY_FBID + "=\"" + online.get(i) + "\" OR ";
        }
        selectQuery += KEY_FBID + "=\"" + online.get(online.size() - 1) + "\"";
        selectQuery += ") ORDER BY " + KEY_NAME;
        return db.rawQuery(selectQuery, null);
    }

    private Cursor getOneContactAndSaveTime(String id, int time, String text, int direction, int unreaded, String name) {
        SQLiteDatabase db = this.getWritableDatabase();

        String selectQuery = "SELECT " + KEY_ID + "," + KEY_NAME + "," + KEY_NOTREADEDNR + "," + KEY_PHOTO + " FROM " + TABLE_CONTACTS + " WHERE " + KEY_FBID + " = \""
                + id + "\"";

        Cursor c = db.rawQuery(selectQuery, null);
        if (!c.moveToFirst()) {
            Log.i(TAG, "User not exists :O, so let's add one, (security function, should not happen)");
            int what = 0;
            if (unreaded == 1)
                what = 1;
            addContact(id, name, time, direction, text, what);
            c = db.rawQuery(selectQuery, null);
            c.moveToFirst();
        } else {
            ContentValues values = new ContentValues();
            values.put(KEY_TIME, time);
            values.put(KEY_DIRECTION, direction);
            values.put(KEY_TEXT, text);

            if (unreaded == 1) {
                if (c.isNull(2)) {
                    values.put(KEY_NOTREADEDNR, 1);
                } else {
                    values.put(KEY_NOTREADEDNR, c.getInt(2) + 1);
                }
            } else {
                values.put(KEY_NOTREADEDNR, 0);
            }

            db.update(TABLE_CONTACTS, values, KEY_FBID + " = \"" + id + "\"", null);
        }

        return c;
    }

    public void updateNotReaded(int id) {
        updateNotReaded(id, 0);
    }

    public void updateNotReaded(int id, int i) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NOTREADEDNR, i);
        db.update(TABLE_CONTACTS, values, KEY_ID + " = " + id, null);
        db.close();
    }

    public void updateAfterChat(int id, String text, int dir, int time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NOTREADEDNR, 0);
        values.put(KEY_TIME, time);
        values.put(KEY_DIRECTION, dir);
        values.put(KEY_TEXT, text);
        db.update(TABLE_CONTACTS, values, KEY_ID + " = " + id, null);
        db.close();
    }

    /**
     * Delete contact.
     *
     * @param fbid
     */
    public void deleteContact(String fbid) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CONTACTS, KEY_FBID + " = " + fbid, null);
        db.close();
    }

    /**
     * Check if present and add contact.
     */
    public boolean checkToAdd(String fbid, String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Cursor cursor = db.query(TABLE_CONTACTS, new String [] {KEY_ID},
        // KEY_ID + " = " + id, null, null, null, null);
        String selectQuery = "SELECT * FROM " + TABLE_CONTACTS + " WHERE " + KEY_FBID + " = \"" + fbid + "\"";
        boolean is = true;
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (!cursor.moveToFirst()) {
            Log.i(TAG, "ADD CONTACT");
            addContact(fbid, name);
        } else if (cursor.getString(2).equals("_New_")) {
            updateContact(fbid, name);
        } else {
            Log.i(TAG, "CONTACT IS PRESENT");
            is = false;
        }
        cursor.close();
        db.close();
        return is;
    }

    private long addMessage(String id, String text, int readed, int direction, int time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TEXT, text);
        values.put(KEY_READED, readed);
        values.put(KEY_DIRECTION, direction);
        values.put(KEY_TIME, time);
        id = "_" + id;
        return db.insert(id, null, values);
    }

    /**
     * Call addMessage, and id return -1, create new one, and gets name and id
     * of user. !very important function!
     *
     * @param fbid table name
     * @param text message body
     * @param time
     * @return bundle equiped with user appid and name
     */
    public Bundle MessageAdder(String fbid, String text, int time) {
        return MessageAdder(fbid, text, time, 1, 0, "_New_");
    }

    public Bundle MessageAdder(String fbid, String text, int time, int unreaded, int direction, String name) {
        Bundle _idAndName = new Bundle();
        Cursor c = getOneContactAndSaveTime(fbid, time, text, direction, unreaded, name);
        String id = c.getString(0);
        _idAndName.putInt("id", c.getInt(0));
        _idAndName.putString("name", c.getString(1));
        _idAndName.putString("fbid", fbid);
        _idAndName.putString("photo", c.getString(3));

        if (c.isNull(2)) {
            _idAndName.putInt("nr", 1);
        } else {
            _idAndName.putInt("nr", c.getInt(2) + 1);
        }

        c.close();

        Log.i(TAG, "Start adding");
        if (addMessage(id, text, unreaded, direction, time) == -1) {
            Log.i(TAG, "No table, creating");
            createMessageTable(id);
            addMessage(id, text, unreaded, direction, time);
        }
        return _idAndName;
    }

    /**
     * AddsMessage
     *
     * @param text
     * @param id
     * @param readed
     * @param direction
     * @param time
     */
    public void addMessageOnThread(String text, String id, int readed, int direction, int time) {
        Log.i(TAG, "Start adding");
        if (addMessage(id, text, readed, direction, time) == -1) {
            Log.i(TAG, "No table, creating"); // almost impossible (should
            // be)...((maybe)), in fact it's
            // possible
            createMessageTable(id);
            addMessage(id, text, readed, direction, time);
        }
        Log.i(TAG, "I think it's a success :D");
    }

    /**
     * Create table for message.
     *
     * @param id table name
     */
    public void createMessageTable(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        String CREATE_MESSAGE_TABLE = "CREATE TABLE IF NOT EXISTS _" + id + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_TEXT
                + " TEXT, " + KEY_READED + " TINYINT, " + KEY_DIRECTION + " TINYINT, " + KEY_TIME + " INTEGER, " + KEY_SENDED + " INTEGER" + ")";
        db.execSQL(CREATE_MESSAGE_TABLE);
    }

    /**
     * Get last contacted friends
     */
    public Cursor getAllLastFriends() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_CONTACTS, null, "NOT " + KEY_TIME + " IS NULL", null, null, null, KEY_TIME + " DESC");
    }

    /**
     * Get messages for thread.
     *
     * @param id
     * @return
     */
    public Cursor getMessages(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query("_" + id, null, null, null, null, null, KEY_TIME + " ASC");
    }

    public void deleteAndCreateDB() {
        SQLiteDatabase db = this.getWritableDatabase();
        List<String> tables = new ArrayList<String>();
        Cursor cursor = db.rawQuery("SELECT * FROM sqlite_master WHERE type='table';", null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String tableName = cursor.getString(1);
            if (!tableName.equals("android_metadata") && !tableName.equals("sqlite_sequence"))
                tables.add(tableName);
            cursor.moveToNext();
        }
        cursor.close();

        for (String tableName : tables) {
            db.execSQL("DROP TABLE IF EXISTS " + tableName);
        }

        onCreate(db);
        db.close();
    }

    public void addOrDeleteFav(String id) {
        if (isFav(id)) {
            deleteFav(id);
        } else {
            addFav(id);
        }
    }

    private void addFav(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_CONTACTS, null, KEY_ID + "=" + id, null, null, null, KEY_FAV + " DESC");
        int put;
        cursor.moveToFirst();
        ContentValues values = new ContentValues();
        if (cursor.isNull(8))
            put = 1;
        else
            put = cursor.getInt(8) + 1;

        cursor.close();
        values.put(KEY_FAV, put);
        db.update(TABLE_CONTACTS, values, KEY_ID + "=" + id, null);
        db.close();
    }

    private void deleteFav(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.putNull(KEY_FAV);
        db.update(TABLE_CONTACTS, values, KEY_ID + "=" + id, null); // TODO check, if
        db.close();
    }

    /**
     * Check if there is fav.
     *
     * @param id
     * @return false - is not, true - is present
     */
    public boolean isFav(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CONTACTS, null, KEY_ID + " = " + id, null, null, null, null);

        if (cursor.moveToFirst() && !cursor.isNull(8))
            return true;

        return false;
    }

    public int getLastUpdated() {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT MAX(" + KEY_TIME + ") FROM " + TABLE_CONTACTS;
        Cursor cursor = db.rawQuery(sql, null);
        int wynik;
        cursor.moveToFirst();
        if (cursor.isAfterLast()) {
            wynik = 0;
        } else {
            wynik = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        Log.i(TAG, wynik + "first");
        return wynik;
    }

    public Cursor getNotNotified() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_CONTACTS, null, KEY_NOTREADEDNR + " IS NOT NULL AND " + KEY_NOTREADEDNR + " <> 0", null, null, null, KEY_TIME
                + " DESC");
    }

    public void addMessageToQueue(int id, String fbid, String text, long time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ID, id);
        values.put(KEY_FBID, fbid);
        values.put(KEY_TEXT, text);
        values.put(KEY_TIME, time);
        db.insert(TABLE_SEND_IT_LATER, null, values);
        db.close();
    }

    public void deleteSendedMessages(String fbid, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SEND_IT_LATER, KEY_TIME + " = " + time + " AND " + KEY_FBID + " = \"" + fbid + "\"", null);
        db.close();
    }

    public Cursor getMessagesQueue() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_SEND_IT_LATER, null, null, null, null, null, KEY_TIME + " ASC");
    }

    public void markError(int id, String text, String time, int error, int timeNew) {
        Log.i(TAG, "At time " + time);
        long timeLong = (Long) Long.parseLong(time) / 1000;
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_SENDED, error);
        if (timeNew != 0)
            values.put(KEY_TIME, timeNew);

        db.update("_" + id, values, KEY_TEXT + " = \"" + text + "\" AND " + KEY_TIME + " = \"" + timeLong + "\"", null);
        db.close();
        Log.i(TAG, "Error added " + error);

    }

    public void updatePhoto(String fbID, String photo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PHOTO, photo);
        db.update(TABLE_CONTACTS, values, KEY_FBID + " = " + fbID, null);
    }

    public Cursor getSearched(String search, List<String> online) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_CONTACTS, null, KEY_NAME + " LIKE \'%" + search + "%\'", null, null, null, KEY_NAME);
    }
}
