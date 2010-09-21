package com.lego.minddroid;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

	class GameThread extends Thread {
		private int ICON_MAX_SIZE;
		private int ICON_MIN_SIZE;

		private int GOAL_HEIGHT;
		private int GOAL_WIDTH;
		private static final int HAPTIC_FEEDBACK_LENGTH = 30;

		boolean inGoal = true;
		Vibrator mHapticFeedback;

		/** The drawable to use as the background of the animation canvas */
		private Bitmap mBackgroundImage;

		private Bitmap mIconInGoal;

		private Drawable mIconOrange;

		private Drawable mIconBlue;

		private Drawable mIconWhite;

		private Bitmap mTarget;

		private Bitmap mActionButton;

		/**
		 * Current height of the surface/canvas.
		 * 
		 * @see #setSurfaceSize
		 */
		private int mCanvasHeight = 1;

		/**
		 * Current width of the surface/canvas.
		 * 
		 * @see #setSurfaceSize
		 */
		private int mCanvasWidth = 1;

		/** Message handler used by thread to interact with TextView */
		private Handler mHandler;

		/** Used to figure out elapsed time between frames */
		private long mLastTime;

		/** Indicate whether the surface has been created & is ready to draw */
		private boolean mRun = false;

		/** Scratch rect object. */
		private RectF mScratchRect;

		/** Handle to the surface manager object we interact with */
		private SurfaceHolder mSurfaceHolder;

		/** X of motion indicator */
		private float mX;

		/** Y of motion indicator */
		private float mY;

		/**
		 * mIconSize grows within target between ICON_MIN_SIZE and ICON_MAX_SIZE
		 */
		private int growAdjust;

		/** buffer before movement begins - 0 means any tilt moves icon */

		private long mFeedbackEnd = 0;
		long test = 0;
		long elapsedSincePulse = 0;
		long elapsedSinceDraw = 0;

		public GameThread(SurfaceHolder surfaceHolder, Context context, Vibrator vibrator, Handler handler) {
			// get handles to some important objects
			mHapticFeedback = vibrator;
			mSurfaceHolder = surfaceHolder;
			mHandler = handler;
			mContext = context;

			Resources res = context.getResources();

			mIconOrange = context.getResources().getDrawable(R.drawable.orange);
			// load background image as a Bitmap instead of a Drawable b/c
			// we don't need to transform it and it's faster to draw this way
			mIconBlue = context.getResources().getDrawable(R.drawable.blue);
			mIconWhite = context.getResources().getDrawable(R.drawable.white);
			mTarget = BitmapFactory.decodeResource(res, R.drawable.target_no_orange_dot);
			mActionButton = BitmapFactory.decodeResource(res, R.drawable.action_btn_up);
			mBackgroundImage = BitmapFactory.decodeResource(res, R.drawable.background_1);

			mScratchRect = new RectF(0, 0, 0, 0);

		}

		/**
		 * Starts the game, setting parameters for the current difficulty.
		 */
		public void doStart() {
			synchronized (mSurfaceHolder) {

				mX = mCanvasWidth / 2;
				mY = (mCanvasHeight - mActionButton.getHeight()) / 2;

			}
		}

		/**
		 * Pauses the animation.
		 */
		public void pause() {
			// thread.pause();
			synchronized (mSurfaceHolder) {

			}
		}

		/**
		 * Restores game state from the indicated Bundle. Typically called when
		 * the Activity is being restored after having been previously
		 * destroyed.
		 * 
		 * @param savedState
		 *            Bundle containing the game state
		 */
		public synchronized void restoreState(Bundle savedState) {
			synchronized (mSurfaceHolder) {

			}
		}
    
		 int aveCount=0;

		@Override
		public void run() {
			Log.d(TAG, "--run--");
			while (mRun) {

				updateTime();

				updateMoveIndicator(mAccelX, mAccelY);

				if (elapsedSinceDraw > 100) {

					Canvas c = null;
					try {
						c = mSurfaceHolder.lockCanvas(null);
						synchronized (mSurfaceHolder) {

							thread.mX = ((mNumX / mNum) + (previousNumX/previousNum))/2;
							thread.mY = ((mNumY / mNum) + (previousNumY/previousNum))/2;
							Log.d(TAG, "mNum in averaging " + mNum);
							
							previousNumY=mNumY;
							previousNumX=mNumX;
							previousNum=mNum;
                           // if (aveCount*3< mNum){
							mNumY = 0;//mNumY/3;
							mNumX = 0;//mNumX/3;
							mNum = 0;//mNum/3;
							
					
                           // }
                            aveCount=0;

							if (elapsedSincePulse > 800) {
								elapsedSincePulse = pulseInGoal == mIconBlue ? 600 : 0;// mLastTime
																						// set
																						// show
																						// white
																						// icon
																						// for
																						// only
																						// 200
																						// ms
								pulseInGoal = pulseInGoal == mIconBlue ? mIconWhite : mIconBlue;

							}

							pulseNotInGoal = pulseNotInGoal == mIconOrange ? mIconWhite : mIconOrange;
							doDraw(c);

						}
					} finally {
						// do this in a finally so that if an exception is
						// thrown
						// during the above, we don't leave the Surface in an
						// inconsistent state
						if (c != null) {
							if (isInGoal()) { // icon is in goal
								thread.inGoal = true;
							} else {

								if (thread.inGoal) {// was in goal before
									thread.inGoal = false;
									thread.vibrate();

								}

							}
							mSurfaceHolder.unlockCanvasAndPost(c);

							elapsedSinceDraw = 0;// mLastTime set to current
													// moment in updateTime
						}
					}
				}
			}
		}

		/**
		 * Dump game state to the provided Bundle. Typically called when the
		 * Activity is being suspended.
		 * 
		 * @return Bundle with this view's state
		 */
		public Bundle saveState(Bundle map) {
			synchronized (mSurfaceHolder) {
				if (map != null) {

				}
			}
			return map;
		}

		/**
		 * Used to signal the thread whether it should be running or not.
		 * Passing true allows the thread to run; passing false will shut it
		 * down if it's already running. Calling start() after this was most
		 * recently called with false will result in an immediate shutdown.
		 * 
		 * @param b
		 *            true to run, false to shut down
		 */
		public void setRunning(boolean b) {
			mRun = b;
		}

		/**
		 * Sets the game mode. That is, whether we are running, paused, etc.
		 * 
		 * @see #setState(int, CharSequence)
		 * @param mode
		 *            one of the STATE_* constants
		 */
		public void setState(int mode) {
			synchronized (mSurfaceHolder) {
				setState(mode, null);
			}
		}

		/**
		 * Sets the game mode. That is, whether we are running, paused, in the
		 * failure state, in the victory state, etc.
		 * 
		 * @param mode
		 *            one of the STATE
		 * @param message
		 *            string to add to screen or null
		 */
		public void setState(int mode, CharSequence message) {

			synchronized (mSurfaceHolder) {

			}

		}

		/* Callback invoked when the surface dimensions change. */
		public void setSurfaceSize(int width, int height) {
			// synchronized to make sure these all change atomically
			synchronized (mSurfaceHolder) {
				mCanvasWidth = width;
				mCanvasHeight = height;
				float mAHeight = mActionButton.getHeight();
				float mAWidth = mActionButton.getWidth();
				mActionButton = Bitmap.createScaledBitmap(mActionButton, width, (Math.round((width * (mAHeight / mAWidth)))), true);

				// don't forget to resize the background image
				mBackgroundImage = Bitmap.createScaledBitmap(mBackgroundImage, width, height, true);

				// mIconOrangeFull = Bitmap.createScaledBitmap(mIconOrangeFull,
				// ICON_MAX_SIZE, ICON_MAX_SIZE, true);

				int temp_ratio = mCanvasWidth / 64;
				GOAL_WIDTH = mCanvasWidth / temp_ratio;

				ICON_MAX_SIZE = (GOAL_WIDTH / 8) * 6;// mIconOrange.getWidth();
				ICON_MIN_SIZE = (GOAL_WIDTH / 8);

				temp_ratio = mCanvasHeight / 64;
				GOAL_HEIGHT = mCanvasHeight / temp_ratio;
				// GOAL_HEIGHT= (GOAL_WIDTH/8)*6;
				mTarget = Bitmap.createScaledBitmap(mTarget, GOAL_WIDTH, GOAL_HEIGHT, true);
				// mIconHeight = mCanvasWidth/temp_ratio;//
				// mIconOrange.getHeight();

			}
		}

		/**
		 * Resumes from a pause.
		 */
		public void unpause() {
			// Move the real time clock up to now
			synchronized (mSurfaceHolder) {
				mLastTime = System.currentTimeMillis() + 100;
			}

		}

		Drawable pulseInGoal;
		Drawable pulseNotInGoal;

		/**
		 * Draws move indicator, button and background to the provided Canvas.
		 */
		private void doDraw(Canvas mCanvas) {
			// Draw the background image. Operations on the Canvas accumulate

			// draw the background
			mCanvas.drawBitmap(mBackgroundImage, 0, 0, null);

			// draw the action button
			mCanvas.drawBitmap(mActionButton, 0, mCanvasHeight - mActionButton.getHeight(), null);

			// draw the goal
			mCanvas.drawBitmap(mTarget, (mCanvasWidth - mTarget.getWidth()) / 2,
					((mCanvasHeight - mActionButton.getHeight()) / 2) - (mTarget.getHeight() / 2), null);

			// update the icon location and draw (or blink) it

			if (inGoal) {

				pulseInGoal.setBounds((int) mX - (growAdjust / 2), (int) mY - ((growAdjust / 2)), ((int) mX + (growAdjust / 2)), (int) mY
						+ (growAdjust / 2));
				pulseInGoal.draw(mCanvas);

			} else {

				pulseNotInGoal.setBounds((int) mX - (growAdjust / 2), (int) mY - (growAdjust / 2), ((int) mX + growAdjust / 2),
						((int) mY + growAdjust / 2));
				pulseNotInGoal.draw(mCanvas);

			}

		}

		private int calcGrowAdjust(float mAcX, float mAcY) {

			int mX2 = ((thread.mCanvasWidth / 2)) + (int) ((mAcX / 10) * (thread.mCanvasWidth / 10));
			int mY2 = (((thread.mCanvasHeight - thread.mActionButton.getHeight()) / 2))
					+ (int) ((mAcY / 10) * ((thread.mCanvasHeight - thread.mActionButton.getHeight()) / 10));

			int xDistanceFromCenter = Math.abs((mCanvasWidth / 2) - mX2);
			int yDistanceFromCenter = Math.abs(((mCanvasHeight - mActionButton.getHeight()) / 2) - mY2);

			if (xDistanceFromCenter > ICON_MAX_SIZE || yDistanceFromCenter > ICON_MAX_SIZE) {
				return ICON_MAX_SIZE;
			}

			if (xDistanceFromCenter > yDistanceFromCenter) {
				return (xDistanceFromCenter > ICON_MIN_SIZE ? xDistanceFromCenter : ICON_MIN_SIZE);
			}

			return (yDistanceFromCenter > ICON_MIN_SIZE ? yDistanceFromCenter : ICON_MIN_SIZE);
		}

		public void vibrate() {
			mHapticFeedback.vibrate(HAPTIC_FEEDBACK_LENGTH);
			mFeedbackEnd = System.currentTimeMillis() + HAPTIC_FEEDBACK_LENGTH + 15;

		}

	

		private void updateTime() {// use for blinking
			long now = System.currentTimeMillis();
			if (test == 0) {
				test = now;
			}
			if (test + 1000 > now) {
				Log.d(TAG, "updatetime for 1000 " + now);
			}

			// Do nothing if mLastTime is in the future.
			// This allows the game-start to delay the start
			// by 100ms or whatever.
			if (mLastTime > now)
				return;

			// double elapsed = (now - mLastTime) / 1000.0;
			long elapsed = now - mLastTime;
			elapsedSincePulse += elapsed;
			elapsedSinceDraw += elapsed;

			mLastTime = now;

		}

	}

	private static final String TAG = GameView.class.getName();;

	private MINDdroid mActivity;
	/** The thread that actually draws the animation */
	private GameThread thread;
	private Paint mPaint;

	private SensorManager mSensorManager;

	private Typeface mFont = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

	/** Handle to the application context, used to e.g. fetch Drawables. */
	private Context mContext;
	private float mAccelX = 0;
	private float mAccelY = 0;
	private float mAccelZ = 0; // heading
	
	private float mNumX;
	private float mNumY;
	private int mNum = 0;
	
	private float previousNumX;
	private float previousNumY;
	private int previousNum = 0;
	/** Message handler used by thread to interact with TextView */

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

            // !!! should be called somewhere above after the digital filtering !!!
			mActivity.updateOrientation(event.values[0], event.values[1], event.values[2], true);

		}

	};

	public GameView(Context context, MINDdroid uiActivity) {
		super(context);
		Log.d(TAG, " ~~~~~~~ UIView ~~~~~~~~");
		mActivity = uiActivity;
		mSensorManager = (SensorManager) mActivity.getSystemService(Context.SENSOR_SERVICE);

		// register our interest in hearing about changes to our surface
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		// create thread only; it's started in surfaceCreated()
		thread = new GameThread(holder, context, (Vibrator) uiActivity.getSystemService(Context.VIBRATOR_SERVICE), new Handler() {
			@Override
			public void handleMessage(Message m) {

			}
		});

		setFocusable(true); // make sure we get key events
		Log.d(TAG, "UIView finished");
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// we only want to handle down events .

		if (event.getAction() == MotionEvent.ACTION_DOWN) {

			if (event.getY() > this.getHeight() - thread.mActionButton.getHeight()) {
				Log.d(TAG, "onTouchEvent in button area TouchEvent");

				// implement action here
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

	

	private void updateMoveIndicator(float mAcX, float mAcY) {
		// if (thread.mLastTime > thread.mFeedbackEnd) {

		thread.growAdjust = thread.calcGrowAdjust(mAcX, mAcY);

		thread.mX = ((thread.mCanvasWidth / 2)) + (int) ((mAcX / 10) * (thread.mCanvasWidth / 10));

		// boundary checking, don't want the move_icon going off-screen.
		if (thread.mX + thread.growAdjust / 2 >= thread.mCanvasWidth) {// set
																		// at
																		// outer
																		// edge
			thread.mX = thread.mCanvasWidth - (thread.growAdjust / 2);
		} else if (thread.mX - (thread.growAdjust / 2) < 0) {
			thread.mX = thread.growAdjust / 2;
		}
		mNumX += thread.mX;

		thread.mY = (((thread.mCanvasHeight - thread.mActionButton.getHeight()) / 2))
				+ (int) ((mAcY / 10) * ((thread.mCanvasHeight - thread.mActionButton.getHeight()) / 10));

		// boundary checking, don't want the move_icon rolling
		// off-screen.
		if (thread.mY + thread.growAdjust / 2 >= (thread.mCanvasHeight - thread.mActionButton.getHeight())) {// set
																												// at
																												// outer
																												// edge
			thread.mY = thread.mCanvasHeight - thread.mActionButton.getHeight() - thread.growAdjust / 2;
		} else if (thread.mY - thread.growAdjust / 2 < 0) {
			thread.mY = thread.growAdjust / 2;
		}

		mNumY += thread.mY;
        
		thread.aveCount++;
		mNum++;

	}

	public boolean isInGoal() {

		if ((thread.mCanvasWidth - thread.mTarget.getWidth()) / 2 > thread.mX || (thread.mCanvasWidth + thread.mTarget.getWidth()) / 2 < thread.mX) {// x
																																						// is
																																						// not
																																						// within
																																						// goal
			return false;
		}

		if (((thread.mCanvasHeight - (thread.mActionButton.getHeight() + (thread.GOAL_HEIGHT))) / 2 > thread.mY || ((thread.mCanvasHeight - thread.mActionButton
				.getHeight()) + (thread.GOAL_HEIGHT)) / 2 < thread.mY)) {
			return false;
		}

		return true;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		thread.setSurfaceSize(width, height);

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surface created");
		// start the thread here so that we don't busy-wait in run()
		// waiting for the surface to be created
		thread.setRunning(true);
		thread.start();

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
		thread.setRunning(false);
		while (retry) {
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}
	}

}
