package com.lego.minddroid;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

public class EnableBT extends Activity { //currently unused.  Will be implemented to allow connection without user having to say "Yes turn bt on" (when it isn't)

    boolean processStarted = false;
    StatusReciever statusReciever;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(Activity.RESULT_CANCELED);

        statusReciever = new StatusReciever();
        registerReceiver(statusReciever, new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED"));
        processStarted = turnOnBt();
        if (!processStarted) {
            sendFailureStatus();
        }
    }

    private boolean turnOnBt() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH}, BTCommunicator.RESULT_BLUETOOTH_SCAN);  // Comment 26
            } else
                Toast.makeText(this, "Issue with BLUETOOTH_CONNECT connection", Toast.LENGTH_LONG).show();
            return false;
        } else
            return BluetoothAdapter.getDefaultAdapter().enable();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case BTCommunicator.RESULT_BLUETOOTH_CONNECT: // Allowed was selected so Permission granted
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Bluetooth permission granted", Toast.LENGTH_LONG).show();
                } else if (Build.VERSION.SDK_INT >= 23 && !shouldShowRequestPermissionRationale(permissions[0])) {
                    Toast.makeText(this, "Bluetooth BLUETOOTH_SCAN xxx", Toast.LENGTH_LONG).show();
                } else {
                    // User selected Deny Dialog to EXIT App ==> OR <== RETRY to have a second chance to Allow Permissions
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(this, "Bluetooth permission NOT granted", Toast.LENGTH_LONG).show();
                    }
                }
            case BTCommunicator.RESULT_BLUETOOTH_SCAN: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Bluetooth BLUETOOTH_SCAN granted", Toast.LENGTH_LONG).show();
                } else if (Build.VERSION.SDK_INT >= 23 && !shouldShowRequestPermissionRationale(permissions[0])) {
                    Toast.makeText(this, "Bluetooth BLUETOOTH_SCAN granted", Toast.LENGTH_LONG).show();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(this, "Bluetooth BLUETOOTH_SCAN NOT granted", Toast.LENGTH_LONG).show();
                    }
                }
                break;
            }
        }
    }

    public void sendFailureStatus() {
        Log.d("EnableBT", "sendFailureStatus RESULT_CANCELED");
        this.setResult(Activity.RESULT_CANCELED);
        finish();
    }

    public void sendSuccessStatus() {
        Log.d("EnableBT", "sendSuccessStatus RESULT_OK");
        this.setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(statusReciever);
        } catch (Exception e) {
            // not registered

        }
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(statusReciever);
        } catch (Exception e) {
            // not registered
        }
        super.onDestroy();

    }

    public class StatusReciever extends BroadcastReceiver {

        /**
         *
         */

        public final static String STATE_CHANGED = "android.bluetooth.adapter.action.STATE_CHANGED";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("EnableBT statusReciever", "onReceive");
            if (intent.getAction().equals(STATE_CHANGED)) {
                Log.d("EnableBT statusReciever", "ACTION_STATE_CHANGED");
                sendSuccessStatus();
            } else {
                Log.d("EnableBT statusReciever", "fail: " + intent.getAction());
                sendFailureStatus();

            }

        }
    }

}
