package com.lego.minddroid;

/*
 * StartSound is a helper thread for playing the start sound This file is free
 * software; you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation;
 * either version 3 of the License, or (at your option) any later version. This
 * library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

public class StartSound extends Thread {
	private Context myContext;
	AudioManager mAudioManager;

	public StartSound(Context myContext) {
		this.myContext = myContext;
		mAudioManager = (AudioManager) myContext.getSystemService(Context.AUDIO_SERVICE);
	}

	@Override
	public void run() {
		if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
			int i=mAudioManager.getStreamVolume(AudioManager.STREAM_RING);	
			MediaPlayer mp = MediaPlayer.create(myContext, R.raw.startdroid);
			mp.start();
			mp.setVolume( ((float)i)/10f,  ((float)i)/10f);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
			mp.stop();
		}
	}
}
