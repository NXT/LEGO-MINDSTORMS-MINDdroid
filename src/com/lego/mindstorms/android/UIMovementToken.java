package com.lego.mindstorms.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

public class UIMovementToken {

	private static final String TAG = UIMovementToken.class.getName();
	private Bitmap mIcon;

	// View controlling movement
	private UIView mView;

	private int mX = 0;
	private int mY = 0;
	private int mIconWidth;
	private int mIconHeight;
	// if 0, even very slight tilt will result in movement
	private float mSensorBuffer = 3;

	/**
	 * MovementIndicator constructor.
	 * 
	 * @param view
	 *            View controlling the move_icon
	 * @param context
	 */
	public UIMovementToken(UIView view, Context context) {
		this.mView = view;
		mIcon = BitmapFactory.decodeResource(context.getApplicationContext().getResources(), R.drawable.alpha_rex);

		mIconWidth = mIcon.getWidth();
		mIconHeight = mIcon.getHeight();

		Log.d(TAG, "iw : ih" + mIconHeight + " : " + mIconHeight);
	}

	/**
	 * Draw the move_icon.
	 * 
	 * @param canvas
	 *            Canvas object to draw too.
	 * @param paint
	 *            Paint object used to draw with.
	 */
	public void draw(Canvas canvas, Paint paint) {
		canvas.drawBitmap(mIcon, mX - (mIconWidth / 2), mY - (mIconHeight / 2), paint);
	}

	/**
	 * Attempt to update the move_icon with a new x value, boundary checking
	 * enabled to make sure the new co-ordinate is valid.
	 * 
	 * @param newX
	 *            Incremental value to add onto current x co-ordinate.
	 */
	public void updateX(float newX) {
		if (newX > mSensorBuffer || newX < -mSensorBuffer) {//if slight tilt, do nothing
			Log.d(TAG, "mAccelX " + newX);

			mX = ((mView.getWidth() / 2)- (mIconWidth / 2)) + (int) ((newX / 10) * (mView.getWidth() / 10));
			// Log.i(TAG,"newX : mX --"+newX+" : "+mX);
			
			// boundary checking, don't want the move_icon going off-screen.
			if (mX + mIconWidth / 2 >= mView.getWidth()) {//set at outer edge
				mX = mView.getWidth() - (mIconWidth / 2);
			} else if (mX - (mIconWidth / 2) < 0) {
				mX = mIconWidth / 2;
			}

		} 

	}

	/**
	 * Attempt to update the move_icon with a new y value, boundary checking
	 * enabled to make sure the new co-ordinate is valid.
	 * 
	 * @param newY
	 *            Incremental value to add onto current y co-ordinate.
	 */
	public void updateY(float newY) {

		if (newY > mSensorBuffer || newY < -mSensorBuffer) {// if slight tilt do nothing
			Log.d(TAG, "mAccelY " + newY);

			mY = ((mView.getHeight() / 2)-mIconHeight / 2) + (int) ((newY / 10) * (mView.getHeight() / 10));
			// Log.i(TAG,"newY : mY --"+newY+" : "+mY);
			
			// boundary checking, don't want the move_icon rolling off-screen.
			if (mY + mView.mActionButtonHeight + mIconHeight / 2 >= mView.getHeight()) {//set at outer edge
				mY = mView.getHeight() - mView.mActionButtonHeight - mIconHeight / 2;
			} else if (mY - mIconHeight / 2 < 0) {
				mY = mIconHeight / 2;
			}
		}  

	}

	/**
	 * @return Current x co-ordinate.
	 */
	public int getX() {
		return mX;
	}

	/**
	 * @return Current y co-ordinate.
	 */
	public int getY() {
		return mY;
	}

	public void centerIcon() {

		mX = (mView.getWidth() / 2) - (mIconWidth / 2);// center - half of icon
		mY = ((mView.getHeight() - mView.mActionButtonHeight) / 2) - (mIconHeight / 2);// center
																						// -
																						// half
																						// of
																						// icon

	}
}