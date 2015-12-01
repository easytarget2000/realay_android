package org.eztarget.realay.data;

import android.content.ContentResolver;

/**
 * Created by michel on 30/12/14.
 *
 */
public class Conversation {

    /** The mime type of a directory of items */
    public static final String CONTENT_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.org.eztarget.realay.conversations";

    /** The mime type of a single item. */
    public static final String CONTENT_ITEM_TYPE =
            ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.org.eztarget.realay.conversations";

}
