/**
 *   Copyright 2011 Guenther Hoelzl
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
import java.io.File;
import java.util.ArrayList;

/**
 * Builds and shows a file dialog for selecting nxj-files
 * @return number of nxj-files in the directory
 */    
class NXJFileDialog {

    private final static String DIR_PATH = "/sdcard/download/";
    private final static String EXTENSION = ".nxj";

    private Activity mActivity;
    private DialogListener mDialogListener;
    private String[] programs;
    private int preinstalledFiles;
    
    public NXJFileDialog(Activity activity, DialogListener dialogListener) {    
        mActivity = activity;
        mDialogListener = dialogListener;
    }  

    /**
     * Refreshes the file list of the directory in DIR_PATH and builds
     * an array.
     * @param preinstalledList list of preinstalled nxj-files
     * @return number of nxj-files in the directory
     */    
    public int refreshFileList(String[] preinstalledList) {
        ArrayList<String> nxjPrograms = new ArrayList();

        // internal nxj-files
        preinstalledFiles = preinstalledList.length;
        for (int index = 0; index < preinstalledFiles; index++)
            nxjPrograms.add(preinstalledList[index]);

        // external nxj-files
        File file = new File(DIR_PATH);
        if (file != null) {
            File[] files = file.listFiles();
            if (files != null) {
                for (int fileNr = 0; fileNr < files.length; fileNr++) {           
                    if (files[fileNr].getName().toLowerCase().endsWith(EXTENSION)) 
                        nxjPrograms.add(files[fileNr].getName());
                }
            }
        }
        programs = new String[nxjPrograms.size()];
        programs = nxjPrograms.toArray(programs);
        return programs.length;
    }

    /**
     * Shows the dialog
     */    
	public void show() {        
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(mActivity.getResources().getString(R.string.nxj_file_dialog_title));
        builder.setItems(programs, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                informDialogListener(item, programs[item]);                
            }
        });
        builder.create().show();        
	}
	
    /**
     * Informs the calling activity about the new selected file name
     * @param item nr in the list of the selected item
     * @param fileName the name of the file
     */    
	public void informDialogListener(int item, String fileName) {
        if (item >= preinstalledFiles)
            fileName = DIR_PATH.concat(fileName);
        mDialogListener.dialogUpdate(fileName);
	}
		    
}
