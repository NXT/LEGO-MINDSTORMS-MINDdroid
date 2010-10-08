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
    private Activity myActivity;
    // the following array holds the scenes, each scene consists of the following properties
    // layout, text/image#1, text/image#2, text/image#3
    private int[] sceneProperties = new int[] { 
        R.layout.tutorial_01, R.string.tutorial_welcome_droid, 0, 0, 
        R.layout.tutorial_02, R.drawable.tutorial_01, 0, 0,
        R.layout.tutorial_03, R.string.tutorial_1, 0, 0, 
        R.layout.tutorial_02, R.drawable.tutorial_02, 0, 0,
        R.layout.tutorial_04, 0, 0, 0,
        R.layout.tutorial_02, R.drawable.tutorial_03, 0, 0,
        R.layout.tutorial_03, R.string.tutorial_5, 0, 0,
        R.layout.tutorial_02, R.drawable.tutorial_04, 0, 0,
        R.layout.tutorial_03, R.string.tutorial_6, 0, 0,        
        R.layout.tutorial_02, R.drawable.tutorial_05, 0, 0,
    };
   
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
	
	    int layout = sceneProperties[currentScene*4];
	    int resource0 = sceneProperties[currentScene*4+1];   
	    int resource1 = sceneProperties[currentScene*4+2];   
   	    int resource2 = sceneProperties[currentScene*4+3];   	    
	    // rehide the current dialog shortly	    
        if (dialog.isShowing())
            dialog.dismiss();        
		dialog.setContentView(layout);
		switch (layout) {
            case R.layout.tutorial_01:
		    case R.layout.tutorial_03:
        	    TextView text = (TextView) dialog.findViewById(R.id.TutorialTextView);
        	    text.setText(myActivity.getResources().getString(resource0));
        	case R.layout.tutorial_04:
            	Button buttonOK = (Button) dialog.findViewById(R.id.nextButton);
            	buttonOK.setOnClickListener(new OnClickListener() {
            		public void onClick(View v)
            		{
            		    currentScene++;
                        setNewContent();
            		}
            	});
            	break;

            case R.layout.tutorial_02:
                ImageView image = (ImageView) dialog.findViewById(R.id.TutorialImageView);
                image.setImageResource(resource0);
            	image.setOnClickListener(new OnClickListener() {
            		public void onClick(View v)
            		{
            		    currentScene++;
         		        setNewContent();
			        }    
            	});  
			    break;
        }
        dialog.show();        	            	    
	}	
}
