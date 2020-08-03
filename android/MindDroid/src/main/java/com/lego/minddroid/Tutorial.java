/*
 * Copyright 2010, 2011, 2012 Guenther Hoelzl, Shawn Brown
 * <p>
 * This file is part of MINDdroid.
 * <p>
 * MINDdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * MINDdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with MINDdroid.  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.lego.minddroid;

import android.app.Activity;
import android.app.Dialog;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

class Tutorial {

    private Dialog dialog;
    private int currentScene = 0;
    private Activity myActivity;
    // the following array holds the scenes, each scene consists of the following properties
    // layout, text/image#1, text/image#2, text/image#3
    private int[] sceneProperties = new int[]{
            R.layout.tutorial_01, R.string.tutorial_welcome_droid, 0, 0,
            R.layout.tutorial_02, R.drawable.tutorial_01, R.string.tutorial_bubble_01, 0,
            R.layout.tutorial_03, R.string.tutorial_a, 0, 0,
            R.layout.tutorial_02, R.drawable.tutorial_02, R.string.tutorial_bubble_02, 0,
            R.layout.tutorial_04, 0, 0, 0,
            R.layout.tutorial_02, R.drawable.tutorial_03, R.string.tutorial_bubble_03, 0,
            R.layout.tutorial_03, R.string.tutorial_e, 0, 0,
            R.layout.tutorial_02, R.drawable.tutorial_04, R.string.tutorial_bubble_04, 0,
            R.layout.tutorial_03, R.string.tutorial_f, 0, 0,
            R.layout.tutorial_02, R.drawable.tutorial_05, R.string.tutorial_bubble_05, 0,
    };

    Tutorial() {
    }

    public void show(final Activity myActivity) {
        this.myActivity = myActivity;
        dialog = new Dialog(myActivity);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setNewContent();

        dialog.show();
    }

    public void setNewContent() {
        // end of the show
        if (currentScene >= (sceneProperties.length / 4)) {
            dialog.dismiss();
            return;
        }

        TextView myTextView;
        ImageView myImageView;

        int layout = sceneProperties[currentScene * 4];
        int resource0 = sceneProperties[currentScene * 4 + 1];
        int resource1 = sceneProperties[currentScene * 4 + 2];
        // rehide the current dialog shortly
        if (dialog.isShowing())
            dialog.dismiss();
        dialog.setContentView(layout);
        if (layout == R.layout.tutorial_01 || layout == R.layout.tutorial_03) {
            myTextView = dialog.findViewById(R.id.TutorialTextView);
            myTextView.setText(myActivity.getResources().getString(resource0));

            Button buttonOK = dialog.findViewById(R.id.nextButton);
            buttonOK.setOnClickListener(v -> {
                currentScene++;
                setNewContent();
            });
        } else if (layout == R.layout.tutorial_04) {
            Button buttonOK = dialog.findViewById(R.id.nextButton);
            buttonOK.setOnClickListener(v -> {
                currentScene++;
                setNewContent();
            });
        } else if (layout == R.layout.tutorial_02) {
            myImageView = dialog.findViewById(R.id.TutorialImageView);
            myImageView.setImageResource(resource0);
            myImageView.setOnClickListener(v -> {
                currentScene++;
                setNewContent();
            });

            myTextView = dialog.findViewById(R.id.TutorialTextView);
            myTextView.setTextSize(20);
            myTextView.setText(myActivity.getResources().getString(resource1));
        }
        dialog.show();
    }
}

