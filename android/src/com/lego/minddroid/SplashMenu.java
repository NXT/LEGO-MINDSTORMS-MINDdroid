/**
 * Copyright 2010 Guenther Hoelzl, Shawn Brown
 *
 * This file is part of MINDdroid.
 *
 * MINDdroid is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * MINDdroid is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MINDdroid. If not, see <http://www.gnu.org/licenses/>.
 **/

package com.lego.minddroid;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

public class SplashMenu extends Activity {

    public static final int MENU_OPTIONS = Menu.FIRST;
    public static final int MENU_UPLOAD = Menu.FIRST + 1;
    public static final int MENU_ABOUT = Menu.FIRST + 2;
    public static final int MENU_QUIT = Menu.FIRST + 3;
    public static final String MINDDROID_PREFS = "Mprefs";
    public static final String MINDDROID_ROBOT_TYPE = "MrobotType";
    private int mRobotType;


    public static void quitApplication() {
        if (MINDdroid.isBtOnByUs() || NXJUploader.isBtOnByUs()) {
            BluetoothAdapter.getDefaultAdapter().disable();
            MINDdroid.setBtOnByUs(false);
            NXJUploader.setBtOnByUs(false);
        }
        splashMenu.finish();

    }

    private View splashMenuView;

    private static Activity splashMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // show Lama and write nxj-files to SD-card
        Lama.show(this);
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        mRobotType=lookupRobotType();
        splashMenuView = new SplashMenuView(getApplicationContext(), this);
        setContentView(splashMenuView);
        splashMenu = this;
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if (MINDdroid.isBtOnByUs() || NXJUploader.isBtOnByUs()) {
            BluetoothAdapter.getDefaultAdapter().disable();
            MINDdroid.setBtOnByUs(false);
            NXJUploader.setBtOnByUs(false);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
    }

    /**
     * Creates the menu items
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_OPTIONS, 1, getResources().getString(R.string.options)).setIcon(R.drawable.ic_menu_preferences);
        menu.add(0, MENU_UPLOAD, 2, getResources().getString(R.string.upload)).setIcon(R.drawable.ic_menu_nxj);
        menu.add(0, MENU_ABOUT, 3, getResources().getString(R.string.about)).setIcon(R.drawable.ic_menu_about);
        menu.add(0, MENU_QUIT, 4, getResources().getString(R.string.quit)).setIcon(R.drawable.ic_menu_exit);
        return true;
    }

    /**
     * Enables/disables the menu items
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean displayMenu;
        displayMenu = super.onPrepareOptionsMenu(menu);
        if (displayMenu) {
            menu.getItem(1).setEnabled(getRobotType() == R.id.robot_type_4);
        }
        return displayMenu;
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
                Intent nxjUpload = new Intent(this.getBaseContext(), NXJUploader.class);
                this.startActivity(nxjUpload);
                return true;
            case MENU_ABOUT:
                About about = new About();
                about.show(this);
                return true;
            case MENU_QUIT:
                finish();
                return true;
        }
        return false;
    }

    public void setRobotType(int mRobotType) {
        SharedPreferences mUserPreferences = getSharedPreferences(MINDDROID_PREFS, Context.MODE_PRIVATE);
        Editor mPrefsEditor = mUserPreferences.edit();
        mPrefsEditor.putInt(MINDDROID_ROBOT_TYPE, mRobotType);
        mPrefsEditor.commit();
        this.mRobotType = mRobotType;
    }

    public int lookupRobotType() {
        SharedPreferences mUserPreferences;
        mUserPreferences =  getSharedPreferences(MINDDROID_PREFS, Context.MODE_PRIVATE);
        return mUserPreferences.getInt(MINDDROID_ROBOT_TYPE, R.id.robot_type_1);
    }

    public int getRobotType() {
        return mRobotType;
    }

}
