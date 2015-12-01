package org.eztarget.realay.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import org.eztarget.realay.R;
import org.eztarget.realay.content_providers.ChatObjectContract;
import org.eztarget.realay.data.Action;
import org.eztarget.realay.data.User;
import org.eztarget.realay.managers.LocalUserManager;
import org.eztarget.realay.ui.ConversationActivity;
import org.eztarget.realay.ui.utils.ImageLoader;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Created by michel on 30/11/14.
 */
public class ImageUploaderTask extends AsyncTask<String, Bitmap, Boolean> {

    private static final String TAG = ImageUploaderTask.class.getSimpleName();

    private Activity mActivity;

    private Bitmap mHiResBitmap;

    private Bitmap mLoResBitmap;

    private Action mMessage;

    private User mLocalUser;

    private boolean mIsMediaMessage = false;

    private boolean mIsProfileImage = false;

    private Bitmap mOldUserImage;

    private WeakReference<ImageView> mViewReference;

    public static ImageUploaderTask userImageUploader(
            Activity toastActivity,
            Bitmap newImage,
            ImageView imagePreview
    ) {
        if (toastActivity == null || newImage == null) {
            Log.e(TAG, "userImageUploader(): Missing arguments.");
            return null;
        }
        return new ImageUploaderTask(toastActivity, newImage, imagePreview);
    }

    public static ImageUploaderTask imageMessageUploader(
            final ConversationActivity conversationActivity,
            Bitmap image,
            Action message
    ) {
        if (conversationActivity == null || image == null || message == null) {
            Log.e(TAG, "userImageUploader(): Missing arguments.");
            return null;
        }
        return new ImageUploaderTask(conversationActivity, image, message);
    }

    private ImageUploaderTask(
            final Activity toastActivity,
            Bitmap bitmap,
            final ImageView imageView
    ) {
        super();
        mActivity = toastActivity;
        mHiResBitmap = bitmap;
        if (imageView != null) mViewReference = new WeakReference<>(imageView);
        mLocalUser = LocalUserManager.getInstance().getUser(toastActivity);

        mIsMediaMessage = false;
        mIsProfileImage = true;
    }

    private ImageUploaderTask(
            final Activity toastActivity,
            Bitmap bitmap,
            Action message
    ) {
        super();
        mActivity = toastActivity;
        mHiResBitmap = bitmap;
        mLocalUser = LocalUserManager.getInstance().getUser(toastActivity);
        mMessage = message;

        mIsMediaMessage = true;
        mIsProfileImage = false;
    }

    protected Boolean doInBackground(String... params) {
        if (!DeviceStatusHelper.isConnected(mActivity)) return false;

        mHiResBitmap = ImageEditor.limitDimensions(mHiResBitmap, ImageEditor.HI_RES_MAX_PIXEL);

        final long localImageId = (long) (Math.random() * Long.MAX_VALUE);
        final String fileId = String.valueOf(localImageId);
        File hiResFile = ImageLoader.getFileForId(mActivity, fileId + ".jpg");
        ImageLoader.storeBitmap(mHiResBitmap, hiResFile, ImageLoader.HI_RES_QUALITY);

        // Crop and scale the low-res image and store it on the device.
        mLoResBitmap = ImageEditor.limitDimensions(mHiResBitmap, ImageEditor.LO_RES_MAX_PIXEL);

        if (mIsProfileImage && mLocalUser != null) {
            // Store the old User image data, so that it can be restored, if this upload fails.
//            mOldUserImageId = mLocalUser.getImageId();
            mOldUserImage = mLocalUser.getPreviewImage();
            mLocalUser.setImageId(localImageId);
        }

        if (mIsMediaMessage && mMessage != null) {
            mMessage.setImageId(localImageId);
        }
        publishProgress(mLoResBitmap);

        final File loResFile = ImageLoader.getFileForId(mActivity, fileId + "s.jpg");
        ImageLoader.storeBitmap(mLoResBitmap, loResFile, ImageLoader.LO_RES_QUALITY);

        final long receivedId = APIHelper.putImage(mActivity, hiResFile, loResFile, mMessage);
        if (receivedId < 100L) return false;



        // Change the local User's Image IDs.
        if (mIsProfileImage && mLocalUser != null) {
            // Rename the files using the server-synced Image IDs to avoid re-downloads.
            if (!hiResFile.renameTo(ImageLoader.getFileForId(mActivity, receivedId + ".jpg"))) {
                Log.w(TAG, "Could not rename hi-res file: " + hiResFile + ", " + receivedId);
            }

            if (!loResFile.renameTo(ImageLoader.getFileForId(mActivity, receivedId + "s.jpg"))) {
                Log.w(TAG, "Could not rename lo-res file: " + loResFile + ", " + receivedId);
            }

            mLocalUser.setImageId(receivedId);
            LocalUserManager.getInstance().updateStorage(mActivity);
        }

//        if (mMessage != null) mMessage.setImageId(receivedId);

        return true;
    }

    @Override
    protected void onProgressUpdate(Bitmap... values) {
        super.onProgressUpdate(values);
        if (mIsMediaMessage && mMessage != null) {
            ChatObjectContract.insertAction(mActivity, mMessage, true);
        }

        if (mViewReference != null && values.length == 1) {
            ImageView imageView = mViewReference.get();
            if (imageView != null) {
                final Bitmap circleImage;
                circleImage = ImageEditor.cropBitmapCircle(values[0]);
                imageView.setImageBitmap(circleImage);
            }
        }
    }

    @Override
    protected void onPostExecute(Boolean didSucceed) {
        super.onPostExecute(didSucceed);

        if (didSucceed) {
            // If this upload is part of a photo message, add it to the Content Provider,
            // so that the message is displayed immediately.
            if (mIsMediaMessage && mMessage != null) {
                ChatObjectContract.insertAction(mActivity, mMessage, false);
//                if (mActivity instanceof ConversationActivity) {
//                    ((ConversationActivity) mActivity).restartLoaders();
//                }
            }

            // The upload was successful.
            if (mViewReference != null && mLoResBitmap != null) {
                ImageView imageView = mViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(mLoResBitmap);
                }
            }
        } else {
            if (mActivity == null) return;
            // An error occurred.
            Toast.makeText(mActivity, R.string.cannot_upload, Toast.LENGTH_SHORT).show();

            if (mIsProfileImage) {
                if (mViewReference != null && mOldUserImage != null) {
                    ImageView imageView = mViewReference.get();
                    if (imageView != null) {
                        imageView.setImageBitmap(mOldUserImage);
                    }
                }
            } else if (mIsMediaMessage && mMessage != null) {
                ChatObjectContract.deleteAction(mActivity, mMessage);
            }
        }
    }

}
