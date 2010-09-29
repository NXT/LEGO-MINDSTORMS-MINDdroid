/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lego.minddroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Closeable;

/**
 * Displays an Info text asset
 * This class was build from 
 * http://code.google.com/p/apps-for-android/source/browse/trunk/DivideAndConquer/src/com/google/android/divideandconquer/Eula.java?r=93
 */
class Info {
    private static final String ASSET_INFO = "INFO";

    /**
     * callback to let the activity know when the user has accepted the INFO.
     */
    static interface OnInfoAgreedTo {

        /**
         * Called when the user has accepted the info and the dialog closes.
         */
        void onInfoAgreedTo();
    }

    /**
     * Displays the INFO if necessary. This method should be called from the onCreate()
     * method of your main Activity.
     *
     * @param activity The Activity to finish if the user rejects the INFO.
     * @return Whether the user has agreed already.
     */
    static boolean show(final Activity activity) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.info);
        builder.setIcon(R.drawable.minddroid_logo);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.info_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (activity instanceof OnInfoAgreedTo) {
                    ((OnInfoAgreedTo) activity).onInfoAgreedTo();
                }
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
            }
        });
        builder.setMessage(readInfo(activity));
        builder.create().show();
        return false;

    }

    private static CharSequence readInfo(Activity activity) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(activity.getAssets().open(ASSET_INFO)));
            String line;
            StringBuilder buffer = new StringBuilder();
            while ((line = in.readLine()) != null) buffer.append(line).append('\n');
            return buffer;
        } catch (IOException e) {
            return "";
        } finally {
            closeStream(in);
        }
    }

    /**
     * Closes the specified stream.
     *
     * @param stream The stream to close.
     */
    private static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}
