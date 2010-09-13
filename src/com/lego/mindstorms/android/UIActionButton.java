package com.lego.mindstorms.android;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

/**
 * Background drawn on screen, each new level is loaded once the previous level
 * has been completed.
 */
public class UIActionButton {

	private static final String TAG = UIActionButton.class.getName();
	// current tile attributes
	private int left = 0;
	private int viewHeight;
	private int viewWidth;
	private Bitmap mActionButton;
	private int buttonTop;
	private int buttonWidth;
	boolean erase = true;

	/**
	 * Background constructor.
	 * 
	 * @param context
	 *            Application context used to load images.
	 */
	UIActionButton(Activity activity) {

		mActionButton = BitmapFactory.decodeResource(activity.getApplicationContext().getResources(), R.drawable.push);

	}

	/**
	 * Draw the Background.
	 * 
	 * @param canvas
	 *            Canvas object to draw too.
	 * @param paint
	 *            Paint object used to draw with.
	 * @param viewHeight
	 * @param viewWidth
	 */
	public void draw(Canvas canvas, Paint paint) {

		buttonTop = getViewHeight() - getActionButtonHeight();

		buttonWidth = mActionButton.getWidth();

		paint.setColor(Color.WHITE);
		canvas.drawRect(left, buttonTop, getViewWidth(), getViewHeight(), paint);
		canvas.drawBitmap(mActionButton, left, buttonTop, paint);

		if (!erase) {
			paint.setColor(Color.BLACK);
			canvas.drawText("Action!", buttonWidth + 5, buttonTop + (getActionButtonHeight() * 1 / 3), paint);
			paint.setColor(Color.WHITE);

		}

	}

	public void drawAction(Canvas canvas, Paint paint) {
		Log.d(TAG, "drawAction erase: " + erase);
		if (erase) {

			erase = false;
		} else {

			erase = true;
		}

	}

	public int getActionButtonHeight() {
		// Log.d(TAG, "mActionButton.getHeight(): " +
		// mActionButton.getHeight());
		return mActionButton.getHeight();
	}

	public int getViewHeight() {
		// Log.d(TAG,"getViewHeight: "+viewHeight);
		return viewHeight;
	}

	public int getViewWidth() {
		// Log.d(TAG,"getViewWidth: "+viewWidth);
		return viewWidth;
	}

	public void setViewHeight(int viewHeight) {
		this.viewHeight = viewHeight;
	}

	public void setViewWidth(int viewWidth) {
		this.viewWidth = viewWidth;
	}

}