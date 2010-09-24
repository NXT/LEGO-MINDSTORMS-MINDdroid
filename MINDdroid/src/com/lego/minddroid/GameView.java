package com.lego.minddroid;

import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
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
	    /** time between each redraw */
		private static final int REDRAW_SCHED = 100;
		private int ICON_MAX_SIZE;
		private int ICON_MIN_SIZE;

		private int GOAL_HEIGHT;
		private int GOAL_WIDTH;
		private static final int HAPTIC_FEEDBACK_LENGTH = 30;
		/**
		 * is tilt icon in goal
		 */
		boolean mInGoal = true;

		/**
		 * to notify users when leaving goal
		 */
		Vibrator mHapticFeedback;

		/** The drawable to use as the background of the animation canvas */
		private Bitmap mBackgroundImage;

		private Drawable mIconOrange;

		private Drawable mIconWhite;

		private Bitmap mTarget;
		
		private Bitmap mTargetInactive;

		private Bitmap mActionButton;

		private Bitmap mActionDownButton;

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
		private int mGrowAdjust;

		/**
		 * time when haptic feedback will stop - needed to ensure we don't take
		 * tilt measurements while handset if vibrating
		 */
		private long mFeedbackEnd = 0;

		/**
		 * track how long since we redrew screen
		 */
		long mElapsedSinceDraw = 0;
		
		/**
		 * track how long since we redrew screen
		 */
		long mElapsedSinceNXTCommand = 0;

		/**
		 * count how many times we took tilt readings in 100ms so we can average
		 * position
		 */
		int mAvCount = 0;
		/**
		 * time when tilt icon should change color
		 */
		long mNextPulse = 0;

		/* holder for current color in pulse effect* */
		Drawable mPulsingTiltIcon;

		/** was action button just pressed */
		boolean mActionPressed = false;
		
		/** */
		boolean mToNXT=false;
		
		
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
			mIconWhite = context.getResources().getDrawable(R.drawable.white);
			mTarget = BitmapFactory.decodeResource(res, R.drawable.target_no_orange_dot);
			mTargetInactive = BitmapFactory.decodeResource(res, R.drawable.target);
			mActionButton = BitmapFactory.decodeResource(res, R.drawable.action_btn_up);
			mActionDownButton = BitmapFactory.decodeResource(res, R.drawable.action_btn_down);
			mBackgroundImage = BitmapFactory.decodeResource(res, R.drawable.background_1);
			mScratchRect = new RectF(0, 0, 0, 0);

		}

		private int calcGrowAdjust(float mX2, float mY2) {

			int xDistanceFromCenter = (int) Math.abs((mCanvasWidth / 2) - mX2);
			int yDistanceFromCenter = (int) Math.abs(((mCanvasHeight - mActionButton.getHeight()) / 2) - mY2);

			if (xDistanceFromCenter > ICON_MAX_SIZE || yDistanceFromCenter > ICON_MAX_SIZE) {
				return ICON_MAX_SIZE;
			}

			if (xDistanceFromCenter > yDistanceFromCenter) {
				return (xDistanceFromCenter > ICON_MIN_SIZE ? xDistanceFromCenter : ICON_MIN_SIZE);
			}

			return (yDistanceFromCenter > ICON_MIN_SIZE ? yDistanceFromCenter : ICON_MIN_SIZE);
		}

		private int calcNextPulse() {

			int xDistanceFromGoal = 0;
			int yDistanceFromGoal = 0;

			if (mX > mCanvasWidth / 2) {
				xDistanceFromGoal = (int) ((mX - (mCanvasWidth / 2)) - (GOAL_WIDTH / 2));

			} else {
				xDistanceFromGoal = (int) ((mCanvasWidth / 2) - mX) - (GOAL_WIDTH / 2);
			}
			xDistanceFromGoal += ICON_MAX_SIZE / 2;//adjust for icon width so that when icon touches outer edge, it will be at 100%.

			if (mY > ((mCanvasHeight - mActionButton.getHeight()) / 2)) {
				yDistanceFromGoal = (int) ((mY - ((mCanvasHeight - mActionButton.getHeight()) / 2)) - (GOAL_WIDTH / 2));//GOAL_WIDTH ok for y when square
			} else {
				yDistanceFromGoal = (int) (((mCanvasHeight - mActionButton.getHeight()) / 2) - mY - (GOAL_WIDTH / 2));

			}
			yDistanceFromGoal += ICON_MAX_SIZE / 2;//adjust for icon width so that when icon touches outer edge, it will be at 100%.

			double mOneSideGameWidth = (mCanvasWidth - GOAL_WIDTH) / 2;//

			double mOneSideGameHeight = ((mCanvasHeight - mActionButton.getHeight()) / 2) - (GOAL_WIDTH / 2);// if it's square --OK

			double mPercentToXEdge = (xDistanceFromGoal / (mOneSideGameWidth)) * 100;
			double mPercentToYEdge = (yDistanceFromGoal / mOneSideGameHeight) * 100;
			//Log.d(TAG,"mPercentToXEdge :" + mPercentToXEdge);
			//Log.d(TAG,"mPercentToYEdge :" + mPercentToYEdge);

			float closeEdge = (float) (mPercentToXEdge > mPercentToYEdge ? mPercentToXEdge : mPercentToYEdge);
			return (int) (800 - ((closeEdge * 8)));
		}

		/**
		 * Draws move indicator, button and background to the provided Canvas.
		 */
		private void doDraw(Canvas mCanvas) {
			 
			if (!mActivity.connected){
				
				// draw the background
				mCanvas.drawBitmap(mBackgroundImage, 0, 0, null);
				//draw pressed action button
				mCanvas.drawBitmap(mActionDownButton, 0, mCanvasHeight - mActionButton.getHeight(), null);
				//draw icon in goal
				// draw the goal
				mCanvas.drawBitmap(mTargetInactive, (mCanvasWidth - mTarget.getWidth()) / 2,
						((mCanvasHeight - mActionButton.getHeight()) / 2) - (mTarget.getHeight() / 2), null);
			}else{
			
			
			// Draw the background image. Operations on the Canvas accumulate
			if (isInGoal()) { // icon is in goal
				mInGoal = true;
				mGrowAdjust = calcGrowAdjust(mX, mY);
			} else {
				mGrowAdjust = ICON_MAX_SIZE;
				if (mInGoal) {// was in goal before
					mInGoal = false;
					vibrate();

				}

			}

			// draw the background
			mCanvas.drawBitmap(mBackgroundImage, 0, 0, null);

			// draw the action button
			mCanvas.drawBitmap(mActionPressed ? mActionDownButton : mActionButton, 0, mCanvasHeight - mActionButton.getHeight(), null);
			mActionPressed = false;

			// draw the goal
			mCanvas.drawBitmap(mTarget, (mCanvasWidth - mTarget.getWidth()) / 2,
					((mCanvasHeight - mActionButton.getHeight()) / 2) - (mTarget.getHeight() / 2), null);

			// update the icon location and draw (or blink) it
			if (mInGoal) {

				mIconOrange.setBounds((int) mX - (mGrowAdjust / 2), (int) mY - ((mGrowAdjust / 2)), ((int) mX + (mGrowAdjust / 2)), (int) mY
						+ (mGrowAdjust / 2));
				mIconOrange.draw(mCanvas);

			} else {

				// boundary checking, don't want the move_icon going off-screen.
				if (mX + ICON_MAX_SIZE / 2 >= mCanvasWidth) {// set at outer edge

					mX = mCanvasWidth - (ICON_MAX_SIZE / 2);
				} else if (mX - (ICON_MAX_SIZE / 2) < 0) {
					mX = ICON_MAX_SIZE / 2;
				}

				// boundary checking, don't want the move_icon rolling
				// off-screen.
				if (mY + ICON_MAX_SIZE / 2 >= (mCanvasHeight - mActionButton.getHeight())) {// set at outer edge

					mY = mCanvasHeight - mActionButton.getHeight() - ICON_MAX_SIZE / 2;
				} else if (mY - ICON_MAX_SIZE / 2 < 0) {
					mY = ICON_MAX_SIZE / 2;
				}

				if (mLastTime > mNextPulse) {

					mPulsingTiltIcon = mPulsingTiltIcon == mIconOrange ? mIconWhite : mIconOrange;
					mNextPulse = mPulsingTiltIcon == mIconOrange ? mLastTime + calcNextPulse() : mLastTime + 90;
					//Log.i(TAG, "next pulse " + (nextPulse - mLastTime));
				}

				mPulsingTiltIcon.setBounds((int) mX - (mGrowAdjust / 2), (int) mY - (mGrowAdjust / 2), ((int) mX + mGrowAdjust / 2),
						((int) mY + mGrowAdjust / 2));
				mPulsingTiltIcon.draw(mCanvas);

			}
			}

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

		@Override
		public void run() {
			Log.d(TAG, "--run--");
			while (mRun) {

				updateTime();
				updateMoveIndicator(mAccelX, mAccelY);
				
				
				if (mElapsedSinceDraw > REDRAW_SCHED) {
				    
				    if ((mElapsedSinceNXTCommand > MINDdroid.UPDATE_TIME) && (mNumAc > 0)) {
					    mActivity.updateMotorControl(-mNumAcY/mNumAc, -mNumAcX/mNumAc);
					    mNumAcX=0;
					    mNumAcY=0;
					    mElapsedSinceNXTCommand = 0;
					    mNumAc=0;
					}

					mX = ((mNumX / mNum) + (mPreviousNumX / mPreviousNum)) / 2;
					mY = ((mNumY / mNum) + (mPreviousNumY / mPreviousNum)) / 2;

					mPreviousNumY = mNumY;
					mPreviousNumX = mNumX;
					mPreviousNum = mNum;

					mNumY = 0;
					mNumX = 0;
					mNum = 0;

					mAvCount = 0;
				    
				    
					Canvas c = null;
					try {
						c = mSurfaceHolder.lockCanvas(null);
						synchronized (mSurfaceHolder) {
							doDraw(c);
							
						}
					} finally {
						// do this in a finally so that if an exception is
						// thrown
						// during the above, we don't leave the Surface in an
						// inconsistent state
						if (c != null) {

							mSurfaceHolder.unlockCanvasAndPost(c);

							mElapsedSinceDraw = 0;// mLastTime set to current
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
				mActionDownButton = Bitmap.createScaledBitmap(mActionDownButton, width, (Math.round((width * (mAHeight / mAWidth)))), true);
				// don't forget to resize the background image
				mBackgroundImage = Bitmap.createScaledBitmap(mBackgroundImage, width, height, true);

				int temp_ratio = mCanvasWidth / 64;
				GOAL_WIDTH = mCanvasWidth / temp_ratio;

				ICON_MAX_SIZE = (GOAL_WIDTH / 8) * 6;
				ICON_MIN_SIZE = (GOAL_WIDTH / 4);

				temp_ratio = mCanvasHeight / 64;
				GOAL_HEIGHT = mCanvasHeight / temp_ratio;

				mTarget = Bitmap.createScaledBitmap(mTarget, GOAL_WIDTH, GOAL_HEIGHT, true);
				mTargetInactive = Bitmap.createScaledBitmap(mTargetInactive, GOAL_WIDTH, GOAL_HEIGHT, true);
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

		private void updateTime() {// use for blinking
			long now = System.currentTimeMillis();

			// Do nothing if mLastTime is in the future.
			// This allows the game-start to delay the start
			// by 100ms or whatever.
			if (mLastTime > now)
				return;

			// double elapsed = (now - mLastTime) / 1000.0;
			long elapsed = now - mLastTime;
			//	elapsedSincePulse += elapsed;
			mElapsedSinceDraw += elapsed;
			mElapsedSinceNXTCommand += elapsed;
			mLastTime = now;

		}

		public void vibrate() {
			mHapticFeedback.vibrate(HAPTIC_FEEDBACK_LENGTH);
			mFeedbackEnd = System.currentTimeMillis() + HAPTIC_FEEDBACK_LENGTH + 15;

		}

	}

	/** used for logging */
	private static final String TAG = GameView.class.getName();;

	private MINDdroid mActivity;

	/** The thread that actually draws the animation */
	private GameThread thread;

	private SensorManager mSensorManager;

	//private Typeface mFont = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

	/** Handle to the application context, used to e.g. fetch Drawables. */
	private Context mContext;

	/** orientation (tilt) readings */
	private float mAccelX = 0;
	private float mAccelY = 0;
	private float mAccelZ = 0; // heading

	/** buffers for averaging to hold calculated screen position */
	private float mNumX;
	private float mNumY;
	
	/** buffers to hold tilt readings for averaging */
	float mNumAcX;
	float mNumAcY;
	

	/** number of tilt readings since last draw */
	private int mNum = 0;
	
	/** number of tilt readings since last draw */
	private int mNumAc =0;

	/** mX value buffer for time between two draws ago and last draw */
	private float mPreviousNumX;

	/** mY value buffer for time between two draws ago and last draw **/
	private float mPreviousNumY;

	/** number of tilt readings for time between two draws ago and last draw **/
	private int mPreviousNum = 0;
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
			//mActivity.updateOrientation(event.values[0], event.values[1], event.values[2], true);

		}

	};

	public GameView(Context context, MINDdroid uiActivity) {
		super(context);
		Log.d(TAG, " ~~~~~~~ UIView ~~~~~~~~");
		mActivity = uiActivity;
		mSensorManager = (SensorManager) mActivity.getSystemService(Context.SENSOR_SERVICE);
		// register our interest in hearing about changes to our surface
		SurfaceHolder holder = getHolder();
		holder.setKeepScreenOn(true);
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

	public GameThread getThread() {
		return thread;
	}

	public boolean isInGoal() {

		if ((getThread().mCanvasWidth - getThread().mTarget.getWidth()) / 2 > getThread().mX
				|| (getThread().mCanvasWidth + getThread().mTarget.getWidth()) / 2 < getThread().mX) {// x is not within goal

			return false;
		}

		if (((getThread().mCanvasHeight - (getThread().mActionButton.getHeight() + (getThread().GOAL_HEIGHT))) / 2 > getThread().mY || ((getThread().mCanvasHeight - getThread().mActionButton
				.getHeight()) + (getThread().GOAL_HEIGHT)) / 2 < getThread().mY)) {
			return false;
		}

		return true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// we only want to handle down events .

		if (event.getAction() == MotionEvent.ACTION_DOWN) {

			if (event.getY() > this.getHeight() - getThread().mActionButton.getHeight()) {
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

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		getThread().setSurfaceSize(width, height);

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		//Log.d(TAG, "surface created");
		// start the thread here so that we don't busy-wait in run()
		// waiting for the surface to be created
		getThread().setRunning(true);
		getThread().start();

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
		getThread().setRunning(false);
		while (retry) {
			try {
				getThread().join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}
	}

	public void unregisterListener() {
		mSensorManager.unregisterListener(mSensorAccelerometer);

	}
	
	private void updateMoveIndicator(float mAcX, float mAcY) {

		getThread().mX = ((getThread().mCanvasWidth / 2)) + (int) ((mAcX / 10) * (getThread().mCanvasWidth / 10));
		mNumX += getThread().mX;
		mNumAcX+=mAcX;

		getThread().mY = (((getThread().mCanvasHeight - getThread().mActionButton.getHeight()) / 2))
				+ (int) ((mAcY / 10) * ((getThread().mCanvasHeight - getThread().mActionButton.getHeight()) / 10));
		mNumY += getThread().mY;
		mNumAcY+=mAcY;

		getThread().mAvCount++;
		mNum++;
		mNumAc++;

	}

}
