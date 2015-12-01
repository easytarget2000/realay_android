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

package org.eztarget.realay;

public class Constants {


    public static final int MAX_NAME_LENGTH = 40;

    public static final String KEY_FORCE_REFRESH = "force_refresh";

    public static final String EXTRA_ACTION = "action";

    public static final String EXTRA_LOCATION_EVENT = "location_event";

    public static final String EXTRA_WARN_REASON = "warn_reason";

    public static final String EXTRA_UNREAD_COUNT = "unread_count";

    public static final String EXTRA_USER_ID = "user_id";

    public static final String EXTRA_SHOW_CONVERSATIONS = "show_convs";

    public static final int EVENT_LOCATION_ISSUE_RESOLVED = 10;

    public static final int EVENT_LOCATION_ISSUE_STARTED = 11;

    /*
    INTENTS
     */

    public static String ACTION_DISPLAY_MESSAGE =
            "org.eztarget.realay.ACTION_DISPLAY_MESSAGE";

    public static final String ACTION_HEARTBEAT =
            "org.eztarget.realay.ACTION_HEARTBEAT";

    public static final String ACTION_LOCATION_PROVIDER_CHANGED =
            "org.eztarget.realay.ACTION_LOCATION_PROVIDER_CHANGED";

    public static final String ACTION_ROOM_LIST_UPDATE =
            "org.eztarget.realay.ACTION_ROOM_LIST_UPDATE";

    public static final String ACTION_PENALTY_EVENT =
            "org.eztarget.realay.ACTION_PENALTY_EVENT";

    public static final String ACTION_UNREAD_COUNT_CHANGED =
            "org.eztarget.realay.ACTION_UNREAD_COUNT_CHANGED";

    public static final String ACTION_USER_LIST_CHANGED =
            "org.eztarget.realay.ACTION_USER_LIST_CHANGED";

    public static final String ACTION_WARN_HEARTBEAT =
            "org.eztarget.realay.ACTION_WARN_HEARTBEAT";

    public static final String ACTION_RETRY_SENDING =
            "org.eztarget.realay.retry_queued_actions";

    public static final String ACTIVE_LOCATION_UPDATE_PROVIDER_DISABLED =
            "org.eztarget.realay.active_location_update_provider_disabled";

    public static final String CONSTRUCTED_LOCATION_PROVIDER = "CONSTRUCTED_LOCATION_PROVIDER";

}
