/*
 * (Changes from original are) Copyright 2010 Guenther Hoelzl, Shawn Brown
 * <p>
 * This file is part of MINDdroid.
 * <p>
 * MINDdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * MINDdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with MINDdroid.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * <p>
 * (original work is) Copyright (C) 2009 The Android Open Source Project
 **/

package com.lego.minddroid;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class DeviceListActivity extends Activity {
    static final String PAIRING = "pairing";

    // Return Intent extra
    public static String DEVICE_NAME_AND_ADDRESS = "device_info";
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize the button to perform device discovery
        Button scanButton = findViewById(R.id.button_scan);
        scanButton.setOnClickListener(v -> {
            doDiscovery();
            v.setVisibility(View.GONE);
        });

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        ArrayAdapter<String> mPairedDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);

        // Find and set up the ListView for paired devices
        ListView pairedListView = findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices;

        // Get a set of currently paired devices
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH}, BTCommunicator.RESULT_BLUETOOTH_CONNECT);  // Comment 26
            } else
                Toast.makeText(this, "Issue with bluetooth connection", Toast.LENGTH_LONG).show();
            return;
        } else
            pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        boolean legoDevicesFound = false;

        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                // only add LEGO devices
                if (device.getAddress().startsWith(BTCommunicator.OUI_LEGO)) {
                    legoDevicesFound = true;
                    mPairedDevicesArrayAdapter.add(device.getName() + "-" + device.getAddress());
                }
            }
        }

        if (!legoDevicesFound) {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    }

    @Override
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.BLUETOOTH}, BTCommunicator.RESULT_BLUETOOTH_SCAN);  // Comment 26
                } else
                    Toast.makeText(this, "Issue with BLUETOOTH_SCAN connection", Toast.LENGTH_LONG).show();
                return;
            } else
                mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // If we're already discovering, stop it
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH}, BTCommunicator.RESULT_BLUETOOTH_SCAN);  // Comment 26
            } else
                Toast.makeText(this, "Issue with BLUETOOTH_SCAN connection", Toast.LENGTH_LONG).show();
        } else if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        } else
            mBtAdapter.startDiscovery();
    }

    // The on-click listener for all devices in the ListViews
    private final OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            String info = ((TextView) v).getText().toString();
            // did we choose a correct name and address?
            if (info.lastIndexOf('-') != info.length() - 18)
                return;

            // Cancel discovery because it's costly and we're about to connect
            if (ActivityCompat.checkSelfPermission(DeviceListActivity.this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.BLUETOOTH}, BTCommunicator.RESULT_BLUETOOTH_SCAN);  // Comment 26
                } else
                    Toast.makeText(DeviceListActivity.this, "Issue with BLUETOOTH_SCAN connection", Toast.LENGTH_LONG).show();
                return;
            } else
                mBtAdapter.cancelDiscovery();

            // Get the device MAC address, this is the text after the last '-' character
            String address = info.substring(info.lastIndexOf('-') + 1);
            // Create the result Intent and include the info
            Intent intent = new Intent();
            Bundle data = new Bundle();
            data.putString(DEVICE_NAME_AND_ADDRESS, info);
            data.putString(EXTRA_DEVICE_ADDRESS, address);
            data.putBoolean(PAIRING, av.getId() == R.id.new_devices);
            intent.putExtras(data);
            // Set result and finish this Activity
            setResult(RESULT_OK, intent);
            finish();
        }
    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (ActivityCompat.checkSelfPermission(DeviceListActivity.this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.BLUETOOTH}, BTCommunicator.RESULT_BLUETOOTH_SCAN);  // Comment 26
                    } else
                        Toast.makeText(DeviceListActivity.this, "Issue with BLUETOOTH_CONNECT connection", Toast.LENGTH_LONG).show();
                } else if (device != null && device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.getName() + "-" + device.getAddress());
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };

}
