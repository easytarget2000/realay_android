package org.eztarget.realay.ui.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import org.eztarget.realay.R;
import org.eztarget.realay.data.ChatObject;
import org.eztarget.realay.data.Room;
import org.eztarget.realay.managers.SessionMainManager;
import org.eztarget.realay.utils.APIHelper;
import org.eztarget.realay.utils.ImageEditor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Created by michel on 22/11/14.
 */
public class ImageLoader {

    private static final String TAG = ImageLoader.class.getSimpleName();

    private static final String DO_GET_LO_RES_FLAG = "DO_GET_LO_RES_FLAG";

    public static final int LO_RES_QUALITY = 50;

    public static final int HI_RES_QUALITY = 90;

    private static final long VIEW_FREEZE_INTERVAL_MILLIS = 10L * 1000L;

    private Context mContext;
    private ChatObject mObject;
    private WeakReference<ImageView> mViewReference;
    private WeakReference<ProgressBar> mProgressBarReference;
    private NotificationCompat.Builder mNotificationBuilder;
    private int mNotificationId = -1;
    private ImageLoaderTask mHiResLoaderTask;
    private boolean mDidFinish = false;
    private boolean mDoLoadHiRes = true;
    private boolean mDoShowHiRes = true;
    private boolean mDoStartIntent = false;
    private boolean mDoLoadUserCircle = false;

    public ImageLoader(Context context) {
        mContext = context;
    }

    public static ImageLoader with(Context context) {
        return new ImageLoader(context);
    }

    public ImageLoader handle(final ChatObject object, final boolean doLoadHiRes) {
        mObject = object;
        mDoLoadHiRes = doLoadHiRes;
        return this;
    }

    public void startViewIntent(final ChatObject object, final ProgressBar progressBar) {
        mObject = object;
        if (mObject == null) {
            Log.e(TAG, "No ChatObject given to get image file names from.");
            return;
        }
        mDidFinish = false;
        mDoShowHiRes = true;
        mDoStartIntent = true;
        if (progressBar != null) {
            mProgressBarReference = new WeakReference<>(progressBar);
        }
        mHiResLoaderTask = new ImageLoaderTask();
        mHiResLoaderTask.execute();
    }

    public void startLoadingIntoAndNotify(
            ChatObject object,
            NotificationCompat.Builder notificationBuilder,
            final int notificationId
    ) {
        if (object == null || notificationBuilder == null) return;
        mObject = object;
        mNotificationBuilder = notificationBuilder;
        mNotificationId = notificationId;
        mDoShowHiRes = true;
        mDoLoadHiRes = true;
        startLoading();
    }

