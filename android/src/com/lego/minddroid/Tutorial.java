/**
 *   Copyright 2010, 2011, 2012 Guenther Hoelzl, Shawn Brown
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
import android.content.Context;
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

import android.util.Log;

class Tutorial {

    private Dialog dialog;
    private int currentScene = 0;
    private Activity myActivity;
    private int myScreenWidth;
    private int myScreenHeight;
    
    // the following array holds the scenes, each scene consists of the following properties
    // layout, text/image#1, text/image#2, text/image#3
    private int[] sceneProperties = new int[] { 
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
    
    public Tutorial(int screenWidth, int screenHeight) {
        myScreenWidth = screenWidth;
        myScreenHeight = screenHeight;
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
        	    myTextView = (TextView) dialog.findViewById(R.id.TutorialTextView);
        	    myTextView.setText(myActivity.getResources().getString(resource0));
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
                myImageView = (ImageView) dialog.findViewById(R.id.TutorialImageView);                               
                myImageView.setImageResource(resource0);
            	myImageView.setOnClickListener(new OnClickListener() {
            		public void onClick(View v)
            		{
            		    currentScene++;
         		        setNewContent();
			        }    
            	});  
            	
                myTextView = (TextView) dialog.findViewById(R.id.TutorialTextView);
                myTextView.setTextSize(20);
        	    myTextView.setText(myActivity.getResources().getString(resource1));    
        	          	              
			    break;
        }
        dialog.show();        	            	    
	}	
}

