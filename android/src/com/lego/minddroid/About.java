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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

class About extends Activity {

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

		dialog.show();

	}
	
}
