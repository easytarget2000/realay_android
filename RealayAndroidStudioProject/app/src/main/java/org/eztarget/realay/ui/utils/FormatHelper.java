package org.eztarget.realay.ui.utils;

import android.content.Context;
import android.text.format.DateUtils;

import org.eztarget.realay.R;
import org.eztarget.realay.data.Room;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by michel on 02/07/15.
 */
public class FormatHelper {

    private static final float YARD_IN_METRE = 1.09361f;

    private static final float MILE_IN_METRE = 0.000621371f;

    private static final int MAX_DISPLAY_FLOAT_MILE = 6000;

    private static final int MAX_DISPLAY_METRE = 2500;

    private static final int MAX_DISPLAY_YARD = 500;

    public static String buildRoomDistance(
            final Context context,
            final Room room,
            final boolean doUseImperial
    ) {
        return buildLength(context, room.getDistance(), doUseImperial);
    }

    public static String buildRoomSize(
            final Context context,
            final Room room,
            final boolean doUseImperial
    ) {
        return buildLength(context, room.getRadius() * 2, doUseImperial);
    }

    private static String buildLength(
            Context context,
            final int metres,
            final boolean doUseImperial
    ) {
        if (context == null) return "-";

        if (!doUseImperial) {
            if (metres < MAX_DISPLAY_METRE) return metres + " m";
            else return (metres / 1000) + " km";
        } else {
            if (metres < MAX_DISPLAY_YARD) {
                return ((int) (metres * YARD_IN_METRE)) + " yd";
            } else if (metres < MAX_DISPLAY_FLOAT_MILE) {
                return String.format("%.1f mi", (metres * MILE_IN_METRE));
            } else {
                return ((int) (metres * MILE_IN_METRE)) + " mi";
            }
        }
    }

    /*
    Dates
     */

    public static String buildRoomStartHour(Context context, final Room room) {
        if (context == null || room == null) return "-";

        final long startDateSec = room.getStartDateSec();
        if (startDateSec < 10000L || startDateSec * 1000L < System.currentTimeMillis()) {
            return context.getResources().getString(R.string.ongoing);
        } else {
            return buildDateTime(startDateSec);
        }
    }

    public static String buildRoomEndHour(
            Context context,
            final Room room,
            final boolean doAddUntil
    ) {
        if (context == null || room == null) return "-";

        if (room.getEndDateSec() < 10000L) {
                return context.getString(R.string.no_time_restriction);
            } else {
                final String end = buildDateTime(room.getEndDateSec());
                if (end == null) return "";

                if (doAddUntil) return context.getResources().getString(R.string.until) + " " + end;
                else return end;
            }
    }

    public static String buildDateTime(final long seconds) {
        final long timestamp = seconds * 1000L;

        // Get the date that is in about 12 hours from now.
        final long now = System.currentTimeMillis();
        final long twelveHours = (DateUtils.DAY_IN_MILLIS / 2L);
        final long todayFuture = now + twelveHours;
        final long todayPast = now - twelveHours;
        final long weekFuture = now + (DateUtils.DAY_IN_MILLIS * 4L);

        if (timestamp > todayPast && timestamp < todayFuture) {
            // Anything within the past 12h and the next 12h is considered "today"
            // to avoid confusion and shorten text for events ending in the early morning.
            return getHoursMinutes(seconds);
        }

        final Date date = new Date(timestamp);
        final DateFormat hoursMinutesFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

        if (timestamp < weekFuture) {
            // Display only the day of the week
            // for anything after today and within the next couple of days.
            final DateFormat dayFormat = new SimpleDateFormat("EEEE");
            return dayFormat.format(date) + " - " + hoursMinutesFormat.format(date);
        } else {
            final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL);
            return dateFormat.format(date) + " - " + hoursMinutesFormat.format(date);
        }
    }

    private static String getHoursMinutes(final long timestampSec) {
        final DateFormat hoursMinutesFormat;
        hoursMinutesFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        final Date date = new Date(timestampSec * 1000L);
        return hoursMinutesFormat.format(date);
    }

//    public String buildSizeString(Context context) {
//        if (context == null) return "---";
//        return buildLength(context, mRadius * 2);
//    }

}
