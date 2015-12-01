/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eztarget.realay.content_providers;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * Content Provider and database for storing the list of rooms nearby our current location
 */
public class RoomsContentProvider extends ContentProvider {

    private static final String LOG_TAG = RoomsContentProvider.class.getSimpleName();

    /**
     * The underlying database helper
     */
    private RoomsDatabaseHelper mHelper;

    private static final String TAG = "RoomsContentProvider";

    private static final String DATABASE_NAME = "realay_rooms.db";

    private static final int DATABASE_VERSION = 9;

    protected static final String ROOMS_TABLE = "rooms";

    public static final Uri CONTENT_URI =
            Uri.parse("content://org.eztarget.realay.provider.rooms/rooms");

    //Create the constants used to differentiate between the different URI requests.
    private static final int ROOM_LIST = 1;

    // Allocate the UriMatcher object.
    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI("org.eztarget.realay.provider.rooms", "rooms", ROOM_LIST);
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        mHelper = new RoomsDatabaseHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case ROOM_LIST:
                return "vnd.android.cursor.dir/vnd.eztarget.realay.rooms";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sort
    ) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(ROOMS_TABLE);

        // If no sort order is specified sort by date / time
        String orderBy;
        if (TextUtils.isEmpty(sort)) orderBy = BaseColumns.ROOM_DISTANCE + " ASC";
        else orderBy = sort;

        // Apply the query to the underlying database.
        Cursor queryCursor = queryBuilder.query(
                mHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                orderBy
        );

        // Register the contexts ContentResolver to be notified if
        // the cursor result set changes.
        queryCursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return a cursor to the query result.
        return queryCursor;
    }

    @Override
    public Uri insert(Uri _uri, ContentValues _initialValues) {
        // Insert the new row, will return the row number if successful.
        SQLiteDatabase db = mHelper.getWritableDatabase();
        long rowID = db.insert(ROOMS_TABLE, "nullhack", _initialValues);

        // Return a URI to the newly inserted row on success.
        if (rowID > 0) {
            Uri uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
//            getContext().getContentResolver().notifyChange(uri, null);
            return uri;
        }
        throw new SQLException("Failed to insert row startLoadingInto " + _uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        if (uriMatcher.match(uri) == ROOM_LIST) {
            SQLiteDatabase db = mHelper.getWritableDatabase();
            final int count = db.delete(ROOMS_TABLE, where, whereArgs);
//            getContext().getContentResolver().notifyChange(uri, null);
            return count;
        } else {
            throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        if (uriMatcher.match(uri) == ROOM_LIST) {
            SQLiteDatabase db = mHelper.getWritableDatabase();
            final int count = db.update(ROOMS_TABLE, values, where, whereArgs);
//            getContext().getContentResolver().notifyChange(uri, null);
            return count;
        } else {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    // Helper class for opening, creating, and managing database version control
    private static class RoomsDatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_CREATE =
                "CREATE TABLE " + ROOMS_TABLE + " ("
                        + android.provider.BaseColumns._ID + " LONG primary key, "
                        + BaseColumns.IMAGE_ID + " LONG,"
                        + BaseColumns.ROOM_TITLE + " TEXT,"
                        + BaseColumns.ROOM_DESCRIPTION + " TEXT,"
                        + BaseColumns.ROOM_CREATOR + " TEXT,"
                        + BaseColumns.ROOM_ADDRESS + " TEXT,"
                        + BaseColumns.ROOM_LAT + " REAL,"
                        + BaseColumns.ROOM_LNG + " REAL,"
                        + BaseColumns.ROOM_DISTANCE + " REAL,"
                        + BaseColumns.ROOM_RADIUS + " INT,"
                        + BaseColumns.ROOM_PASSWORD + " TEXT,"
                        + BaseColumns.ROOM_NUM_OF_USERS + " INT,"
                        + BaseColumns.ROOM_START_DATE + " LONG,"
                        + BaseColumns.ROOM_END_DATE + " LONG,"
                        + BaseColumns.LAST_UPDATE + " LONG);";

        public RoomsDatabaseHelper(
                Context context,
                String name,
                CursorFactory factory,
                int version
        ) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(
                    TAG,
                    "Upgrading database from version " + oldVersion + " to "
                            + newVersion + ", which will destroy all old data"
            );

            db.execSQL("DROP TABLE IF EXISTS " + ROOMS_TABLE);
            onCreate(db);
        }
    }

}