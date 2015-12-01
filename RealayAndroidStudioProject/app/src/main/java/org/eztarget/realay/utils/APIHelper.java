package org.eztarget.realay.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import org.eztarget.realay.Constants;
import org.eztarget.realay.PreferenceHelper;
import org.eztarget.realay.R;
import org.eztarget.realay.content_providers.ChatObjectContract;
import org.eztarget.realay.content_providers.RoomsContract;
import org.eztarget.realay.data.Action;
import org.eztarget.realay.data.ChatObjectJSONFactory;
import org.eztarget.realay.data.Room;
import org.eztarget.realay.data.User;
import org.eztarget.realay.managers.Bouncer;
import org.eztarget.realay.managers.LocalUserManager;
import org.eztarget.realay.managers.LocationWatcher;
import org.eztarget.realay.managers.SessionActionsManager;
import org.eztarget.realay.managers.SessionMainManager;
import org.eztarget.realay.managers.UsersCache;
import org.eztarget.realay.ui.utils.ImageLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;

/**
 * Created by michel on 18/11/14.
 *
 */
public class APIHelper {

    private static final String TAG = APIHelper.class.getSimpleName();

    protected static final String STATUS_TAG = "st";

    /**
     * The default search radius in km when searching for rooms nearby.
     */
    public static final int SEARCH_RADIUS = 20;

    private static final String API_BASE_URL = "";

    private static final String SENDER_PARAM = "sender";

    private static final String SESSION_PARAM = "session";

    private static final String USER_PARAM = "user";

    private static final String DO_JOIN_ROOM_CALL = "do_join_room";

    private static final String DO_JOIN_ROOM_SUCCESS_STATUS = "IUJ_YES";

    private static final String GET_ACTIONS_CALL = "get_actions";

    private static final String INITAL_QUERY_PARAM = "initial";

    private static final String LAST_ACTION_ID_PARAM = "last";

    private static final String BLOCKED_IDS_PARAM = "blocked";

    private static final String PARAM_PING = "ping";

    private static final String GET_ACTIONS_SUCCESS_STATUS = "AS_YES";

    private static final String ACTIONS_ARRAY_TAG = "as";

    private static final String GET_LAST_ACTION_ID_CALL = "get_last_action_id";

    private static final String GET_LAST_ACTION_SUCCESS_STATUS = "GLA_YES";

    private static final String ACTION_ID_VALUE_TAG = "a";

    private static final String PUT_ACTION_CALL = "put_action";

    private static final String RECIPIENT_PARAM = "recipient";

    private static final String CODE_PARAM = "code";

    private static final String TIME_PARAM = "time";

    private static final String MESSAGE_PARAM = "message";

    private static final String PUT_ACTION_SUCCESS_STATUS = "INA_YES";

    private static final String PUT_MESSAGE_SUCCESS_STATUS = "INM_YES";

    private static final String GET_IMAGE_CALL = "get_image";

    private static final String FILE_NAME_PARAM = "f";

    private static final String PUT_IMAGE_CALL = "put_image";

    private static final String HI_RES_FILE_PARAM = "userfile";

    private static final String LO_RES_FILE_PARAM = "userfile_s";

    private static final String PUT_USER_IMAGE_SUCCESS_STATUS = "IIU_YES";

    private static final String PUT_IMAGE_MESSAGE_SUCCESS_STATUS = "IIM_YES";

    private static final String IMAGE_ID_TAG = "i";

    private static final String GET_ROOMS_CALL = "get_rooms";

    private static final String LATITUDE_PARAM = "lat";

    private static final String LONGITUDE_PARAM = "lng";

    private static final String DISTANCE_PARAM = "dist";

    private static final String GET_ROOMS_SUCCESS_STATUS = "RS_YES";

    private static final String ROOMS_ARRAY_TAG = "rs";

    private static final String GET_USER_CALL = "get_user";

    private static final String GET_USER_SUCCESS_STATUS = "SU_YES";

