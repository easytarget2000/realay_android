package org.eztarget.realay.content_providers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by michel on 30/12/14.
 *
 */
public class ChatObjectOpenHelper extends SQLiteOpenHelper {

    private static final String LOG_TAG = ChatObjectOpenHelper.class.getSimpleName();

    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE " + ChatObjectContentProvider.TABLE_USERS + " ("
                    + android.provider.BaseColumns._ID
                    + " LONG primary key, "
                    + BaseColumns.USER_IS_BLOCKED + " INTEGER, "
                    + BaseColumns.IMAGE_ID + " LONG,"
                    + BaseColumns.USER_NAME + " TEXT,"
                    + BaseColumns.USER_STATUS + " TEXT,"
                    + BaseColumns.USER_MAIL + " TEXT,"
                    + BaseColumns.USER_PHONE + " TEXT,"
                    + BaseColumns.USER_WEBSITE + " TEXT,"
                    + BaseColumns.USER_IG + " TEXT,"
                    + BaseColumns.USER_FB + " TEXT,"
                    + BaseColumns.USER_TWITTER + " TEXT);";

    private static final String INSERT_ADMIN_USER =
            "INSERT INTO " + ChatObjectContentProvider.TABLE_USERS
                    + " (" + android.provider.BaseColumns._ID + ","
                    + BaseColumns.IMAGE_ID + ","
                    + BaseColumns.USER_NAME + ")"
                    + " VALUES (-10, -10, \"Admin\");";

    private static final String CREATE_TABLE_MSGS_PUB =
            "CREATE TABLE " + ChatObjectContentProvider.TABLE_MSGS_PUB + " ("
                    + android.provider.BaseColumns._ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + BaseColumns.ROOM_ID + " LONG, "
                    + BaseColumns.MSG_SENDER + " LONG, "
                    + BaseColumns.MSG_TIME + " LONG, "
                    + BaseColumns.MSG_CODE + " INTEGER, "
                    + BaseColumns.MSG_CONTENT + " TEXT, "
                    + BaseColumns.MSG_IN_QUEUE + " INTEGER);";

    private static final String CREATE_TABLE_MSGS_PRV =
            "CREATE TABLE " + ChatObjectContentProvider.TABLE_MSGS_PRV + " ("
                    + android.provider.BaseColumns._ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + BaseColumns.ROOM_ID + " LONG, "
                    + BaseColumns.MSG_SENDER + " LONG, "
                    + BaseColumns.MSG_RECIPIENT + " LONG, "
                    + BaseColumns.MSG_TIME + " LONG, "
                    + BaseColumns.MSG_CODE + " INTEGER, "
                    + BaseColumns.MSG_CONTENT + " TEXT, "
                    + BaseColumns.MSG_IN_QUEUE + " INTEGER);";

    private static final String CREATE_TABLE_CONVS =
            "CREATE TABLE " + ChatObjectContentProvider.TABLE_CONVS + " ("
                    + BaseColumns.ROOM_ID + " LONG NOT NULL,"
                    + BaseColumns.PARTNER_ID + " LONG NOT NULL,"
                    + BaseColumns.LAST_MSG_ID + " LONG,"
                    + " PRIMARY KEY ("
                    + BaseColumns.ROOM_ID + "," + BaseColumns.PARTNER_ID
                    + "));";

    public ChatObjectOpenHelper(
            Context context,
            String name,
            SQLiteDatabase.CursorFactory factory,
            int version
    ) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(INSERT_ADMIN_USER);
        db.execSQL(CREATE_TABLE_MSGS_PUB);
        db.execSQL(CREATE_TABLE_MSGS_PRV);
        db.execSQL(CREATE_TABLE_CONVS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(LOG_TAG, "Table version upgrade: " + oldVersion + " to " + newVersion);

        db.execSQL("DROP TABLE IF EXISTS " + ChatObjectContentProvider.TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + ChatObjectContentProvider.TABLE_MSGS_PUB);
        db.execSQL("DROP TABLE IF EXISTS " + ChatObjectContentProvider.TABLE_MSGS_PRV);
        db.execSQL("DROP TABLE IF EXISTS " + ChatObjectContentProvider.TABLE_CONVS);
        onCreate(db);
    }



}
