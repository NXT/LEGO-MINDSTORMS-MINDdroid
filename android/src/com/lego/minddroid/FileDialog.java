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
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

class FileDialog {

    private Activity myActivity;
    private List<String> myList;    
    private int programNr = -1;
    private CharSequence[] programs;
    
    public FileDialog(Activity activity, List<String> list) {    
        myActivity = activity;
        myList = list;
        // copy Strings from list to CharSequence array
        programs = new CharSequence[myList.size()];
        Iterator<String> iterator = myList.iterator(); 
        int position = 0;
        while(iterator.hasNext()) {
            programs[position++] = iterator.next();
        } 	        
    }    

    /**
     * Shows the dialog
     * @param startStop when true shows another title (for leJOSMINDdroid)
     */    
	public void show(boolean startStop) {        
        AlertDialog.Builder builder = new AlertDialog.Builder(myActivity);
        builder.setTitle(myActivity.getResources().getString(startStop ? R.string.file_dialog_title_1 : R.string.file_dialog_title_2));
        builder.setItems(programs, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                startProgram(item);
            }
        });
        builder.create().show();        
	}
	
	private void startProgram(int number) {
        ((MINDdroid) myActivity).startProgram((String) programs[number]);
	}
		    
}