    private static final String USER_OBJECT_TAG = "user";

    private static final String GET_LOCAL_USER_CALL = "get_local_user";

    private static final String DEVICE_ID_PARAM = "device_id";

    private static final String USER_NAME_PARAM = "name";

    private static final String USER_STATUS_PARAM = "status";

    private static final String GET_SESSION_USERS_CALL = "get_session_users";

    private static final String GET_SESSION_USERS_SUCCESS_STATUS = "UIR_YES";

    private static final String USERS_ARRAY_TAG = "us";

    private static final String DO_UPDATE_USER_CALL = "do_update_user";

    private static final String EMAIL_PARAM = "email";

    private static final String PHONE_PARAM = "phone";

    private static final String WEBSITE_PARAM = "website";

    private static final String FB_PARAM = "fb";

    private static final String IG_PARAM = "ig";

    private static final String TWITTER_PARAM = "twitter";

    private static final String UPDATE_USER_SUCCESS_STATUS = "UU_YES";

    private static final String KICK_USER_CALL = "do_request";

    private static final String BAN_USER_PARAM = "perm";

    private static final String KICK_USER_SUCCESS_STATUS = "KU_YES";

    private static final long MAX_REQUEST_MILLIS = 20L * 1000L;

    private static HashMap<String, Long> sCurrentRequests;

    /*
    Actions
     */

    public static boolean doJoinRoom(Context context) {
        if (context == null) return false;

        if (isPerformingRequest(DO_JOIN_ROOM_CALL)) return false;

        HashMap<String, String> parameter = buildSessionParameterMap(context);
        if (parameter == null) return false;
        parameter.put(DEVICE_ID_PARAM, PreferenceHelper.from(context).getAuthId());

        // Make a call to the script that inserts the user into the database.
        // This returns a status code and the ID of the new user.
        final boolean didSucceed = URLConnectionHelper.performCall(
                context,
                DO_JOIN_ROOM_CALL,
                parameter,
                DO_JOIN_ROOM_SUCCESS_STATUS
        );

        didFinishRequest(DO_JOIN_ROOM_CALL);

        return didSucceed;
    }

