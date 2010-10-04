/**
 *   Copyright 2010 Guenther Hoelzl, Shawn Brown
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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

public class SplashMenu extends Activity {

	public static final int DISPLAY_TUTORIAL = 0;
	public static final int START = 1;
	public static final int CREDITS = 2;
	static Activity splashMenu;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		// class Eula added from http://code.google.com/p/apps-for-android/source/browse/trunk/DivideAndConquer/src/com/google/android/divideandconquer/Eula.java?r=93
		// Tutorial see http://androiddevstudio.com/tutorials/adding-eula-to-android-app
		// Eula.show(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash_menu);
		splashMenu = this;
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	
	public static void quitApplication(){
		splashMenu.finish();
	}
	
	public void showCredits(View v) {
		About about = new About();
		about.show(this);
	}
	
	public void playGame(View v) {
		Intent playGame = new Intent(splashMenu.getBaseContext(), MINDdroid.class);
		splashMenu.startActivity(playGame);
	}
	
	public void showTutorial(View v) {
		//change to real tutorial
		About about = new About();
		about.show(this);
	 
	}

}
