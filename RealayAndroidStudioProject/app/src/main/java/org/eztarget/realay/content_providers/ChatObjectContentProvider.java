package org.eztarget.realay.content_providers;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import org.eztarget.realay.data.Action;
import org.eztarget.realay.data.Conversation;
import org.eztarget.realay.data.User;

/**
 * Created by michel@eztarget.org on 29/11/14.
 *
 */
public class ChatObjectContentProvider extends ContentProvider {

    private static final String LOG_TAG = ChatObjectContentProvider.class.getSimpleName();

    /** Database file */
    protected static final String REALAY_MAIN_DB = "realay_main.db";

    /** The underlying database helper */
    private ChatObjectOpenHelper mHelper;

    /** The underlying database */
    private SQLiteDatabase mDatabase;

    public static final int DATABASE_VERSION = 30;

    /** Table name: Public messages */
    protected static final String TABLE_MSGS_PUB = "messages_public";

    /** Table name: Private messages */
    protected static final String TABLE_MSGS_PRV = "messages_private";

    /** Table name: Users */
    protected static final String TABLE_USERS = "users";

    /** Table name: Conversations */
    protected static final String TABLE_CONVS = "conversations";

    protected static final int URI_CODE_MSGS_PUB = 9;

    protected static final int URI_CODE_MSGS_PUB_JOIN_USER = 10;

    protected static final int URI_CODE_MSGS_PRV = 19;

    protected static final int URI_CODE_MSGS_PRV_JOIN_USERS = 20;

    protected static final int URI_CODE_MSG_PRV_ID = 21;

    protected static final int URI_CODE_USERS = 30;

    protected static final int URI_CODE_USER_ID = 31;

    protected static final int URI_CODE_CONVS = 40;