    /**
     * Description from server script that will be called:
     * Selects all actions that are older than the given last action ID,
     * happened in a certain room, were NOT sent BY me, are not private messages
     * or are private messages specifically FOR ME;
     *
     * @return The number of received Actions or an error code < 0
     */
    public static int getActions(Context context) {
        // Ask the Managers which User ID is querying data from which Room.
        final long roomId = SessionMainManager.getInstance().getRoomId(context);
        final long localUserId = LocalUserManager.getInstance().getUserId(context);

        if (context == null || roomId < 10L || localUserId < 10L) return -1;

        final SessionActionsManager actionMan = SessionActionsManager.from(context);
        final boolean isFirstCall = !actionMan.didFirstQuery();

        final HashMap<String, String> parameter = new HashMap<>();
        if (isFirstCall) {
            // Initial query: only get public messages from the near past
            parameter.put(INITAL_QUERY_PARAM, "1");
        } else {
            // Last received Action ID:
            final long lastActionId = actionMan.getLastReceivedActionId();
            parameter.put(LAST_ACTION_ID_PARAM, String.valueOf(lastActionId));
        }
        parameter.put(SESSION_PARAM, String.valueOf(roomId));
        parameter.put(SENDER_PARAM, String.valueOf(localUserId));

        // Blocked User IDs:
        final Long[] blockedUsers = UsersCache.getInstance().getBlockedUsers(context);
        if (blockedUsers.length > 0) {
            String blockedUsersString = "";
            for (Long l : blockedUsers) {
                blockedUsersString += l + ",";
            }
            blockedUsersString += "-1";
            parameter.put(BLOCKED_IDS_PARAM, blockedUsersString);
        }

        // After a certain amount of time, update the activity timestamp of this User on the db.
        final boolean doSendPing = actionMan.doSendPing();
        if (doSendPing) parameter.put(PARAM_PING, "1");

        final JSONArray jsonActions = URLConnectionHelper.getJsonArray(
                context,
                GET_ACTIONS_CALL,
                parameter,
                GET_ACTIONS_SUCCESS_STATUS,
                ACTIONS_ARRAY_TAG
        );

        if (jsonActions == null) {
            actionMan.increaseHeartbeatInterval();
            return -2;
        }

        if (doSendPing) actionMan.acknowledgePing();
        actionMan.acknowledgeQuery();
//        if (jsonActions.length() < 0) {
//            actionMan.increaseHeartbeatInterval(context);
//            return -3;
//        }

        boolean doShowPublicMsgNotification = false;
        boolean doShowPrivateMsgNotification = false;
        int i;
        for (i = 0; i < jsonActions.length(); i++) {
            Action a = null;
            long actionIdExternal = -9L;
            try {
                JSONObject receivedAction = jsonActions.getJSONObject(i);
                a = ChatObjectJSONFactory.buildAction(receivedAction);
                actionIdExternal = receivedAction.getLong(ACTION_ID_VALUE_TAG);
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
            }

            // Process the new action.
            if (a != null) {
                // Save the highest ID of the new Actions.
                actionMan.setLastReceivedAction(actionIdExternal);
                handleAction(context, a, isFirstCall);
                if (a.isMessage()) {
                    if (a.isPublic()) doShowPublicMsgNotification = true;
                    else doShowPrivateMsgNotification = true;
                }
            }
        }
        if (i > 0) {
            if (!isFirstCall) {
                if (doShowPrivateMsgNotification) actionMan.showNotifications(true);
                if (doShowPublicMsgNotification) actionMan.showNotifications(false);
            }

            context.getContentResolver().notifyChange(
                    ChatObjectContract.CONTENT_URI_ACTIONS_PUBLIC, null
            );
            context.getContentResolver().notifyChange(
                    ChatObjectContract.CONTENT_URI_ACTIONS_PRIVATE, null
            );
            actionMan.resetHeartbeatInterval();
        } else {
            actionMan.increaseHeartbeatInterval();
        }

        // The initial query ignores Actions that are not messages,
        // so the true last Action ID is possibly higher than what was received.
        if (isFirstCall) {
            final long lastActionId = URLConnectionHelper.getLong(
                    context,
                    GET_LAST_ACTION_ID_CALL,
                    parameter,
                    GET_LAST_ACTION_SUCCESS_STATUS,
                    ACTION_ID_VALUE_TAG
            );
            if (lastActionId > 100L) actionMan.setLastReceivedAction(lastActionId);
        }

        return i;
    }

