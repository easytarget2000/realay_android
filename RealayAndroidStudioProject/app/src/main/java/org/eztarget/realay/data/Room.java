package org.eztarget.realay.data;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by michel on 15/11/14.
 */
public class Room extends ChatObject {

    public static final String ADDRESS_PLAYSTORE_REDIRECT = "PlayStore";

    private String mTitle;

    private String mDescription;

    private String mCreator;

    private String mAddress;

    private LatLng mLatLng;

    private int mDistance;

    private int mRadius;

    private String mPassword;

    private int mNumOfUsers;

    private long mStartDateSec;

    private long mEndDateSec;

    public Room(
            final long roomId,
            final long imageId,
            final String title,
            final String description,
            final String creator,
            final String address,
            final double latitude,
            final double longitude,
            final int distance,
            final int radius,
            final String password,
            final int numOfUsers,
            final long startDateSec,
            final long endDateSec
    ) {
        mId = roomId;
        mImageId = imageId;
        mTitle = title;
        mDescription = description;
        mCreator = creator;
        mAddress = address != null ? address.trim() : null;
        mLatLng = new LatLng(latitude, longitude);
        mDistance = distance;
        mRadius = radius;
        mPassword = password;
        mNumOfUsers = numOfUsers;
        mStartDateSec = startDateSec;
        mEndDateSec = endDateSec;
    }

    @Override
    public String toString() {
        return mTitle + ": " + mDistance + "m, " + mRadius + "m pw: " + mPassword;
    }

    public int getDistance() {
        return mDistance;
    }

    public void setDistance(int distance) {
        mDistance = distance;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getCreator() {
        return mCreator;
    }

    public LatLng getLatLng() {
        return mLatLng;
    }

    public String getAddress() {
        return mAddress;
    }

    public int getRadius() {
        if (mRadius < 20) return 50;
        else return mRadius;
    }

    public String getPassword() {
        return mPassword;
    }

    public int getNumOfUsers() {
        return mNumOfUsers;
    }

    public long getStartDateSec() {
        return mStartDateSec;
    }

    public long getEndDateSec() {
        return mEndDateSec;
    }

}
