/**
 *   Copyright 2010 Guenther Hoelzl, Shawn Brown
 *
 *   This file is part of MINDdroid.
 *
 *   MINDdroid is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   MINDdroid is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with MINDdroid.  If not, see <http://www.gnu.org/licenses/>.
**/

package com.lego.minddroid;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Closeable;
import java.util.regex.Pattern;

class Tutorial {

    private Dialog dialog;
    private int currentScene = 0;
    private ImageView image;
    private int[] resourceID = new int[] { 
        R.drawable.tutorial_01,
        R.drawable.tutorial_02,
        R.drawable.tutorial_03,        
        R.drawable.tutorial_04,
        R.drawable.tutorial_05
    };
   
	public void show(final Activity myActivity) {
		dialog = new Dialog(myActivity);
		dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.tutorial_image);

        image = (ImageView) dialog.findViewById(R.id.TutorialImageView);
        image.setImageResource(resourceID[currentScene]);
    	image.setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    		    if (++currentScene == 5)
        			dialog.dismiss();
        		else {
        		    image.setImageResource(resourceID[currentScene]);
				}    

    		}
    	});

		dialog.show();

	}
	
}
