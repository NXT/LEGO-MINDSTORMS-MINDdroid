package com.lego.minddroid;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

public class SplashMenu extends Activity {
	
	public static final int DISPLAY_TUTORIAL=0;
	public static final int START=1;
	public static final int CREDITS=2;
	Activity splashMenu;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		// class Eula added from http://code.google.com/p/apps-for-android/source/browse/trunk/DivideAndConquer/src/com/google/android/divideandconquer/Eula.java?r=93
        // Tutorial see http://androiddevstudio.com/tutorials/adding-eula-to-android-app
		Eula.show(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash_menu);
		setupButtons();
		splashMenu=this;
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	
	private void setupButtons() 
    {
      
        
        Button tutorial_button = (Button)this.findViewById(R.id.tutorial);
        tutorial_button.setOnClickListener(
        		new Button.OnClickListener(){

					@Override
					public void onClick(View v) {
					 
						
					}
        			
        		});
        
        Button start_button = (Button)this.findViewById(R.id.start);
        start_button.setOnClickListener(
        		new Button.OnClickListener(){

					@Override
					public void onClick(View v) {
						  
					    Intent playGame = new Intent(splashMenu.getBaseContext(), MINDdroid.class);
					    splashMenu.startActivity(playGame);
 
					}
        			
        		});
        
        Button credits_button = (Button)this.findViewById(R.id.credits);
        credits_button.setOnClickListener(
        		new Button.OnClickListener(){

					@Override
					public void onClick(View v) {
						showCredits();
					      
					}
        			
        		});
        
       
    }
	
	private void showCredits(){
		Info.show(this);
	}
	
}
