package com.lego.mindstorms.android;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

public class UIActivity extends Activity {

	private UIView mView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// setup our view, give it focus and display.
		mView = new UIView(getApplicationContext(), this);
		mView.setFocusable(true);
		setContentView(mView);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mView.registerListener();
		mView.init = true;
	}

	@Override
	public void onSaveInstanceState(Bundle icicle) {
		super.onSaveInstanceState(icicle);
		mView.unregisterListener();
	}
}