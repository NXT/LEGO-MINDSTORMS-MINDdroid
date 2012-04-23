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
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.Toast;

public class Options {

	private Dialog mDialog;
	String mSelectionMessage;
	SplashMenu splashMenu;

	public Options(Activity myActivity) {
		this.splashMenu=(SplashMenu) myActivity;
		mDialog = new Dialog(myActivity);
		mDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		mDialog.setContentView(R.layout.options);
		mSelectionMessage = myActivity.getString(R.string.model_type_selected);

		final RadioButton robotTypeShooterbot = 
            (RadioButton) mDialog.findViewById(R.id.robot_type_shooterbot);
		final RadioButton robotTypeTribot = 
            (RadioButton) mDialog.findViewById(R.id.robot_type_tribot);
		final RadioButton robotTypeRobogator = 
            (RadioButton) mDialog.findViewById(R.id.robot_type_robogator);
		final RadioButton robotTypeLejos = 
            (RadioButton) mDialog.findViewById(R.id.robot_type_lejos);
		
		switch (splashMenu.getRobotType()) {

			case R.id.robot_type_tribot:
				robotTypeTribot.setChecked(true);
				break;

			case R.id.robot_type_robogator:
				robotTypeRobogator.setChecked(true);
				break;

			case R.id.robot_type_lejos:
				robotTypeLejos.setChecked(true);
				break;

			default:
				robotTypeShooterbot.setChecked(true);
				break;
		}

		robotTypeShooterbot.setOnClickListener(radio_listener);
		robotTypeTribot.setOnClickListener(radio_listener);
		robotTypeRobogator.setOnClickListener(radio_listener);
		robotTypeLejos.setOnClickListener(radio_listener);
	}

	public void show() {
		mDialog.show();
	}

	private OnClickListener radio_listener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// Perform action on clicks
			RadioButton rb = (RadioButton) v;
			rb.setChecked(true);
			splashMenu.setRobotType(rb.getId());
			Toast.makeText(mDialog.getContext(), mSelectionMessage + " " + 
                rb.getText(), Toast.LENGTH_SHORT).show();
			mDialog.dismiss();
		}
	};

}