    private static void handleAction(
            Context context,
            Action action,
            final boolean isFirstCall
    ) {
        if (context == null || action == null) return;

        // Download and store User data if it is not cached yet.
        UsersCache usersCache = UsersCache.getInstance();
        final long senderId = action.getSenderUserId();
        final long localUserId = LocalUserManager.getInstance().getUserId(context);
        final boolean isOutgoing = senderId == localUserId;
        if (!isOutgoing) usersCache.downloadIfNeeded(context, senderId);

        final long recipientId = action.getRecipientUserId();
        if (recipientId == Action.PUBLIC_RECIPIENT_ID || recipientId == localUserId) {
            usersCache.downloadIfNeeded(context, recipientId);
        }
//        else {
//            Log.d(TAG, "Processing Action " + action.getId() + " : " + recipientId);
//        }

        switch (action.getCode()) {
            case Action.ACTION_CODE_PHOTO_PUB:
            case Action.ACTION_CODE_PHOTO_PRV:
                ImageLoader.with(context).handle(action, false).startLoading();

            case Action.ACTION_CODE_MSG_PUB:
            case Action.ACTION_CODE_MSG_PRV:

                // This Action is a private or public chat message.
                // Add it to the local database and create a Notification.

                // Receiving a Message from this User, implies that the User is here.
                // Initial queries are ignored here because they possibly carry old messages.
                if (!isFirstCall) SessionMainManager.getInstance().addUserId(senderId);

                ChatObjectContract.insertAction(context, action, true);
                SessionActionsManager.from(context).addToNotificationQuery(action);
                return;

            case Action.ACTION_CODE_KICK:
                // A User was kicked.
                // As long as Messages and Actions are mixed, we receive ALL kicks, bans, etc.
                // Check if this kick action was addressed at the local user ID.
                if (!isFirstCall && recipientId == localUserId) {
                    Bouncer.from(context).kick(Bouncer.REASON_RECEIVED_KICK, TAG);
                }
                return;

            case Action.ACTION_CODE_BAN:
                // A user was banned. Check if this was sent to the local User.
                if (!isFirstCall && recipientId == localUserId) {
                    Bouncer.from(context).kick(Bouncer.REASON_RECEIVED_KICK, TAG);
                    RoomsContract.deleteRoom(context, action.getRoomId());
                    Log.i(TAG, "BAN " + action.getSenderUserId());
                }
                return;

            case Action.ACTION_CODE_JOIN:
                // This Action says that a User has joined the Room.
                // If the User is not the local User, add the ID to the Session list,
                // notify a list change and query the User details if necessary.
                if (!isOutgoing) {
                    UsersCache.getInstance().downloadIfNeeded(context, senderId);
                    final boolean didChangeList = SessionMainManager.getInstance().addUserId(senderId);
                    if (!isFirstCall && didChangeList) sendUserListBroadcast(context);
//                    Log.d(TAG, "User " + senderId + " has joined.");
                }
                return;

            case Action.ACTION_CODE_QUIT:
                // This Action says that a User has left the Room.
                // If the User is not the local User, remove the ID from the Session list
                // and notify a list change.
                if (!isFirstCall && !isOutgoing) {
                    SessionMainManager.getInstance().removeUser(senderId);
                    sendUserListBroadcast(context);
//                    Log.d(TAG, "User  " + senderId + " has left.");
                }
                return;

            case Action.ACTION_CODE_UPD_USER:
                // This Action says that a User has updated information or a new picture.
                // If the User is not the local User, query the new data.
                if (!isFirstCall && !isOutgoing) {
//                    Log.d(TAG, "Updating User data: " + senderId);
                    UsersCache.getInstance().startDownloadService(context, senderId);
                }
        }
    }

    public static boolean putAction(Context context, final Action action) {
        if (context == null || action == null) return false;

        // Additional preparation tasks that have to be done with this kind of Action:
        final int code = action.getCode();

        // Since this may be called by the Action Queue, certain Actions require specific API calls.
        if (code == Action.ACTION_CODE_UPD_USER) return doUpdateLocalUser(context);


        final long roomId = action.getRoomId();
        if (roomId < 10L) {
            return true;
        }

        HashMap<String, String> parameter = new HashMap<>();
        parameter.put(SESSION_PARAM, String.valueOf(roomId));
        final long senderId = action.getSenderUserId();
        parameter.put(SENDER_PARAM, String.valueOf(senderId));
        final long recipientId = action.getRecipientUserId();
        parameter.put(RECIPIENT_PARAM, String.valueOf(recipientId));
        final long timestamp = action.getTimeStampSec();
        parameter.put(TIME_PARAM, String.valueOf(timestamp));
        parameter.put(CODE_PARAM, String.valueOf(code));

        // Define the status which the script should return depending on the Action type.
        final String jsonStatusOk;
        final String message = action.getMessage();
        if (code == Action.ACTION_CODE_MSG_PRV || code == Action.ACTION_CODE_MSG_PUB) {
            if (TextUtils.isEmpty(message)) {
                Log.e(TAG, "Empty message.");
                return false;
            }
            parameter.put(MESSAGE_PARAM, message);
            jsonStatusOk = PUT_MESSAGE_SUCCESS_STATUS;
        } else {
            jsonStatusOk = PUT_ACTION_SUCCESS_STATUS;
        }

        return URLConnectionHelper.performCall(context, PUT_ACTION_CALL, parameter, jsonStatusOk);
    }