    public void startLoadingInto(
            final ImageView imageView,
            final boolean doShowHiRes,
            final ProgressBar progressBar,
            int placeHolderResourceId
    ) {
        mDoShowHiRes = doShowHiRes;

        if (imageView == null) {
            Log.e(TAG, "ImageView cannot be null.");
            return;
        }

        if (mObject == null) {
            Log.e(TAG, "No ChatObject set before started loading.");
            return;
        }

        final Object lastFinished = imageView.getTag(R.string.tag_image_view_last_finished);
        if (lastFinished != null && lastFinished instanceof Long) {
            final long now = System.currentTimeMillis();
            final long lastFinishedMillis = (long) lastFinished;
            if (now - lastFinishedMillis < VIEW_FREEZE_INTERVAL_MILLIS) {
                return;
            }
        }

//        // Do not apply Images from a ChatObject
//        // that has not been associated to this ImageView.
//        // This might occur due to View recycling.
//        final Object imageId = imageView.getTag(R.string.tag_image_view_image_id);
//        if (imageId != null && imageId instanceof Long) {
//            if ((Long) imageId == mObject.getImageId()) {
//                return;
//            }
//        }

        final Bitmap objectIcon = mObject.getPreviewImage();
        if (objectIcon != null) {
            if (imageView.getScaleType() != ImageView.ScaleType.CENTER_CROP) {
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
            if (mDoLoadUserCircle) {
                final Bitmap circleBitmap;
                circleBitmap = ImageEditor.cropBitmapCircle(objectIcon);
                imageView.setImageBitmap(circleBitmap);
            } else {
                imageView.setImageBitmap(objectIcon);
            }
        } else if (placeHolderResourceId > 0) {
            // Load the placeholder in an ImageView, if it was referenced.
            if (imageView.getScaleType() != ImageView.ScaleType.CENTER_INSIDE) {
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }
            imageView.setImageResource(placeHolderResourceId);
        }

        // Adjust the load flag to download the high-resolution image,
        // if that one is supposed to be shown in the ImageView.
        if (!mDoLoadHiRes) mDoLoadHiRes = mDoShowHiRes;
        imageView.setTag(R.string.tag_image_view_object_id, mObject.getId());
        mViewReference = new WeakReference<>(imageView);

        if (progressBar != null) mProgressBarReference = new WeakReference<>(progressBar);

        startLoading();
    }

    public void doCropUserCircle() {
        mDoLoadUserCircle = true;
    }

    public void startLoading() {
        if (mObject == null) {
            Log.e(TAG, "No ChatObject given to get image file names from.");
            return;
        }

        mDidFinish = false;

        // Always start a Task that loads the low-res image.
        new ImageLoaderTask().execute(DO_GET_LO_RES_FLAG);

        // If the high-res image is requested, start the appropriate Task.
        if (mDoLoadHiRes) {
            mHiResLoaderTask = new ImageLoaderTask();
            mHiResLoaderTask.execute();
        }
    }

    private class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {

        private boolean mIsLoResLoader = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mObject == null) cancel(true);

            // Show a ProgressBar, if a reference exists.
            if (mProgressBarReference != null) {
                final ProgressBar progressBar = mProgressBarReference.get();
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            mIsLoResLoader = params.length > 0 && params[0].equals(DO_GET_LO_RES_FLAG);

            // If the image does not contain a Bitmap or a high-resolution image was requested,
            // look in the file cache or download the image if needed.
            final String fileId = mObject.getImageFileId(mIsLoResLoader);
            if (TextUtils.isEmpty(fileId)) {
                Log.e(TAG, "No image file ID found.");
                return null;
            }

            // Look for the requested image on the SD card, if a fresh download is not forced.
            final File requestedFile = getFileForId(mContext, fileId);
            if (requestedFile.exists()) {
                final String filePath = requestedFile.getPath();

                if (mDoStartIntent) startViewIntent(filePath);

                final Bitmap storedBitmap;
                try {
                    storedBitmap = BitmapFactory.decodeFile(filePath);
                } catch (OutOfMemoryError e) {
                    Log.e(TAG, e.toString());
                    return null;
                }
                return adjustedResultBitmap(storedBitmap);
            }

            // If the image file does not exist, download the image and store it.
            final Bitmap downloadedBitmap = APIHelper.getImage(mContext, fileId, 0);

            if (downloadedBitmap == null) {
                Log.w(TAG, "Could not open or download " + fileId + ".");
                return null;
            } else {
                storeBitmap(downloadedBitmap, requestedFile, mIsLoResLoader ? 70 : 100);
                if (mDoStartIntent) startViewIntent(requestedFile.getPath());
                if (mIsLoResLoader) mObject.setPreviewImage(downloadedBitmap);
                return adjustedResultBitmap(downloadedBitmap);
            }
        }

        private Bitmap adjustedResultBitmap(final Bitmap bitmap) {
            if (mIsLoResLoader && bitmap != null) mObject.setPreviewImage(bitmap);

            if (mDoLoadUserCircle) {
                return ImageEditor.cropBitmapCircle(bitmap);
            } else {
                return bitmap;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            if (isCancelled() || mDidFinish) return;

//            if (mIsLoResLoader && bitmap != null) mObject.setPreviewImage(bitmap);

            if (mViewReference != null && bitmap != null) {
                if (!mIsLoResLoader) {
                    // Keep the low-res loader from replacing the image in the ImageView
                    // or just do not show the high-res image, if requested.
                    if (mDoShowHiRes) mDidFinish = true;
                    else return;
                }

                // Place the image in an ImageView, if it was referenced.
                final ImageView imageView = mViewReference.get();
                if (imageView != null) {

                    // Do not apply Images from a ChatObject
                    // that has not been associated to this ImageView.
                    // This might occur due to View recycling.
                    final Object viewObjectId = imageView.getTag(R.string.tag_image_view_object_id);
                    if (viewObjectId != null && viewObjectId instanceof Long) {
                        if ((Long) viewObjectId != mObject.getId()) {
                            return;
                        }
                    }

                    // Do no apply lo-res images to ImageViews that already display a hi-res image.
                    if (mIsLoResLoader) {
                        final Object didSetHiRes;
                        didSetHiRes = imageView.getTag(R.string.tag_image_view_set_hi_res);
                        if (didSetHiRes != null && didSetHiRes instanceof Boolean) {
                            if ((Boolean) didSetHiRes) return;
                        }
                    } else {
                        imageView.setTag(R.string.tag_image_view_set_hi_res, true);
                    }

                    if (imageView.getScaleType() != ImageView.ScaleType.CENTER_CROP) {
                        imageView.setImageDrawable(null);
                        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    }

                    Drawable[] layers = new Drawable[2];
                    layers[0] = imageView.getDrawable();
                    layers[1] = new BitmapDrawable(mContext.getResources(), bitmap);

                    if (layers[0] == null) {
                        if (mDoLoadUserCircle) {
                            layers[0] = ContextCompat.getDrawable(
                                    mContext,
                                    R.drawable.shape_user_icon_placeholder
                            );
                        } else {
                            layers[0] = ContextCompat.getDrawable(
                                    mContext,
                                    R.drawable.image_view_background
                            );
                        }
                    }

                    final TransitionDrawable transitionDrawable;
                    transitionDrawable = new TransitionDrawable(layers);
                    imageView.setImageDrawable(transitionDrawable);
                    transitionDrawable.startTransition(800);

                    imageView.setTag(R.string.tag_image_view_last_finished, !mIsLoResLoader);
//                    if (!mIsLoResLoader) {
//                        // Store in this ImageView that it is done with this Image ID.
//                        imageView.setTag(R.string.tag_image_view_image_id, mObject.getImageId());
//                    }
                }
            }

            // Hide the ProgressBar, if a reference exists.
            if (mProgressBarReference != null) {
                ProgressBar progressBar = mProgressBarReference.get();
                if (progressBar == null) return;
                progressBar.setVisibility(View.GONE);
            }

            // If a NotificationBuilder was stored, set the large Icon and show the notification.
            if (mNotificationBuilder != null && !mIsLoResLoader) {
                final Bitmap notificationIcon;
                if (bitmap != null) {
                    notificationIcon = bitmap;
                } else {
                    // If no image has been retrieved, use the Room icon.
                    final Room sessionRoom;
                    sessionRoom = SessionMainManager.getInstance().getRoom(mContext, false);
                    notificationIcon = sessionRoom.getPreviewImage();
                }

//                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
//                    mNotificationBuilder.setLargeIcon(notificationIcon);
//                    final Notification notification = mNotificationBuilder.build();
//                    showNotification(notification);
//                    mDidFinish = true;
//                } else {
                final Bitmap icon;
                icon = ImageEditor.getLargeNotificationIcon(mContext, notificationIcon);
                mNotificationBuilder.setLargeIcon(icon);
                final Notification notification = mNotificationBuilder.build();
                showNotification(notification);
                mDidFinish = true;
//                }
            }
        }
    }


    private void showNotification(final Notification notification) {
        if (notification == null || mNotificationId <= 0 || mContext == null) return;

        try {
            NotificationManager nm;
            nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(mNotificationId, notification);
        } catch (SecurityException e) {
            Log.e(TAG, e.toString());
        }
    }

    public static File getFileForId(Context context, final String fileId) {
        return new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileId);
    }

    public static void storeBitmap(final Bitmap bitmap, final File file, int quality) {
        if (bitmap == null || file == null) return;

        file.delete();
        try {
            if (quality < LO_RES_QUALITY) quality = LO_RES_QUALITY;
            if (file.createNewFile()) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, new FileOutputStream(file));
            }
        } catch (IOException ioEx) {
            Log.w(TAG, file.getName() + ": " + ioEx.toString());
        }
    }

    private void startViewIntent(final String fileId) {
        if (mContext != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse("file://" + fileId), "image/jpeg");
            try {
                mContext.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

}