    protected static final UriMatcher URI_MATCHER;

    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(
                ChatObjectContract.AUTHORITY,
                "actions_public",
                URI_CODE_MSGS_PUB_JOIN_USER
        );
        URI_MATCHER.addURI(
                ChatObjectContract.AUTHORITY,
                "actions_private",
                URI_CODE_MSGS_PRV_JOIN_USERS
        );
        URI_MATCHER.addURI(
                ChatObjectContract.AUTHORITY,
                "actions_private/#",
                URI_CODE_MSG_PRV_ID
        );
        URI_MATCHER.addURI(
                ChatObjectContract.AUTHORITY,
                "users",
                URI_CODE_USERS
        );
        URI_MATCHER.addURI(
                ChatObjectContract.AUTHORITY,
                "users/#",
                URI_CODE_USER_ID
        );
        URI_MATCHER.addURI(
                ChatObjectContract.AUTHORITY,
                "conversations",
                URI_CODE_CONVS
        );
    }

    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case URI_CODE_MSGS_PUB:
            case URI_CODE_MSGS_PUB_JOIN_USER:
                return Action.CONTENT_TYPE;
            case URI_CODE_MSGS_PRV:
            case URI_CODE_MSGS_PRV_JOIN_USERS:
                return Action.CONTENT_TYPE;
            case URI_CODE_MSG_PRV_ID:
                return Action.CONTENT_ITEM_TYPE;
            case URI_CODE_USERS:
                return User.CONTENT_TYPE;
            case URI_CODE_USER_ID:
                return User.CONTENT_ITEM_TYPE;
            case URI_CODE_CONVS:
                return Conversation.CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public boolean onCreate() {
        mHelper = new ChatObjectOpenHelper(getContext(), REALAY_MAIN_DB, null, DATABASE_VERSION);
        return true;
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder
    ) {
        final String tables;
        switch (URI_MATCHER.match(uri)) {
            case URI_CODE_MSGS_PUB:
                tables = TABLE_MSGS_PUB;
                break;
            case URI_CODE_MSGS_PUB_JOIN_USER:
                tables = TABLE_MSGS_PUB + " INNER JOIN " + TABLE_USERS
                        + " ON " + TABLE_MSGS_PUB + "." + BaseColumns.MSG_SENDER
                        + "=" + TABLE_USERS + "." + BaseColumns._ID;
                break;
            case URI_CODE_MSGS_PRV:
                tables = TABLE_MSGS_PRV;
                break;
            case URI_CODE_MSGS_PRV_JOIN_USERS:
                tables = TABLE_MSGS_PRV + " INNER JOIN " + TABLE_USERS
                        + " ON " + TABLE_MSGS_PRV + "." + BaseColumns.MSG_SENDER
                        + "=" + TABLE_USERS + "." + android.provider.BaseColumns._ID;
                break;
            case URI_CODE_USERS:
                tables = TABLE_USERS;
                break;
            case URI_CODE_CONVS:
                tables = TABLE_CONVS + " INNER JOIN " + TABLE_MSGS_PRV
                        + " ON " + TABLE_CONVS + "." + BaseColumns.LAST_MSG_ID
                        + "=" + TABLE_MSGS_PRV + "." + android.provider.BaseColumns._ID
                        + " INNER JOIN " + TABLE_USERS
                        + " ON " + TABLE_CONVS + "." + BaseColumns.PARTNER_ID
                        + "=" + TABLE_USERS + "." + android.provider.BaseColumns._ID;
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(tables);

        // Return the cursor to the query result.
        SQLiteDatabase db = getWritableDatabase();
        Cursor c;
        c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        // Register the contexts ContentResolver to be notified if the cursor result set changes.
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = getWritableDatabase();

        int count;
        switch (URI_MATCHER.match(uri)) {
            case URI_CODE_MSGS_PUB_JOIN_USER:
                count = db.delete(TABLE_MSGS_PUB, where, whereArgs);
                break;
            case URI_CODE_MSGS_PRV_JOIN_USERS:
            case URI_CODE_MSG_PRV_ID:
                count = db.delete(TABLE_MSGS_PRV, where, whereArgs);
                break;
            case URI_CODE_USERS:
            case URI_CODE_USER_ID:
                count = db.delete(TABLE_USERS, where, whereArgs);
                break;
            case URI_CODE_CONVS:
                count = db.delete(TABLE_CONVS, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // Inserting the new row, will return the row number if successful.
        long rowId;
        Uri contentUri, newUri;
        String table;

        switch (URI_MATCHER.match(uri)) {
            case URI_CODE_MSGS_PUB_JOIN_USER:
                table = TABLE_MSGS_PUB;
                contentUri = ChatObjectContract.CONTENT_URI_ACTIONS_PUBLIC;
                break;
            case URI_CODE_MSGS_PRV_JOIN_USERS:
            case URI_CODE_MSG_PRV_ID:
                table = TABLE_MSGS_PRV;
                contentUri = ChatObjectContract.CONTENT_URI_ACTIONS_PRIVATE;
                break;
            case URI_CODE_USERS:
            case URI_CODE_USER_ID:
                table = TABLE_USERS;
                contentUri = ChatObjectContract.CONTENT_URI_USERS;
                break;
            case URI_CODE_CONVS:
                table = TABLE_CONVS;
                contentUri = ChatObjectContract.CONTENT_URI_CONVS;
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        rowId = getWritableDatabase().insert(table, "nullhack", values);
        if (rowId < 0) return null;
        newUri = ContentUris.withAppendedId(contentUri, rowId);
        getContext().getContentResolver().notifyChange(newUri, null);
        return newUri;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = getWritableDatabase();
        
        int count;
        switch (URI_MATCHER.match(uri)) {
            case URI_CODE_MSGS_PUB_JOIN_USER:
                count = db.update(TABLE_MSGS_PUB, values, where, whereArgs);
                break;
            case URI_CODE_MSGS_PRV_JOIN_USERS:
            case URI_CODE_MSG_PRV_ID:
                count = db.update(TABLE_MSGS_PRV, values, where, whereArgs);
                break;
            case URI_CODE_USERS:
                count = db.update(TABLE_USERS, values, where, whereArgs);
                break;
            case URI_CODE_CONVS:
                count = db.update(TABLE_CONVS, values, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    private SQLiteDatabase getWritableDatabase() {
        if (mDatabase == null) mDatabase = mHelper.getWritableDatabase();
        return mDatabase;
    }
}
