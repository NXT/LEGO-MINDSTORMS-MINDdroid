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

import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

class FileDialog {

    private final Activity myActivity;
    private final CharSequence[] programs;
    
    public FileDialog(Activity activity, List<String> list) {    
        myActivity = activity;
        // copy Strings from list to CharSequence array
        programs = new CharSequence[list.size()];
        Iterator<String> iterator = list.iterator();
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
