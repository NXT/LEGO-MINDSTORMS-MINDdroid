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
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;


import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Closeable;
import java.util.regex.Pattern;

class About {

    private Dialog dialog;
    
	public void show(Activity myActivity) {
		dialog = new Dialog(myActivity);
		dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.aboutbox);

    	Button buttonOK = (Button) dialog.findViewById(R.id.AboutOKbutton);
    	buttonOK.setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			dialog.dismiss();
    		}
    	});

//		final TextView peopleTextView = (TextView) dialog.findViewById(R.id.AboutPeople);
//		Linkify.addLinks(peopleTextView, Pattern.compile("LEGO"), "http://www.lego.com");

		dialog.show();

	}
	
}
