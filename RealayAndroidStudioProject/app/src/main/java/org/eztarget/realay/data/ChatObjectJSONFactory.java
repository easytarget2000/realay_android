package org.eztarget.realay.data;

import android.content.Context;
import android.support.annotation.NonNull;

import org.eztarget.realay.managers.LocationWatcher;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by michel on 12/12/14.
 */
public class ChatObjectJSONFactory {

    private static final String TAG_ADDRESS = "ad";

    private static final String TAG_CODE = "cd";

//    private static final String TAG_COLOR = "cl";

    private static final String TAG_CREATOR = "cr";

    private static final String TAG_DESCRIPTION = "ds";

    private static final String TAG_DISTANCE = "dst";

    private static final String TAG_EMAIL = "em";

    private static final String TAG_END_DATE = "et";

    private static final String TAG_FB = "fb";

    private static final String TAG_IMAGE_ID = "i";

    private static final String TAG_IG = "ig";

    private static final String TAG_LAT = "lat";

    private static final String TAG_LNG = "lng";

    private static final String TAG_MESSAGE = "m";

    private static final String TAG_NAME = "n";

    private static final String TAG_NUM_OF_USERS = "ucn";

    private static final String TAG_PASSWORD = "pw";

    private static final String TAG_PHONE = "ph";

    private static final String TAG_RADIUS = "rd";

    private static final String TAG_RECIPIENT_ID = "rc";

    private static final String TAG_ROOM_ID = "r";

    private static final String TAG_SENDER_ID = "sn";

    private static final String TAG_START_DATE = "st";

    private static final String TAG_STATUS = "s";

    private static final String TAG_TIME = "t";

    private static final String TAG_TITLE = "tt";

    private static final String TAG_TWITTER = "tw";

    private static final String TAG_USER_ID = "u";

    private static final String TAG_WEBSITE = "ws";

    public static Action buildAction(@NonNull final JSONObject jsonAction) throws JSONException {
        return new Action(
                -22,
                jsonAction.getLong(TAG_ROOM_ID),
                jsonAction.getLong(TAG_SENDER_ID),
                jsonAction.getLong(TAG_RECIPIENT_ID),
                jsonAction.getInt(TAG_TIME),
                jsonAction.getInt(TAG_CODE),
                jsonAction.isNull(TAG_MESSAGE) ? null : jsonAction.getString(TAG_MESSAGE)
        );
    }

    public static Room buildRoom(final Context context, final JSONObject jsonRoom)
            throws JSONException {

        final Room room = new Room(
                jsonRoom.getLong(TAG_ROOM_ID),
                jsonRoom.getLong(TAG_IMAGE_ID),
                jsonRoom.getString(TAG_TITLE),
                jsonRoom.getString(TAG_DESCRIPTION),
                jsonRoom.getString(TAG_CREATOR),
                jsonRoom.isNull(TAG_ADDRESS) ? null : jsonRoom.getString(TAG_ADDRESS),
                jsonRoom.getDouble(TAG_LAT),
                jsonRoom.getDouble(TAG_LNG),
                778866,
                jsonRoom.getInt(TAG_RADIUS),
                jsonRoom.getString(TAG_PASSWORD),
                jsonRoom.getInt(TAG_NUM_OF_USERS),
                jsonRoom.getLong(TAG_START_DATE),
                jsonRoom.getLong(TAG_END_DATE)
        );

        final int distance;
        if (LocationWatcher.from(context).getLocation(false) != null) {
            distance = LocationWatcher.from(context).getUpdatedDistanceToRoom(room);
        } else {
            distance = (int) (jsonRoom.getDouble(TAG_DISTANCE) * 1000.0);
        }
        room.setDistance(distance);
        return room;
    }

    public static User buildUser(final JSONObject jsonUser) throws JSONException {
        if (jsonUser == null) return null;
        return new User(
                jsonUser.getLong(TAG_USER_ID),
                jsonUser.getLong(TAG_IMAGE_ID),
                jsonUser.getString(TAG_NAME),
                jsonUser.isNull(TAG_STATUS) ? "." : jsonUser.getString(TAG_STATUS),
                jsonUser.isNull(TAG_EMAIL) ? null : jsonUser.getString(TAG_EMAIL),
                jsonUser.isNull(TAG_PHONE) ? null : jsonUser.getString(TAG_PHONE),
                jsonUser.isNull(TAG_WEBSITE) ? null : jsonUser.getString(TAG_WEBSITE),
                jsonUser.isNull(TAG_IG) ? null : jsonUser.getString(TAG_IG),
                jsonUser.isNull(TAG_FB) ? null : jsonUser.getString(TAG_FB),
                jsonUser.isNull(TAG_TWITTER) ? null : jsonUser.getString(TAG_TWITTER)
        );
    }
}
