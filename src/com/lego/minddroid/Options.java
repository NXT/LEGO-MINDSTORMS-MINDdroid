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

		final RadioButton robot_type_1 = (RadioButton) mDialog.findViewById(R.id.robot_type_1);
		final RadioButton robot_type_2 = (RadioButton) mDialog.findViewById(R.id.robot_type_2);
		final RadioButton robot_type_3 = (RadioButton) mDialog.findViewById(R.id.robot_type_3);

		switch (splashMenu.getRobotType()) {
			case R.id.robot_type_2:
				robot_type_2.setChecked(true);
				break;

			case R.id.robot_type_3:
				robot_type_3.setChecked(true);
				break;

			default:
				robot_type_1.setChecked(true);
				break;
		}

		robot_type_1.setOnClickListener(radio_listener);
		robot_type_2.setOnClickListener(radio_listener);
		robot_type_3.setOnClickListener(radio_listener);
		robot_type_3.setEnabled(false);
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
			Toast.makeText(mDialog.getContext(), mSelectionMessage + " " + rb.getText(), Toast.LENGTH_SHORT).show();
			mDialog.dismiss();
		}
	};

}