    public static void kickUser(final Context context, final long userId, final boolean doBan) {
//        if (SessionMainManager.getInstance().isAdmin()) {
//
//            HashMap<String, String> parameter = buildSessionParameterMap(context);
//            parameter.put(RECIPIENT_PARAM, String.valueOf(userId));
//            if (doBan) {
//                parameter.put(BAN_USER_PARAM, "1");
//            }
//            URLConnectionHelper.performCall(
//                    context,
//                    KICK_USER_CALL,
//                    parameter,
//                    KICK_USER_SUCCESS_STATUS
//            );
//        }
    }

    /**
     * Broadcasts an Intent with the Action Constants.ACTION_USER_LIST_CHANGED
     * to notify Activities that display the User and Conversation lists
     */
    private static void sendUserListBroadcast(final Context context) {
        if (context != null) {
            Intent broadcast = new Intent(Constants.ACTION_USER_LIST_CHANGED);
            context.sendBroadcast(broadcast);
        }
    }

    /*
    Images
     */

    public static Bitmap getImage(Context context, final String fileName, int numberOfAttempts) {
        if (TextUtils.isEmpty(fileName)) return null;

        HashMap<String, String> parameter = new HashMap<>();
        parameter.put(FILE_NAME_PARAM, fileName);

        final HttpURLConnection httpConnection;
        httpConnection = URLConnectionHelper.getConnection(context, GET_IMAGE_CALL, parameter);
        if (httpConnection == null) {
            Log.e(TAG, "Could not get HttpUrlConnection to get image from.");
            return null;
        }

        Bitmap bitmap = null;
        InputStream inputStream = null;
        try {
            inputStream = httpConnection.getInputStream();
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (IOException | OutOfMemoryError e) {
            Log.e(TAG, "getImage(): " + numberOfAttempts + ": " + e.toString());
            if (numberOfAttempts < URLConnectionHelper.NUMBER_OF_RETRIES) {
                return getImage(context, fileName, ++numberOfAttempts);
            }
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "getImage() finally: " + e.toString());
            }
        }
        httpConnection.disconnect();

