/**
 * Copyright 2010, 2011, 2012 Guenther Hoelzl, Shawn Brown
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
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class SplashMenuView extends View {

	int mScreenWidth;
	int mScreenHeight;
	int startButtonYStart;
	int tutorialButtonYStart;
	Activity splashMenuActivity;

	Resources res;
	Bitmap ic_splash_tutorial;
	Bitmap ic_splash_start;
	Bitmap ic_splash_legal;
	Bitmap logo_splash_minddroid;
	Bitmap mBackgroundImage;

	public SplashMenuView(Context context, Activity splashMenuActivity) {
		super(context);
		this.splashMenuActivity = splashMenuActivity;
		res = context.getResources();
	}

	private int calcImgHeight(float originalImageHeight, float originalImageWidth) {
		float screenWidth = mScreenWidth;
		return (int) (originalImageHeight * (screenWidth / originalImageWidth));
	}

	private float calcStartPos() {

		float remainingSpace;
		remainingSpace = mScreenHeight - logo_splash_minddroid.getHeight() - ic_splash_legal.getHeight() - ic_splash_start.getHeight()
				- ic_splash_tutorial.getHeight();
		float divider = remainingSpace / 5;
		startButtonYStart = (int) getStartButtonYPos(divider);
		return getStartButtonYPos(divider);
	}

	private float calcTutorialPos() {
		float remainingSpace;
		remainingSpace = mScreenHeight - logo_splash_minddroid.getHeight() - ic_splash_legal.getHeight() - ic_splash_start.getHeight()
				- ic_splash_tutorial.getHeight();

		float divider = remainingSpace / 5;
		tutorialButtonYStart = (int) getTutorialButtonYPos(divider);
		return getTutorialButtonYPos(divider);
	}

	public float getStartButtonYPos(float divider) {
		return (logo_splash_minddroid.getHeight() + ic_splash_start.getHeight() + (divider * 3));
	}

	public float getTutorialButtonYPos(float divider) {
		return (logo_splash_minddroid.getHeight() + (divider * 2));
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawBitmap(mBackgroundImage, 0, 0, null);
		canvas.drawBitmap(logo_splash_minddroid, 0, 0, null);
		canvas.drawBitmap(ic_splash_start, 0, calcStartPos(), null);
		canvas.drawBitmap(ic_splash_tutorial, 0, calcTutorialPos(), null);
		canvas.drawBitmap(ic_splash_legal, 0, mScreenHeight - ic_splash_legal.getHeight(), null);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		mScreenHeight = h;
		mScreenWidth = w;
		setupBitmaps();
		invalidate();

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {

			if (event.getY() > tutorialButtonYStart && event.getY() <= tutorialButtonYStart + ic_splash_tutorial.getHeight()) {
				Tutorial tutorial = new Tutorial(mScreenWidth, mScreenWidth);
				tutorial.show(splashMenuActivity);
			} else if (event.getY() > startButtonYStart && event.getY() <= startButtonYStart + ic_splash_start.getHeight()) {
				Intent playGame = new Intent(splashMenuActivity.getBaseContext(), MINDdroid.class);
				playGame.putExtra(SplashMenu.MINDDROID_ROBOT_TYPE, ((SplashMenu)splashMenuActivity).getRobotType());
				splashMenuActivity.startActivity(playGame);
			}
		}
		return true;
	}

	private void setupBitmaps() {

		ic_splash_tutorial = BitmapFactory.decodeResource(res, R.drawable.ic_splash_tutorial);
		ic_splash_tutorial = Bitmap.createScaledBitmap(ic_splash_tutorial, mScreenWidth,
				calcImgHeight(ic_splash_tutorial.getHeight(), ic_splash_tutorial.getWidth()), true);

		ic_splash_start = BitmapFactory.decodeResource(res, R.drawable.ic_splash_start);
		ic_splash_start = Bitmap.createScaledBitmap(ic_splash_start, mScreenWidth,
				calcImgHeight(ic_splash_start.getHeight(), ic_splash_start.getWidth()), true);

		ic_splash_legal = BitmapFactory.decodeResource(res, R.drawable.ic_splash_legal);
		ic_splash_legal = Bitmap.createScaledBitmap(ic_splash_legal, mScreenWidth,
				calcImgHeight(ic_splash_legal.getHeight(), ic_splash_legal.getWidth()), true);

		logo_splash_minddroid = BitmapFactory.decodeResource(res, R.drawable.logo_splash_minddroid);
		logo_splash_minddroid = Bitmap.createScaledBitmap(logo_splash_minddroid, mScreenWidth,
				calcImgHeight(logo_splash_minddroid.getHeight(), logo_splash_minddroid.getWidth()), true);

		mBackgroundImage = BitmapFactory.decodeResource(res, R.drawable.background_1);
		mBackgroundImage = Bitmap.createScaledBitmap(mBackgroundImage, mScreenWidth, mScreenHeight, true);

	}

}
