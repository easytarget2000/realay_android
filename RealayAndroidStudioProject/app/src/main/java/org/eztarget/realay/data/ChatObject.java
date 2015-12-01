package org.eztarget.realay.data;

import android.graphics.Bitmap;

/**
 *
 *
 */
public class ChatObject {

    protected long mId = -1;
    protected Bitmap mPreviewImage;
    protected long mImageId;

    public long getId() {
        return mId;
    }

    public Bitmap getPreviewImage() {
        return mPreviewImage;
    }

    public void setPreviewImage(final Bitmap previewImage) {
        mPreviewImage = previewImage;
    }

    public void setImageId(final long imageId) {
        mImageId = imageId;
        mPreviewImage = null;
    }

    public long getImageId() {
        return mImageId;
    }

    /**
     * Builds an image ID. The ID + ".jpg" is the actual file name.
     * This file is stored on the server and/or locally.
     * The image ID is built from the Object class and the Object database ID.
     *
     * @return Image ID / file name
     */
    public String getImageFileId(final boolean doGetLoRes) {
        if (mImageId < 100L) return null;
        final String id = String.valueOf(mImageId);
        return doGetLoRes ? id + "s.jpg" : id + ".jpg";
    }

}
