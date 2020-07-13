/*
 * Copyright 2010, 2011, 2012 Guenther Hoelzl, Shawn Brown
 * <p>
 * This file is part of MINDdroid.
 * <p>
 * MINDdroid is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * MINDdroid is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * MINDdroid. If not, see <http://www.gnu.org/licenses/>.
 **/

package com.lego.minddroid;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import info.hannes.github.AppUpdateHelper;

public class SplashMenu extends AppCompatActivity {

    public static final int MENU_OPTIONS = Menu.FIRST;
    public static final int MENU_UPLOAD = Menu.FIRST + 1;
    public static final int MENU_ABOUT = Menu.FIRST + 2;
    public static final int MENU_QUIT = Menu.FIRST + 3;
    public static final String MINDDROID_PREFS = "Mprefs";
    public static final String MINDDROID_ROBOT_TYPE = "MrobotType";
    private int mRobotType;

    public static void quitApplication() {
        if (MINDdroid.isBtOnByUs() || UniversalUploader.isBtOnByUs()) {
            BluetoothAdapter.getDefaultAdapter().disable();
            MINDdroid.setBtOnByUs(false);
            UniversalUploader.setBtOnByUs(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Lama.show(this);
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        mRobotType = lookupRobotType();
        View splashMenuView = new SplashMenuView(getApplicationContext(), this);
        setContentView(splashMenuView);

        AppUpdateHelper.INSTANCE.checkForNewVersion(this, BuildConfig.GIT_USER, BuildConfig.GIT_REPOSITORY, BuildConfig.VERSION_NAME);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if (MINDdroid.isBtOnByUs() || UniversalUploader.isBtOnByUs()) {
            BluetoothAdapter.getDefaultAdapter().disable();
            MINDdroid.setBtOnByUs(false);
            UniversalUploader.setBtOnByUs(false);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Creates the menu items
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_OPTIONS, 1, getResources().getString(R.string.options)).setIcon(R.drawable.ic_menu_preferences);
        menu.add(0, MENU_UPLOAD, 2, getResources().getString(R.string.upload)).setIcon(R.drawable.ic_menu_file);
        menu.add(0, MENU_ABOUT, 3, getResources().getString(R.string.about)).setIcon(R.drawable.ic_menu_about);
        menu.add(0, MENU_QUIT, 4, getResources().getString(R.string.quit)).setIcon(R.drawable.ic_menu_exit);
        return true;
    }

    /**
     * Handles item selections
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_OPTIONS:
                Options options = new Options(this);
                options.show();
                return true;
            case MENU_UPLOAD:
                Intent uulIntent = new Intent(this.getBaseContext(), UniversalUploader.class);
                uulIntent.putExtra("robotType", Integer.valueOf(mRobotType));
                this.startActivity(uulIntent);
                return true;
            case MENU_ABOUT:
                showAbout();
                return true;
            case MENU_QUIT:
                finish();
                return true;
        }
        return false;
    }

    private void showAbout() {
        Dialog dialog = new Dialog(this);
        if (dialog.getWindow() != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.aboutbox);

            Button buttonOK = dialog.findViewById(R.id.AboutOKbutton);
            buttonOK.setOnClickListener(v -> dialog.dismiss());

            dialog.show();
        }
    }

    public void setRobotType(int mRobotType) {
        Editor mPrefsEditor = getSharedPreferences(MINDDROID_PREFS, Context.MODE_PRIVATE).edit();
        mPrefsEditor.putInt(MINDDROID_ROBOT_TYPE, mRobotType);
        mPrefsEditor.apply();
        this.mRobotType = mRobotType;
    }

    public int lookupRobotType() {
        return getSharedPreferences(MINDDROID_PREFS, Context.MODE_PRIVATE).getInt(MINDDROID_ROBOT_TYPE, R.id.robot_type_shooterbot);
    }

    public int getRobotType() {
        return mRobotType;
    }

}
