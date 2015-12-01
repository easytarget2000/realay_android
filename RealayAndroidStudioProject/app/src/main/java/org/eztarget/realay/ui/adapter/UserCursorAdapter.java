package org.eztarget.realay.ui.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.eztarget.realay.Constants;
import org.eztarget.realay.R;
import org.eztarget.realay.content_providers.BaseColumns;
import org.eztarget.realay.content_providers.ChatObjectContract;
import org.eztarget.realay.data.Action;
import org.eztarget.realay.data.User;
import org.eztarget.realay.managers.SessionActionsManager;
import org.eztarget.realay.managers.UsersCache;
import org.eztarget.realay.ui.BlockedUsersActivity;
import org.eztarget.realay.ui.PrivateConversationActivity;
import org.eztarget.realay.ui.utils.FormatHelper;
import org.eztarget.realay.ui.utils.ImageLoader;
import org.eztarget.realay.ui.utils.ViewAnimator;

/**
 * Created by michel on 09/12/14.
 */
public class UserCursorAdapter
        extends CursorRecyclerViewAdapter<UserCursorAdapter.UserViewHolder>
        implements View.OnClickListener, View.OnLongClickListener {

    private static final String TAG = UserCursorAdapter.class.getSimpleName();

    private BlockedUsersActivity mBlockedUsersActivity;

    private boolean mHasLongClick = false;

//    private Uri mImageShareUri;

    public UserCursorAdapter(Context context, Cursor cursor, final boolean hasLongClick) {
        super(context, cursor);
        mHasLongClick = hasLongClick;
    }

    public UserCursorAdapter(BlockedUsersActivity blockedUsersActivity, Cursor cursor) {
        super(blockedUsersActivity, cursor);
        mBlockedUsersActivity = blockedUsersActivity;
        mHasLongClick = false;
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        protected ImageView mIconView;
        protected TextView mNameView;
        protected View mNotificationView;
        protected TextView mStatusView;
        protected TextView mTimeView;
        protected View mUnblockButton;

        public UserViewHolder(View v) {
            super(v);
            mIconView = (ImageView) v.findViewById(R.id.image_user_icon);
            mNameView = (TextView) v.findViewById(R.id.text_user_name);
            mNotificationView = v.findViewById(R.id.view_notification);
            mStatusView = (TextView) v.findViewById(R.id.text_user_status);
            mTimeView = (TextView) v.findViewById(R.id.text_user_msg_time);
            mUnblockButton = v.findViewById(R.id.image_unblock);
        }
    }

//    public void setImageShareUri(Uri imageShareUri) {
//        mImageShareUri = imageShareUri;
//    }

    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        final LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View userCardView = inflater.inflate(R.layout.item_user, viewGroup, false);

        final UserViewHolder viewHolder = new UserViewHolder(userCardView);
        userCardView.setTag(viewHolder);

        if (mBlockedUsersActivity != null) {
            viewHolder.mUnblockButton.setOnClickListener(this);
        } else {
            userCardView.setOnClickListener(this);
        }

        if (mHasLongClick) userCardView.setOnLongClickListener(this);

        return viewHolder;
    }

    @Override
    public void onClick(View view) {
        ViewAnimator.quickFade(view);
        handleClick(view, false);
    }

    @Override
    public boolean onLongClick(View view) {
        handleClick(view, true);
        return true;
    }

    private void handleClick(View view, final boolean isLongClick) {
        final Object viewTag = view.getTag();
        if (!(viewTag instanceof Long)) return;

        ViewAnimator.quickFade(
                view,
                new ViewAnimator.Callback() {
                    @Override
                    public void onAnimationEnd() {
                        final long selectedUserId = (long) viewTag;

                        if (mBlockedUsersActivity == null) {
                            if (isLongClick) {
                                // Not in the Blocked User Activity and a long click:
                                // Show the Delete Conversation Dialog.

                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                builder.setItems(
                                        R.array.conversation_dialog_items,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(
                                                    DialogInterface dialogInterface,
                                                    int i
                                            ) {
                                                if (i == 0) {
                                                    showConversationDeleteDialog(selectedUserId);
                                                }
                                            }
                                        }
                                );
                                builder.setCancelable(true);
                                builder.show();
                            } else {
                                // Not in the Blocked User Activity and a short click:
                                // Open a Conversation.
                                final Intent conversation = new Intent(
                                        getContext(),
                                        PrivateConversationActivity.class
                                );
                                conversation.putExtra(Constants.EXTRA_USER_ID, selectedUserId);
                                getContext().startActivity(conversation);
                            }
                        } else {
                            if (!isLongClick) mBlockedUsersActivity.unblockUser(selectedUserId);
                        }
                    }
                });
    }

    private void showConversationDeleteDialog(final long partnerId) {
        final User partner = UsersCache.getInstance().fetch(partnerId);
        if (partner == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final String messageFormat;
        messageFormat = getContext().getResources().getString(R.string.delete_conversation_with);
        final String message = String.format(messageFormat, partner.getName());
        builder.setMessage(message);
        builder.setNegativeButton(android.R.string.no, null);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ChatObjectContract.deleteConversation(getContext(), partnerId);
            }
        });
        builder.show();
    }

    @Override
    public void onBindViewHolder(UserViewHolder viewHolder, Cursor cursor) {
        // Retrieve the User for this cursor.
        final User cursorUser = UsersCache.getInstance().fetch(cursor, false);
        if (cursorUser == null) {
            Log.e(TAG, "Could not get User from " + cursor.toString() + ".");
            DatabaseUtils.dumpCursor(cursor);
            return;
        }

        // Load the low-resolution image into the small ImageView here
        ImageLoader imageLoader = new ImageLoader(getContext());
        imageLoader.handle(cursorUser, true);
        imageLoader.doCropUserCircle();
        imageLoader.startLoadingInto(
                viewHolder.mIconView,
                false,
                null,
                0
        );

        // Put the User's name in the name View.
        viewHolder.mNameView.setText(cursorUser.getName());

        // Show an unblock button if the Activity requested this.
        if (mBlockedUsersActivity != null) {
            viewHolder.mUnblockButton.setVisibility(View.VISIBLE);
            viewHolder.mUnblockButton.setTag(cursorUser.getId());
        } else {
            viewHolder.itemView.setTag(cursorUser.getId());
        }

        // Display the last message from/to this User, if available.
        // Otherwise display the User's status.
        Action lastMessage = buildMessagePreview(cursor);
        if (lastMessage == null) {
            viewHolder.mStatusView.setText(cursorUser.getStatusMessage());
        } else {
            final String lastMessageContent;
            if (lastMessage.isPhotoMessage())
                lastMessageContent = getContext().getResources().getString(R.string.picture);
            else lastMessageContent = lastMessage.getMessage();
            viewHolder.mStatusView.setText(lastMessageContent);

            viewHolder.mTimeView.setText(FormatHelper.buildDateTime(lastMessage.getTimeStampSec()));

            if (SessionActionsManager.from(getContext()).hasUnreadMessages(cursorUser.getId())) {
                viewHolder.mStatusView.setPaintFlags(Paint.FAKE_BOLD_TEXT_FLAG);
                viewHolder.mTimeView.setPaintFlags(Paint.FAKE_BOLD_TEXT_FLAG);
                viewHolder.mNotificationView.setVisibility(View.VISIBLE);
            } else {
                viewHolder.mNotificationView.setVisibility(View.GONE);
                viewHolder.mStatusView.setPaintFlags(1);
                viewHolder.mTimeView.setPaintFlags(1);
            }
        }

    }

    private static Action buildMessagePreview(Cursor conversationCursor) {
        if (conversationCursor == null || conversationCursor.getColumnCount() <= 0) return null;

        final int contentIndex = conversationCursor.getColumnIndex(BaseColumns.MSG_CONTENT);
        if (contentIndex < 0) return null;
        final String messageContent = conversationCursor.getString(contentIndex);

        final int timeIndex = conversationCursor.getColumnIndex(BaseColumns.MSG_TIME);
        if (timeIndex < 0) return null;
        final long timestampSec = conversationCursor.getLong(timeIndex);

        final int codeIndex = conversationCursor.getColumnIndex(BaseColumns.MSG_CODE);
        if (codeIndex < 0) return null;
        final int actionCode = conversationCursor.getInt(codeIndex);

        return new Action(timestampSec, messageContent, actionCode);
    }

}