        return bitmap;
    }

    public static long putImage(
            Context context,
            final File hiResFile,
            final File loResFile,
            final Action message
    ) {
        if (context == null || hiResFile == null || loResFile == null) return -9L;

        final User localUser = LocalUserManager.getInstance().getUser(context);
        if (localUser == null) return -10L;

        // Both media Actions & profile picture changes require the Sender and Session ID.
        final StringBuilder partBuilder = new StringBuilder();
        final String senderPart = URLConnectionHelper.buildEntityPart(
                SENDER_PARAM, String.valueOf(localUser.getId())
        );
        partBuilder.append(senderPart);

        // Add the current Room ID, _if_ in a Session, so that the other Users are notified.
        final long sessionId = SessionMainManager.getInstance().getRoomId(context);
        if (sessionId > 10L) {
            partBuilder.append(
                    URLConnectionHelper.buildEntityPart(
                            SESSION_PARAM,
                            String.valueOf(sessionId)
                    )
            );
        }

        final String successStatus;
        if (message != null) {
            successStatus = PUT_IMAGE_MESSAGE_SUCCESS_STATUS;
            // This image upload has been triggered by sending a photo message.
            // All the data is stored in the Action Object.
            partBuilder.append(
                    URLConnectionHelper.buildEntityPart(
                            RECIPIENT_PARAM,
                            String.valueOf(message.getRecipientUserId())
                    )
            );
            partBuilder.append(
                    URLConnectionHelper.buildEntityPart(
                            CODE_PARAM,
                            String.valueOf(message.getCode())
                    )
            );
            partBuilder.append(
                    URLConnectionHelper.buildEntityPart(
                            TIME_PARAM,
                            String.valueOf(message.getTimeStampSec())
                    )
            );
        } else {
            // This image upload has been triggered by a profile picture change.
            successStatus = PUT_USER_IMAGE_SUCCESS_STATUS;

            // Changing a profile picture requires authorisation using the UUID.
            partBuilder.append(
                    URLConnectionHelper.buildEntityPart(
                            DEVICE_ID_PARAM,
                            PreferenceHelper.from(context).getAuthId()
                    )
            );
            partBuilder.append(
                    URLConnectionHelper.buildEntityPart(
                            TIME_PARAM,
                            String.valueOf(System.currentTimeMillis() / 1000L)
                    )
            );
        }

        final long startTime = System.currentTimeMillis();

        final String jsonResult = URLConnectionHelper.putImage(
                API_BASE_URL + PUT_IMAGE_CALL,
                HI_RES_FILE_PARAM,
                hiResFile,
                LO_RES_FILE_PARAM,
                loResFile,
                partBuilder.toString()
        );

        final long size = hiResFile.length() / 1024L;
        final long duration = System.currentTimeMillis() - startTime;
//        Log.d(TAG, "Uploaded image (" + size + "kB) in " + duration + " ms.");

        if (TextUtils.isEmpty(jsonResult)) {
            Log.e(TAG, "Upload returned empty result.");
            return -13L;
        }

        JSONObject jsonObj;
        final String jsonStatus;
        try {
            jsonObj = new JSONObject(jsonResult);
            jsonStatus = jsonObj.getString(APIHelper.STATUS_TAG);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return -14L;
        }

        if (!jsonStatus.equals(successStatus)) {
            Log.e(TAG, "API call status: " + jsonStatus);
            return -15L;
        }

        // The insert worked. Get the ID of the new entry.
        final long receivedId;
        try {
            receivedId = jsonObj.getLong(IMAGE_ID_TAG);
        } catch (JSONException e) {
            e.printStackTrace();
            return -16L;
        }
        return receivedId;
    }

    /*
    Rooms
     */

    /**
     * Polls the underlying service to return a list of rooms within the specified
     * distance of the specified Location.
     * LocationWatcher has to be started and maintain the current position
     * before starting this request.
     */
    public static void getRooms(Context context) {
        if (context == null) return;

        final Location location = LocationWatcher.from(context).getLocation(true);
        if (location == null) {
            Log.d(TAG, "Cannot fetch nearby Rooms while Location is unknown.");
            return;
        }

        if (isPerformingRequest(GET_ROOMS_CALL)) return;

        // Prepare the HTTP parameter.
        final HashMap<String, String> queryParameter = new HashMap<>();
        queryParameter.put(LATITUDE_PARAM, String.valueOf(location.getLatitude()));
        queryParameter.put(LONGITUDE_PARAM, String.valueOf(location.getLongitude()));
        queryParameter.put(DISTANCE_PARAM, String.valueOf(SEARCH_RADIUS));

        // Loop through all JSON Room objects.
        final JSONArray jsonRooms = URLConnectionHelper.getJsonArray(
                context,
                GET_ROOMS_CALL,
                queryParameter,
                GET_ROOMS_SUCCESS_STATUS,
                ROOMS_ARRAY_TAG
        );

        didFinishRequest(GET_ROOMS_CALL);

        if (jsonRooms == null) {
            Log.e(TAG, "Received null JSONArray.");
            return;
        }

        int i;
        for (i = 0; i < jsonRooms.length(); i++) {
            Room r = null;
            try {
                r = ChatObjectJSONFactory.buildRoom(context, jsonRooms.getJSONObject(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (r != null) {
                // Start loading the images associated with this Room
                // and add it to the Content Provider.
                ImageLoader.with(context).handle(r, true).startLoading();
                RoomsContract.insertRoom(context.getApplicationContext(), r);
//                Log.d(TAG, r.toString());
            }
        }

        RoomsContract.notifyChange(context);
        if (i > 0) PreferenceHelper.from(context).storeLastUpdateLocation(location);

        // If a Session has been started. Let the Manager update the Room data.
        if (SessionMainManager.getInstance().didLogin()) {
            SessionMainManager.getInstance().getRoom(context, true);
        }
    }

    /*
    Users
     */

    public static User getUser(Context context, final long userId) {
        if (userId < 10L || context == null) {
            Log.e(TAG, "Download not starting because method parameters are invalid.");
            return null;
        }

        final String requestId = GET_USER_CALL + userId;
        if (isPerformingRequest(requestId)) return null;

        HashMap<String, String> parameter = new HashMap<>();
        // Session and local User ID for authorisation:
        final long sessionId = SessionMainManager.getInstance().getRoomId(context);
        parameter.put(SESSION_PARAM, String.valueOf(sessionId));
        final long localUserId = LocalUserManager.getInstance().getUserId(context);
        parameter.put(SENDER_PARAM, String.valueOf(localUserId));
        // Requested User ID:
        parameter.put(USER_PARAM, String.valueOf(userId));

        final JSONObject jsonUser = URLConnectionHelper.getJsonObject(
                context,
                GET_USER_CALL,
                parameter,
                GET_USER_SUCCESS_STATUS,
                USER_OBJECT_TAG
        );

        didFinishRequest(requestId);

        if (jsonUser == null) {
            Log.e(TAG, "Received JSON Object is null.");
            return null;
        }

        final User user;
        try {
            user = ChatObjectJSONFactory.buildUser(jsonUser);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }
        return user;
    }

    public static User getLocalUser(Context context, final String userName) {
        if (context == null || TextUtils.isEmpty(userName)) return null;

        final String initialStatus = context.getResources().getString(R.string.default_status);

        HashMap<String, String> parameter = new HashMap<>();
        parameter.put(DEVICE_ID_PARAM, PreferenceHelper.from(context).getAuthId());
        parameter.put(USER_NAME_PARAM, userName);
        parameter.put(USER_STATUS_PARAM, initialStatus);

        final JSONObject jsonUser = URLConnectionHelper.getJsonObject(
                context,
                GET_LOCAL_USER_CALL,
                parameter,
                GET_USER_SUCCESS_STATUS,
                USER_OBJECT_TAG
        );

        if (jsonUser == null) {
            Log.e(TAG, "Received JSON Object is null.");
            return null;
        }

        User user;
        try {
            user = ChatObjectJSONFactory.buildUser(jsonUser);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }
        return user;
    }

    /**
     * Asks the server for the entire list of user IDs in the session Room;
     * adds the users to the local database of all Users and to the list of users in this session
     */
    public static boolean getSessionUsers(Context context) {
        if (context == null) return false;

        HashMap<String, String> parameter = new HashMap<>();
        final long localUserId = LocalUserManager.getInstance().getUserId(context);
        parameter.put(SENDER_PARAM, String.valueOf(localUserId));
        final long sessionId = SessionMainManager.getInstance().getRoomId(context);
        parameter.put(SESSION_PARAM, String.valueOf(sessionId));

        final JSONArray jsonUsers = URLConnectionHelper.getJsonArray(
                context,
                GET_SESSION_USERS_CALL,
                parameter,
                GET_SESSION_USERS_SUCCESS_STATUS,
                USERS_ARRAY_TAG
        );

        if (jsonUsers == null) {
            Log.e(TAG, "Received null Users Array.");
            return false;
        }

        SessionMainManager.getInstance().resetUserList();
        for (int i = 0; i < jsonUsers.length(); i++) {
            // Create the User object from the downloaded JSON data.
            User user;
            try {
                user = ChatObjectJSONFactory.buildUser(jsonUsers.getJSONObject(i));
            } catch (JSONException e) {
                e.printStackTrace();
                user = null;
            }

            if (user != null && !user.isLocalUser(context)) {
                // Add the User to the local database of all users ever seen.
                ChatObjectContract.insertUser(context, user);
                // Add the User to the list of user IDs in the current session.
                SessionMainManager.getInstance().addUserId(user.getId());
                ImageLoader.with(context).handle(user, true).startLoading();
                UsersCache.getInstance().addUser(user);
            }
        }

        SessionMainManager.getInstance().ackUserListUpdate();
        return true;
    }

    private static boolean doUpdateLocalUser(Context context) {
        if (context == null) return false;
        final User localUser = LocalUserManager.getInstance().getUser(context);
        if (localUser == null) return false;

        HashMap<String, String> parameter = new HashMap<>();
        parameter.put(SENDER_PARAM, String.valueOf(localUser.getId()));
        parameter.put(DEVICE_ID_PARAM, PreferenceHelper.from(context).getAuthId());
        parameter.put(USER_NAME_PARAM, localUser.getName());
        parameter.put(USER_STATUS_PARAM, localUser.getStatusMessage());
        parameter.put(EMAIL_PARAM, localUser.getEmailAddress());
        parameter.put(PHONE_PARAM, localUser.getPhoneNumber());
        parameter.put(WEBSITE_PARAM, localUser.getWebsite());
        parameter.put(FB_PARAM, localUser.getFacebookId());
        parameter.put(IG_PARAM, localUser.getIgName());
        parameter.put(TWITTER_PARAM, localUser.getTwitterName());

        // Add the current Room ID, _if_ in a Session, so that the other Users are notified.
        final long sessionId = SessionMainManager.getInstance().getRoomId(context);
        if (sessionId > 10L) parameter.put(SESSION_PARAM, String.valueOf(sessionId));

        // Make a call to the script that inserts the user startLoadingInto the database.
        // This returns a status code and the ID of the new user.
        return URLConnectionHelper.performCall(
                context,
                DO_UPDATE_USER_CALL,
                parameter,
                UPDATE_USER_SUCCESS_STATUS
        );
    }

    /*
    Authentication Parameters
     */

    private static HashMap<String, String> buildSessionParameterMap(final Context context) {
        HashMap<String, String> parameter = new HashMap<>();
        final long roomId = SessionMainManager.getInstance().getRoomId(context);
        if (roomId > 10L) parameter.put(SESSION_PARAM, String.valueOf(roomId));
        else return null;

        final long senderId = LocalUserManager.getInstance().getUserId(context);
        if (senderId > 10L) parameter.put(SENDER_PARAM, String.valueOf(senderId));
        else return null;

        return parameter;
    }

    /*
    Duplicate request watcher
     */

    private static boolean isPerformingRequest(final String identifier) {
        if (sCurrentRequests != null) {
            if (!sCurrentRequests.containsKey(identifier)) {
                sCurrentRequests.put(identifier, System.currentTimeMillis());
//                Log.d(TAG, "Added request to current connections: " + identifier);
                return false;
            } else {
                long timestamp = sCurrentRequests.get(identifier);
                long now = System.currentTimeMillis();
                if (now - timestamp > MAX_REQUEST_MILLIS) {
//                    Log.d(TAG, "Timeout of request " + identifier + ".");
                    sCurrentRequests.put(identifier, System.currentTimeMillis());
                    return false;
                } else {
//                    Log.d(TAG, "Avoided request duplicate of running " + identifier + ".");
                    return true;
                }
            }
        } else {
            sCurrentRequests = new HashMap<>();
            sCurrentRequests.put(identifier, System.currentTimeMillis());
//            Log.d(TAG, "Added request to current connections: " + identifier + " (0)");
            return false;
        }
    }

    private static void didFinishRequest(final String identifier) {
        if (sCurrentRequests != null && !sCurrentRequests.isEmpty()) {
//            Log.d(TAG, "Removed request from current connections: " + identifier);
            sCurrentRequests.remove(identifier);
        }
    }

}
