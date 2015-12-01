package org.eztarget.realay.content_providers;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.eztarget.realay.data.Room;

/**
 * Created by michel on 14/02/15.
 */
public class RoomsContract {

    private static final String TAG = RoomsContract.class.getSimpleName();

    public static final String SELECTION_ROOM_BY_ID =
            RoomsContentProvider.ROOMS_TABLE + "." + BaseColumns._ID + "=?";

    public static Room buildRoom(Cursor cursor) {
        try {
            final int idIndex =
                    cursor.getColumnIndexOrThrow(android.provider.BaseColumns._ID);
            final int imageIdIndex =
                    cursor.getColumnIndexOrThrow(BaseColumns.IMAGE_ID);
            final int titleIndex =
                    cursor.getColumnIndexOrThrow(BaseColumns.ROOM_TITLE);
            final int descriptionIndex =
                    cursor.getColumnIndexOrThrow(BaseColumns.ROOM_DESCRIPTION);
            final int creatorIndex =
                    cursor.getColumnIndexOrThrow(BaseColumns.ROOM_CREATOR);
            final int addressIndex =
                    cursor.getColumnIndexOrThrow(BaseColumns.ROOM_ADDRESS);
            final int latIndex =
                    cursor.getColumnIndexOrThrow(BaseColumns.ROOM_LAT);
            final int lngIndex =
                    cursor.getColumnIndexOrThrow(BaseColumns.ROOM_LNG);
            final int distanceIndex =
                    cursor.getColumnIndexOrThrow(BaseColumns.ROOM_DISTANCE);
            final int radiusIndex =
                    cursor.getColumnIndexOrThrow(BaseColumns.ROOM_RADIUS);
            final int fPasswordIndex =
                    cursor.getColumnIndexOrThrow(BaseColumns.ROOM_PASSWORD);
            final int fNumOfUsersIndex =
                    cursor.getColumnIndexOrThrow(BaseColumns.ROOM_NUM_OF_USERS);
            final int startDateIndex =
                    cursor.getColumnIndexOrThrow(BaseColumns.ROOM_START_DATE);
            final int endDateIndex =
                    cursor.getColumnIndexOrThrow(BaseColumns.ROOM_END_DATE);

            return (
                    new Room(
                            cursor.getLong(idIndex),
                            cursor.getLong(imageIdIndex),
                            cursor.getString(titleIndex),
                            cursor.getString(descriptionIndex),
                            cursor.getString(creatorIndex),
                            cursor.getString(addressIndex),
                            cursor.getFloat(latIndex),
                            cursor.getFloat(lngIndex),
                            cursor.getInt(distanceIndex),
                            cursor.getInt(radiusIndex),
                            cursor.getString(fPasswordIndex),
                            cursor.getInt(fNumOfUsersIndex),
                            cursor.getLong(startDateIndex),
                            cursor.getLong(endDateIndex)
                    )
            );
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            return null;
        }
    }

    /**
     * Adds a Room to the RoomsContentProvider using the values inside the object.
     */
    public static boolean insertRoom(final Context context, final Room room) {
        if (context == null || room == null) return false;

        final long roomId = room.getId();


        // Construct the Content Values.
        ContentValues values = new ContentValues();
        values.put(BaseColumns._ID, roomId);
        values.put(BaseColumns.IMAGE_ID, room.getImageId());
        values.put(BaseColumns.ROOM_TITLE, room.getTitle());
        values.put(BaseColumns.ROOM_DESCRIPTION, room.getDescription());
        values.put(BaseColumns.ROOM_CREATOR, room.getCreator());
        values.put(BaseColumns.ROOM_ADDRESS, room.getAddress());
        values.put(BaseColumns.ROOM_LAT, room.getLatLng().latitude);
        values.put(BaseColumns.ROOM_LNG, room.getLatLng().longitude);
        values.put(BaseColumns.ROOM_DISTANCE, room.getDistance());
        values.put(BaseColumns.ROOM_RADIUS, room.getRadius());
        values.put(BaseColumns.ROOM_PASSWORD, room.getPassword());
        values.put(BaseColumns.ROOM_NUM_OF_USERS, room.getNumOfUsers());
        values.put(BaseColumns.ROOM_START_DATE, room.getStartDateSec());
        values.put(BaseColumns.ROOM_END_DATE, room.getEndDateSec());
        values.put(BaseColumns.LAST_UPDATE, System.currentTimeMillis());

        // Update or insert the new place to the PlacesContentProvider.
        final ContentResolver resolver = context.getContentResolver();

        // If the update returned 0, try an insert.
        final String where = android.provider.BaseColumns._ID + "=" + roomId;
        final int updated = resolver.update(RoomsContentProvider.CONTENT_URI, values, where, null);
        return updated > 0 || resolver.insert(RoomsContentProvider.CONTENT_URI, values) != null;
    }

    public static void notifyChange(final Context context) {
        context.getContentResolver().notifyChange(RoomsContentProvider.CONTENT_URI, null);
    }

    public static void deleteRoom(Context context, final long roomId) {
        context.getContentResolver().delete(
                RoomsContentProvider.CONTENT_URI,
                android.provider.BaseColumns._ID + "=" + roomId,
                null
        );
    }
}
