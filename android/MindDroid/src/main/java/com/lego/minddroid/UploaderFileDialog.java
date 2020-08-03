/*
 *   Copyright 2011, 2012 Guenther Hoelzl
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
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Builds and shows a file dialog for selecting files
 */
class UploaderFileDialog {

    private final static String DIR_PATH = Environment.getExternalStorageDirectory().getPath();
    private final static String[] NXTG_EXTENSIONS = {".rcd", ".rso", ".ric", ".rxe"};

    private Activity mActivity;
    private DialogListener mDialogListener;
    private String[] programs;
    private int preinstalledFiles;
    private int mRobotType;

    UploaderFileDialog(Activity activity, DialogListener dialogListener, int currentRobotType) {
        mActivity = activity;
        mDialogListener = dialogListener;
        mRobotType = currentRobotType;
    }

    /**
     * Checks whether the given filename ends with an
     * extension used by NXT-G
     *
     * @param filename the name of the file
     * @return if it has an appropriate extension
     */
    private boolean matchNXTGExtension(String filename) {
        for (String nxtgExtension : NXTG_EXTENSIONS) {
            if (filename.toLowerCase(Locale.getDefault()).endsWith(nxtgExtension))
                return true;
        }
        return false;
    }

    /**
     * Refreshes the file list of the directory in DIR_PATH and builds
     * an array.
     *
     * @param preinstalledList list of preinstalled files
     * @return number of files in the directory
     */
    int refreshFileList(String[] preinstalledList) {
        ArrayList<String> fileList = new ArrayList<>();

        // internal files
        preinstalledFiles = preinstalledList.length;
        for (int index = 0; index < preinstalledFiles; index++) {
            if (mRobotType == R.id.robot_type_lejos) {
                if (matchNXTGExtension(preinstalledList[index]))
                    continue;
            } else {
                if (!matchNXTGExtension(preinstalledList[index]))
                    continue;
            }
            fileList.add(preinstalledList[index]);
        }
        preinstalledFiles = fileList.size();

        // external files
        File file = new File(DIR_PATH);
        File[] files = file.listFiles();
        if (files != null) {
            for (File value : files) {
                if (value.isDirectory())
                    continue;
                String fileName = value.getName();
                if (mRobotType == R.id.robot_type_lejos) {
                    if (matchNXTGExtension(fileName))
                        continue;
                } else {
                    if (!matchNXTGExtension(fileName))
                        continue;
                }
                fileList.add(fileName);
            }
        }
        programs = new String[fileList.size()];
        programs = fileList.toArray(programs);
        return programs.length;
    }

    /**
     * Shows the dialog
     */
    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(mActivity.getResources().getString(R.string.uul_file_dialog_title));
        builder.setItems(programs, (dialog, item) -> informDialogListener(item, programs[item]));
        builder.create().show();
    }

    /**
     * Informs the calling activity about the new selected file name
     *
     * @param item     nr in the list of the selected item
     * @param fileName the name of the file
     */
    private void informDialogListener(int item, String fileName) {
        if (item >= preinstalledFiles)
            fileName = DIR_PATH.concat(fileName);
        mDialogListener.dialogUpdate(fileName);
    }

}
