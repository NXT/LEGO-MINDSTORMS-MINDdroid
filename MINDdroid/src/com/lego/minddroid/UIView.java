package com.lego.minddroid;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;

public class UIView extends View {

	private static final String TAG = UIView.class.getName();;
	UIMovementToken mMoveIndicator;
	private UIActionButton myActionButton;
	private MINDdroid mActivity;
	private Canvas mCanvas;
	private Paint mPaint;

	private Bitmap myBackground;

	private SensorManager mSensorManager;

	private Typeface mFont = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

	private float mAccelX = 0;
	private float mAccelY = 0;
	private float mAccelZ = 0; // heading
	boolean init = true;
	// screen dimensions
	// int mCanvasWidth = 0;
	// int mCanvasHeight = 0;
	int mActionButtonHeight = 0;

	private final SensorEventListener mSensorAccelerometer = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onSensorChanged(SensorEvent event) {

			mAccelX = 0 - event.values[2];
			mAccelY = 0 - event.values[1];
			mAccelZ = event.values[0];

            mActivity.updateOrientation(event.values[0], event.values[1], event.values[2], true);

		}

	};

	public UIView(Context context, MINDdroid uiActivity) {
		super(context);

		mActivity = uiActivity;
		Display display = uiActivity.getWindowManager().getDefaultDisplay();

		// init paint and make is look "nice" with anti-aliasing.
		mPaint = new Paint();
		mPaint.setTextSize(14);
		mPaint.setTypeface(mFont);
		mPaint.setAntiAlias(true);

		mSensorManager = (SensorManager) mActivity.getSystemService(Context.SENSOR_SERVICE);

		// setup our button, background and movementIndicator.
        myBackground = BitmapFactory.decodeResource(mActivity.getApplicationContext().getResources(), R.drawable.background_1);
		myActionButton = new UIActionButton(mActivity);
		mMoveIndicator = new UIMovementToken(this, context.getApplicationContext());

	}

	@Override
	public void onDraw(Canvas canvas) {
		// update our canvas reference.
		mCanvas = canvas;

		if (init) {
			Log.d(TAG, "init.getHeight()  " + this.getHeight());
			Log.d(TAG, "init.getWidth() " + this.getWidth());
			myActionButton.setViewHeight(this.getHeight());
			myActionButton.setViewWidth(this.getWidth());
            myActionButton.scale();
            mActionButtonHeight = myActionButton.getActionButtonHeight();

            myBackground = Bitmap.createScaledBitmap(myBackground, this.getWidth(), this.getHeight(), true);
			mMoveIndicator.centerIcon();
			init = false;
		} else {
			updateMoveIndicator();
		}

		// clear the screen.
		// mPaint.setColor(Color.WHITE);
		// mCanvas.drawRect(0, 0, this.getWidth(), this.getHeight() - mActionButtonHeight, mPaint);
        // draw background image
        mCanvas.drawBitmap(myBackground, 0, 0, mPaint);
    	mMoveIndicator.draw(mCanvas, mPaint);
    	myActionButton.draw(mCanvas, mPaint);

		invalidate();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// we only want to handle down events .
		Log.d(TAG, "onTouchEvent");
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			Log.d(TAG, "onTouchEvent down");
			if (event.getY() > this.getHeight() - mActionButtonHeight) {
				Log.d(TAG, "onTouchEvent in button area");
				// myActionButton.drawAction(mCanvas, mPaint);
                mActivity.actionButtonPressed();
			}
		}
		return true;
	}

	public void registerListener() {
		List<Sensor> sensorList;
		// register orientation sensor
		sensorList = mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
		mSensorManager.registerListener(mSensorAccelerometer, sensorList.get(0), SensorManager.SENSOR_DELAY_GAME);

	}

	public void unregisterListener() {
		mSensorManager.unregisterListener(mSensorAccelerometer);

	}

	private void updateMoveIndicator() {
		mMoveIndicator.updateX(mAccelX);
		mMoveIndicator.updateY(mAccelY);

	}

}
