package org.eztarget.realay.ui.adapter;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Build;
import android.provider.BaseColumns;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.eztarget.realay.Constants;
import org.eztarget.realay.R;
import org.eztarget.realay.data.Action;
import org.eztarget.realay.data.Room;
import org.eztarget.realay.data.User;
import org.eztarget.realay.managers.LocalUserManager;
import org.eztarget.realay.managers.SessionMainManager;
import org.eztarget.realay.managers.UsersCache;
import org.eztarget.realay.ui.ConversationActivity;
import org.eztarget.realay.ui.PrivateConversationActivity;
import org.eztarget.realay.ui.UserDetailsActivity;
import org.eztarget.realay.ui.utils.FormatHelper;
import org.eztarget.realay.ui.utils.ImageLoader;
import org.eztarget.realay.ui.utils.ViewAnimator;

import java.util.HashMap;

/**
 * Created by michel@eztarget.org on 13/12/14.
 */
public class MessageCursorAdapter
        extends CursorRecyclerViewAdapter<MessageCursorAdapter.MessageViewHolder>
        implements View.OnLongClickListener, View.OnClickListener {

    private static final String TAG = MessageCursorAdapter.class.getSimpleName();

    private HashMap<Long, Action> mMessageMap = new HashMap<>();

    private ConversationActivity mActivity;

    protected boolean mIsPublicConversation = true;

    public MessageCursorAdapter(ConversationActivity activity, Cursor cursor) {
        super(activity, cursor);
        mActivity = activity;
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        protected ImageView mIconView;
        protected TextView mNameView;
        protected ImageView mPhotoView;
        protected ProgressBar mProgressBar;
        protected TextView mTextContentView;
        protected TextView mTimeStampView;

        public MessageViewHolder(
                final View v,
                final View.OnClickListener clickListener,
                final View.OnLongClickListener longClickListener,
                final boolean doShowNames
        ) {
            super(v);
            mIconView = (ImageView) v.findViewById(R.id.image_user_icon);

            mNameView = (TextView) v.findViewById(R.id.text_message_sender);
            if (!doShowNames && mNameView != null) {
                mNameView.setVisibility(View.GONE);
                mNameView = null;
            }
            mPhotoView = (ImageView) v.findViewById(R.id.image_message_photo);
            mProgressBar = (ProgressBar) v.findViewById(R.id.progress_media);
            mTextContentView = (TextView) v.findViewById(R.id.text_message_content);
            mTimeStampView = (TextView) v.findViewById(R.id.text_message_time);

            View card = v.findViewById(R.id.group_message_card);
            card.setTag(this);
            if (clickListener != null) card.setOnClickListener(clickListener);
            if (longClickListener != null) card.setOnLongClickListener(longClickListener);
        }
    }

    public void isPublicConversation(final boolean doShowNames) {
        mIsPublicConversation = doShowNames;
    }

    public Action getMessage(final int cursorPosition) {
        Cursor cursor = getCursor();
        cursor.moveToPosition(cursorPosition);

        final int messageIdIndex = cursor.getColumnIndex(BaseColumns._ID);
        final long cursorMessageId = cursor.getLong(messageIdIndex);

        final Action cursorMessage = mMessageMap.get(cursorMessageId);
        if (cursorMessage == null) return Action.buildAction(cursor);
        else return cursorMessage;
    }

    private static final int TYPE_INFO_MESSAGE = 0;

    private static final int TYPE_SENT_MESSAGE = 1;

    private static final int TYPE_RECEIVED_MESSAGE = 2;

    private static final int TYPE_SERVER_MESSAGE = 4;

    private static final int TYPE_SENT_PHOTO = 11;

    private static final int TYPE_RECEIVED_PHOTO = 12;

    private long mLocalUserId = -100L;

    @Override
    public int getItemViewType(int position) {
        Action message = getMessage(position);
        if (message.isInfoMessage()) return TYPE_INFO_MESSAGE;
        //        if (message.isTaggedMessage()) return TYPE_TAGGED_MESSAGE;

        if (mLocalUserId < 10L) {
            mLocalUserId = LocalUserManager.getInstance().getUserId(getContext());
        }
        final boolean isSentMessage = (message.getSenderUserId() == mLocalUserId);
        final boolean isPhotoMessage = message.isPhotoMessage();
        if (isSentMessage) {
            if (isPhotoMessage) return TYPE_SENT_PHOTO;
            else return TYPE_SENT_MESSAGE;
        } else {
            if (isPhotoMessage) return TYPE_RECEIVED_PHOTO;
            else return TYPE_RECEIVED_MESSAGE;
        }

    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        final LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());

        final View cardView;
        switch (i) {
            case TYPE_RECEIVED_MESSAGE:
                cardView = inflater.inflate(R.layout.item_message, viewGroup, false);
                return new MessageViewHolder(cardView, this, this, mIsPublicConversation);

            case TYPE_SENT_MESSAGE:
                cardView = inflater.inflate(R.layout.item_message_sent, viewGroup, false);
                return new MessageViewHolder(cardView, this, this, false);

            case TYPE_RECEIVED_PHOTO:
                cardView = inflater.inflate(R.layout.item_message_photo, viewGroup, false);
                return new MessageViewHolder(cardView, this, this, mIsPublicConversation);

            case TYPE_SENT_PHOTO:
                cardView = inflater.inflate(R.layout.item_message_photo_sent, viewGroup, false);
                // No longClickListener. No name.
                return new MessageViewHolder(cardView, this, null, false);

            case TYPE_INFO_MESSAGE:
                cardView = inflater.inflate(R.layout.card_server_message, viewGroup, false);
                return new MessageViewHolder(cardView, null, null, false);

            default:
                cardView = inflater.inflate(R.layout.item_message, viewGroup, false);
                return new MessageViewHolder(cardView, this, this, false);
        }

    }

    @Override
    public void onBindViewHolder(MessageViewHolder viewHolder, Cursor cursor) {
        // Grab the message ID from the cursor. It is always in column 0.
        final long cursorMessageId = cursor.getLong(0);
        Action message = mMessageMap.get(cursorMessageId);
        if (message == null) {
            message = Action.buildAction(cursor);
            if (message == null) return;
            mMessageMap.put(cursorMessageId, message);
        }

        if (viewHolder.mIconView != null) {
            // Grab the User from the cache that has the given sender ID.
            final long senderId = message.getSenderUserId();
            final User sender = UsersCache.getInstance().fetch(senderId);
            if (sender != null) {

                // Load the low-resolution image into the small ImageView here
                // and also cache the high-res image.
                ImageLoader imageLoader = new ImageLoader(getContext());
                imageLoader.handle(sender, false);
                imageLoader.doCropUserCircle();
                imageLoader.startLoadingInto(
                        viewHolder.mIconView,
                        false,
                        null,
                        0
                );

                if (viewHolder.mNameView != null) {
                    // Find the name of this sender.
                    // If the User is not cached,
                    // fall back to using the name that came with the Cursor.
                    final String userName = UsersCache.getInstance().fetchName(senderId);
                    viewHolder.mNameView.setText(userName);
                }
            }
        }

        if (viewHolder.mProgressBar != null) {
            if (viewHolder.mProgressBar.getVisibility() == View.VISIBLE) {
                viewHolder.mProgressBar.setVisibility(View.INVISIBLE);
            }
        }

        if (viewHolder.mPhotoView != null) {

            final Bitmap cachedImage = message.getPreviewImage();
            if (cachedImage == null) {
                // Start loading the photo into this message.
                ImageLoader imageLoader = new ImageLoader(getContext());
                imageLoader.handle(message, false);
                imageLoader.startLoadingInto(
                        viewHolder.mPhotoView,
                        false,
                        viewHolder.mProgressBar,
                        R.drawable.ic_photo_camera_white_24dp
                );
            } else {
                viewHolder.mPhotoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                viewHolder.mPhotoView.setImageBitmap(cachedImage);
            }
        } else {
            // Display the message and a timestamp.
            viewHolder.mTextContentView.setText(message.getMessage());
        }

        viewHolder.mTimeStampView.setText(FormatHelper.buildDateTime(message.getTimeStampSec()));
//        viewHolder.mTimeStampView.setText(cursorMessageId + ", " + message.getImageId() + ", " + message.buildDateTimeString());
    }

    @Override
    public void onClick(View v) {
        ViewAnimator.quickFade(v);

        final Object tag = v.getTag();
        if (!(tag instanceof MessageViewHolder)) return;
        final MessageViewHolder viewHolder = (MessageViewHolder) tag;

        final Action message = getMessage(viewHolder.getAdapterPosition());
        if (!message.isPhotoMessage()) return;

        ImageLoader loader = new ImageLoader(getContext());
        loader.startViewIntent(message, viewHolder.mProgressBar);
    }

    @Override
    public boolean onLongClick(View v) {
        ViewAnimator.quickFade(v);

        final Object tag = v.getTag();
        if (!(tag instanceof MessageViewHolder)) return false;
        final MessageViewHolder viewHolder = (MessageViewHolder) tag;

        final Action message = getMessage(viewHolder.getAdapterPosition());
        final Context context = getContext();
        final long senderId = message.getSenderUserId();

        final Resources res = context.getResources();
        final CharSequence[] items;
        final String copy = res.getString(R.string.copy_message);

        if (!mIsPublicConversation || senderId == mLocalUserId) {
            items = new CharSequence[]{copy};
        } else {
            final String showProfile = res.getString(R.string.show_profile);
            final String privateConv = res.getString(R.string.private_conversation);
            final String block = res.getString(R.string.block_user);
            final String ban = res.getString(R.string.ban_user);
            final String kick = res.getString(R.string.kick_user);

            // Build the dialog items. Media messages cannot be copied.
            if (SessionMainManager.getInstance().isAdmin()) {
                if (message.isPhotoMessage()) {
                    items = new CharSequence[]{privateConv, showProfile, block, kick, ban};
                } else {
                    items = new CharSequence[]{privateConv, showProfile, block, copy, kick, ban};
                }
            } else {
                if (message.isPhotoMessage()) {
                    items = new CharSequence[]{privateConv, showProfile, block};
                } else {
                    items = new CharSequence[]{privateConv, showProfile, block, copy};
                }
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!mIsPublicConversation || senderId == mLocalUserId) {
                    if (which == 0) {
                        // Act like the regular (third) copy button has been pressed.
                        which = 3;
                    } else {
                        return;
                    }
                }

                switch (which) {
                    case 0:
                        // Start a conversation with the sender, IF they are still in the Room.
                        if (SessionMainManager.getInstance().containsUser(senderId)) {
                            Intent conversation;
                            conversation = new Intent(context, PrivateConversationActivity.class);
                            conversation.putExtra(Constants.EXTRA_USER_ID, senderId);
                            context.startActivity(conversation);
                            return;
                        } else {
                            // If the User is no longer available, display a toast.
                            final String format = res.getString(R.string.has_left);
                            final String userName = UsersCache.getInstance().fetchName(senderId);
                            final Room room;
                            room = SessionMainManager.getInstance().getRoom(getContext(), false);
                            if (room == null) return;
                            final String hasLeft = String.format(format, userName, room.getTitle());
                            Toast.makeText(context, hasLeft, Toast.LENGTH_LONG).show();
                            return;
                        }

                    case 1:
                        // Show the User's profile.
                        Intent profileActivity = new Intent(context, UserDetailsActivity.class);
                        profileActivity.putExtra(Constants.EXTRA_USER_ID, senderId);
                        context.startActivity(profileActivity);
                        return;

                    case 2:
                        // Show blocking Dialog.
                        final User smug = UsersCache.getInstance().fetch(senderId);
                        mActivity.showBlockingDialog(smug);
                        return;

                    case 3:
                        // Copy the text to the clipboard.
                        ClipboardManager board =
                                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        final String user = UsersCache.getInstance().fetchName(senderId);
                        final String label = "Realay Message";
                        final String text;
                        text = user + ", "
                                + FormatHelper.buildDateTime(message.getTimeStampSec())
                                + ": " + message.getMessage();
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                            board.setText(text);
                        } else {
                            final ClipData clip = ClipData.newPlainText(label, text);
                            board.setPrimaryClip(clip);
                        }

                        Toast.makeText(getContext(), R.string.message_copied, Toast.LENGTH_SHORT)
                                .show();
                        return;

                    case 4:
                        mActivity.showKickDialog(UsersCache.getInstance().fetch(senderId), false);
                        return;

                    case 5:
                        mActivity.showKickDialog(UsersCache.getInstance().fetch(senderId), true);
                }
            }

        });

        builder.show();
        return true;
    }

}
