package com.example.jhbra.android_project;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

public class DBProvider extends ContentProvider {
    static final Uri CONTENT_URI = Uri.parse("content://moapp1.gps.calendar/schedule");
    static final int GET_ALL = 1;
    static final int GET_ONE = 2;

    public static final String SQL_CREATE_DEFAULT = "CREATE TABLE IF NOT EXISTS schedule "
            + "(_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
            + "title TEXT, time INTEGER, longitude REAL, latitude REAL, transport TEXT, "
            + "memo TEXT);";

    static final UriMatcher matcher;

    static {
        matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI("moapp1.gps.calendar", "schedule", GET_ALL);
        matcher.addURI("moapp1.gps.calendar", "schedule/#", GET_ONE);
    }

    SQLiteDatabase mDB;

    static class DBHelper extends SQLiteOpenHelper {
        public DBHelper(Context context) {
            super(context, "test.db", null, 1);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_DEFAULT);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
            db.execSQL("DROP TABLE IF EXISTS schedule;");
            onCreate(db);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int cnt = 0;

        switch (matcher.match(uri)) {
            case GET_ALL:
                cnt = mDB.delete("schedule", selection, selectionArgs);
                break;
            case GET_ONE:
                String where = "_id = '" + uri.getPathSegments().get(1) + "'";
                if (TextUtils.isEmpty(selection) == false) {
                    where += " AND " + selection;
                }
                cnt = mDB.delete("schedule", where, selectionArgs);
                break;
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return cnt;
    }

    @Override
    public String getType(Uri uri) {
        switch (matcher.match(uri)) {
            case GET_ALL:
                return "vnd.android.cursor.dir/vnd.vr.schedule";
            case GET_ONE:
                return "vnd.android.cursor.item/vnd.vr.schedule";
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long row = mDB.insert("schedule", null, values);
        if (row > 0) {
            Uri notiURI = ContentUris.withAppendedId(CONTENT_URI, row);
            getContext().getContentResolver().notifyChange(notiURI, null);
            return notiURI;
        }
        return null;
    }

    @Override
    public boolean onCreate() {
        DBHelper dbHelper = new DBHelper(getContext());
        mDB = dbHelper.getWritableDatabase();
        return mDB != null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        String base = "SELECT * FROM schedule";
        String where = "";

        if (matcher.match(uri) == GET_ONE) {
            where += "_id='" + uri.getPathSegments().get(1) + "'";
        }
        if (TextUtils.isEmpty(selection) == false) {
            if (!where.isEmpty()) {
                where += " AND ";
            }
            where += selection;
        }

        String sql = base;
        if (!where.isEmpty()) {
            sql += " WHERE ";
            sql += where;
        }
        sql += ';';

        Cursor cursor = mDB.rawQuery(sql, null);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int cnt = 0;

        switch (matcher.match(uri)) {
            case GET_ALL:
                cnt = mDB.update("schedule", values, selection, selectionArgs);
                break;
            case GET_ONE:
                String where = "_id = '" + uri.getPathSegments().get(1) + "'";
                if (TextUtils.isEmpty(selection) == false) {
                    where += " AND " + selection;
                }
                cnt = mDB.update("schedule", values, where, selectionArgs);
                break;
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return cnt;
    }
}
