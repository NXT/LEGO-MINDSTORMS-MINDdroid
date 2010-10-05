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
import android.os.Bundle;
import android.view.View;
import android.view.Window;

public class SplashMenu extends Activity {

	public static void quitApplication() {
		splashMenu.finish();

	}
	private View splashMenuView;

	private static Activity splashMenu;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		splashMenuView = new SplashMenuView(getApplicationContext(), this);
		setContentView(splashMenuView);
		splashMenu = this;
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

}
