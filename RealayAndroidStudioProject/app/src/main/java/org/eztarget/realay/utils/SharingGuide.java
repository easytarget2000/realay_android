package org.eztarget.realay.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.text.TextUtils;

import org.eztarget.realay.R;
import org.eztarget.realay.data.Room;
import org.eztarget.realay.managers.SessionMainManager;

/**
 * Created by michel on 25/02/15.
 */
public class SharingGuide {

    private static final String WEBSITE_URL = "http://realay.net";

    public static void showLanguageDialog(final Activity activity) {
        if (activity == null) return;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setTitle(R.string.select_sharing_language);
        dialogBuilder.setSingleChoiceItems(
                R.array.language_choice_labels,
                -1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (dialogInterface != null) dialogInterface.dismiss();

                        final Room room = SessionMainManager.getInstance().getRoom(activity, false);
                        if (room == null) return;
                        final String roomTitle = room.getTitle();
                        if (TextUtils.isEmpty(roomTitle)) return;

                        final Resources resources = activity.getResources();

                        final String[] languageCodes;
                        languageCodes = resources.getStringArray(R.array.language_choice_ids);

                        // Match the label Array selection to the code Array.
                        // Both Arrays are sorted equally, based on their localisation.
                        final int messageResourceId;
                        if (i < languageCodes.length) {
                            final String de = resources.getString(R.string.language_code_german);
                            if (languageCodes[i].equals(de)) {
                                messageResourceId = R.string.using_realay_at_ger;
                            } else {
                                messageResourceId = R.string.using_realay_at;
                            }
                        } else {
                            messageResourceId = R.string.using_realay_at;
                        }

                        // Build the message.
                        final String formatString = resources.getString(messageResourceId);
                        final String text = String.format(formatString, roomTitle, WEBSITE_URL);

                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
                        sendIntent.setType("text/plain");
                        activity.startActivity(sendIntent);
                    }
                }
        );

        dialogBuilder.show();
    }

}
