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
package com.lego.minddroid

import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.lego.minddroid.MINDdroid.Companion.isBtOnByUs
import com.lego.minddroid.MINDdroid.Companion.setBtOnByUs

class SplashMenu : AppCompatActivity() {

    private var mRobotType = 0

    var robotType: Int
        get() = mRobotType
        set(mRobotType) {
            val mPrefsEditor = getSharedPreferences(MINDDROID_PREFS, MODE_PRIVATE).edit()
            mPrefsEditor.putInt(MINDDROID_ROBOT_TYPE, mRobotType)
            mPrefsEditor.apply()
            this.mRobotType = mRobotType
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        Lama.show(this)
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_NO_TITLE)
        mRobotType = lookupRobotType()
        val splashMenuView: View = SplashMenuView(applicationContext, this)
        setContentView(splashMenuView)
    }

    override fun onPause() {
        if (isBtOnByUs() || UniversalUploader.isBtOnByUs()) {
            BluetoothAdapter.getDefaultAdapter().disable()
            setBtOnByUs(false)
            UniversalUploader.setBtOnByUs(false)
        }
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, MENU_OPTIONS, 1, resources.getString(R.string.options))
            .setIcon(R.drawable.ic_menu_preferences)
        menu.add(0, MENU_UPLOAD, 2, resources.getString(R.string.upload))
            .setIcon(R.drawable.ic_menu_file)
        menu.add(0, MENU_ABOUT, 3, resources.getString(R.string.about))
            .setIcon(R.drawable.ic_menu_about)
        menu.add(0, MENU_QUIT, 4, resources.getString(R.string.quit))
            .setIcon(R.drawable.ic_menu_exit)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            MENU_OPTIONS -> {
                val options = Options(this)
                options.show()
                return true
            }
            MENU_UPLOAD -> {
                val uulIntent = Intent(this.baseContext, UniversalUploader::class.java)
                uulIntent.putExtra("robotType", Integer.valueOf(mRobotType))
                this.startActivity(uulIntent)
                return true
            }
            MENU_ABOUT -> {
                showAbout()
                return true
            }
            MENU_QUIT -> {
                finish()
                return true
            }
        }
        return false
    }

    private fun showAbout() {
        val dialog = Dialog(this)
        if (dialog.window != null) {
            dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.aboutbox)
            val buttonOK = dialog.findViewById<Button>(R.id.AboutOKbutton)
            buttonOK.setOnClickListener { v: View? -> dialog.dismiss() }
            dialog.show()
        }
    }

    private fun lookupRobotType(): Int {
        return getSharedPreferences(MINDDROID_PREFS, MODE_PRIVATE).getInt(
            MINDDROID_ROBOT_TYPE,
            R.id.robot_type_shooterbot
        )
    }

    companion object {
        const val MENU_OPTIONS = Menu.FIRST
        const val MENU_UPLOAD = Menu.FIRST + 1
        const val MENU_ABOUT = Menu.FIRST + 2
        const val MENU_QUIT = Menu.FIRST + 3
        const val MINDDROID_PREFS = "Mprefs"
        const val MINDDROID_ROBOT_TYPE = "MrobotType"
        fun quitApplication() {
            if (isBtOnByUs() || UniversalUploader.isBtOnByUs()) {
                BluetoothAdapter.getDefaultAdapter().disable()
                setBtOnByUs(false)
                UniversalUploader.setBtOnByUs(false)
            }
        }
    }
}
