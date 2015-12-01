package org.eztarget.realay.ui.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by michel on 30/11/14.
 */
public class MediaPickHelper {

    private static final String TAG = MediaPickHelper.class.getSimpleName();

    public static final int PICKED_IMAGE_SIZE = 1080;

    public static final int IMAGE_STATE_ORIGINAL = 1;

    public static final int IMAGE_STATE_READY = 2;

    private static final String TEMP_PHOTO_FILE = "temp_crop_me.jpg";

    public static Intent getPhotoIntent(Context context, final boolean doShowGallery) {
        if (context == null) return null;

        // Clear the temp file just to make sure that no old sharing attempts are in there.
        getTempFile(context, IMAGE_STATE_ORIGINAL).delete();

        final Intent photoCropIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        photoCropIntent.putExtra(
                MediaStore.EXTRA_OUTPUT,
                getTempUri(context, IMAGE_STATE_ORIGINAL)
        );

        if (doShowGallery) photoCropIntent.setType("image/*");

        final List<ResolveInfo> list;
        list = context.getPackageManager().queryIntentActivities(photoCropIntent, 0);
        // Provide a fallback photo picker in case the Intent cannot be started on this device.
        if (list.size() < 1) {
            photoCropIntent.setAction(Intent.ACTION_PICK);
            photoCropIntent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            if (doShowGallery) photoCropIntent.setType("image/*");
        }

        return photoCropIntent;
    }

    public static Intent buildCropIntent(
            Context context,
            final Intent data,
            final Uri fileUri,
            final boolean doCropSquared
    ) {
        final Intent cropIntent = new Intent("com.android.camera.action.CROP");

        if (getTempFile(context, IMAGE_STATE_ORIGINAL).exists()) {
            cropIntent.setDataAndType(getTempUri(context, 1), "image/*");
            Log.d(TAG, "Cropping image in temporary file.");
        } else if (fileUri != null) {
            cropIntent.setDataAndType(fileUri, "image/*");
            Log.d(TAG, "Cropping image at given URI.");
        } else if (data.getData() != null) {
            cropIntent.setDataAndType(data.getData(), "image/*");
            Log.d(TAG, "Cropping image in Intent Data.");
        } else {
            Log.e(TAG, "No image to be cropped found.");
            return null;
        }

        cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, getTempUri(context, 2));
        cropIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        cropIntent.putExtra("return-data", false);

        cropIntent.putExtra("crop", true);
        if (doCropSquared) {
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            cropIntent.putExtra("outputX", PICKED_IMAGE_SIZE);
            cropIntent.putExtra("outputY", PICKED_IMAGE_SIZE);
            cropIntent.putExtra("scaleUpIfNeeded", true);
        }

        // Provide a fallback photo picker in case the crop Intent cannot be started on this device.
        List<ResolveInfo> list;
        list = context.getPackageManager().queryIntentActivities(cropIntent, 0);
        if (list.size() < 1) {
            cropIntent.setAction(Intent.ACTION_PICK);
            cropIntent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, getTempUri(context, 1));
            cropIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        }

        return cropIntent;
    }

    public static Bitmap readTempFile(Context context) {
        File tempFile = getTempFile(context, 2);

        if (!tempFile.exists()) {
            tempFile = getTempFile(context, 1);
            if (!tempFile.exists()) return null;
        }

        // The Intent stored the file already. Just decode the temporary file and delete it.
        final String tempPath = tempFile.getAbsolutePath();

        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeFile(tempPath);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, e.toString());
        }

        if (!tempFile.delete()) Log.w(TAG, "Could not delete " + tempPath + ".");
        return bitmap;
    }

    private static Uri getTempUri(Context context, final int number) {
        return Uri.fromFile(getTempFile(context, number));
    }

    public static File getTempFile(Context context, final int state) {
        return new File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                String.valueOf(state) + TEMP_PHOTO_FILE
        );
    }

    public static String getPathFromUri(final Context context, final Uri contentUri) {
        if (context == null || contentUri == null) return "";

        final String[] projection = {MediaStore.Images.Media.DATA};
        final CursorLoader loader;
        loader = new CursorLoader(context, contentUri, projection, null, null, null);

        final Cursor cursor = loader.loadInBackground();
        final int dataIndex;
        try {
            dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        } catch (IllegalArgumentException e) {
            cursor.close();
            Log.e(TAG, e.toString());
            return null;
        }

        cursor.moveToFirst();
        final String path = cursor.getString(dataIndex);
        Log.d(TAG, "Path: " + path + "; From URI: " + contentUri);
        cursor.close();
        return path;
    }

    public static void streamIntoFile(final InputStream inputStream, final File destination) {
        if (inputStream == null || destination == null) {
            Log.e(TAG, "streamIntoFile() called with null parameter.");
            return;
        }

        final OutputStream outputStream;
        try {
            outputStream = new FileOutputStream(destination);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        // Transfer bytes from in to out
        byte[] buffer = new byte[1024];

        int length;
        try {
            while ((length = inputStream.read(buffer)) > 0) outputStream.write(buffer, 0, length);

            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void copyFile(final File source, final File destination) {
        final InputStream inputStream;
        try {
            inputStream = new FileInputStream(source);
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.toString());
            return;
        }
        streamIntoFile(inputStream, destination);
    }

}
