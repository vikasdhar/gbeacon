/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.golfzon.gbeacon.bluetooth;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */

public class DeviceListActivity extends Activity
{
    // Debugging
    private static final String RTTI = "DeviceListActivity";
    private static final boolean D = true;

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Member fields
    private BluetoothAdapter m_BluetoothAdapter;
    private ArrayAdapter<String> m_PairedDevicesArrayAdapter;
    private ArrayAdapter<String> m_NewDevicesArrayAdapter;

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        // Setup the window
        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );
        setContentView( com.golfzon.gbeacon.R.layout.device_list );

        // Set result CANCELED in case the user backs out
        setResult( Activity.RESULT_CANCELED );

        // Initialize the button to perform device discovery
        Button scanButton = ( Button )findViewById( com.golfzon.gbeacon.R.id.button_scan );
        scanButton.setOnClickListener(
        		new OnClickListener()
        		{
		            public void onClick( View v )
		            {
		                doDiscovery();
		                v.setVisibility( View.GONE );
		            }
		        }
        	);

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        m_PairedDevicesArrayAdapter = new ArrayAdapter<String>( this , com.golfzon.gbeacon.R.layout.device_name );
        m_NewDevicesArrayAdapter = new ArrayAdapter<String>( this , com.golfzon.gbeacon.R.layout.device_name );

        // Find and set up the ListView for paired devices
        ListView pairedListView = ( ListView )findViewById( com.golfzon.gbeacon.R.id.paired_devices );
        pairedListView.setAdapter( m_PairedDevicesArrayAdapter );
        pairedListView.setOnItemClickListener( m_DeviceClickListener );

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = ( ListView )findViewById( com.golfzon.gbeacon.R.id.new_devices );
        newDevicesListView.setAdapter( m_NewDevicesArrayAdapter );
        newDevicesListView.setOnItemClickListener( m_DeviceClickListener );

        // Register for broadcasts when a device is discovered
        IntentFilter filterDiscovered = new IntentFilter( BluetoothDevice.ACTION_FOUND );
        this.registerReceiver( m_Receiver , filterDiscovered );

        // Register for broadcasts when discovery has finished
        IntentFilter filterFinished = new IntentFilter( BluetoothAdapter.ACTION_DISCOVERY_FINISHED );
        this.registerReceiver( m_Receiver , filterFinished );

        // Get the local Bluetooth adapter
        m_BluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = m_BluetoothAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if ( pairedDevices.size() > 0 )
        {
            findViewById( com.golfzon.gbeacon.R.id.title_paired_devices ).setVisibility( View.VISIBLE );
            
            for ( BluetoothDevice device : pairedDevices )
            {
                m_PairedDevicesArrayAdapter.add( device.getName() + "\n" + device.getAddress() );
            }
        }
        else
        {
            String noDevices = getResources().getText( com.golfzon.gbeacon.R.string.none_paired ).toString();
            m_PairedDevicesArrayAdapter.add( noDevices );
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if ( m_BluetoothAdapter != null )
        {
            m_BluetoothAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver( m_Receiver );
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery()
    {
        if ( D )
        {
        	Log.d(RTTI, "doDiscovery()");
        }

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility( true );
        setTitle( com.golfzon.gbeacon.R.string.scanning );

        // Turn on sub-title for new devices
        findViewById( com.golfzon.gbeacon.R.id.title_new_devices ).setVisibility( View.VISIBLE );

        // If we're already discovering, stop it
        if (m_BluetoothAdapter.isDiscovering() )
        {
            m_BluetoothAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        m_BluetoothAdapter.startDiscovery();
    }

    // The on-click listener for all devices in the ListViews
    private OnItemClickListener m_DeviceClickListener = 
    		new OnItemClickListener()
    		{
		        public void onItemClick( AdapterView<?> av , View v , int arg2 , long arg3 )
		        {
		            // Cancel discovery because it's costly and we're about to connect
		            m_BluetoothAdapter.cancelDiscovery();
		
		            // Get the device MAC address, which is the last 17 chars in the View
		            String info = ( ( TextView )v ).getText().toString();
		            String address = info.substring( info.length() - 17 );
		
		            // Create the result Intent and include the MAC address
		            Intent intent = new Intent();
		            intent.putExtra( EXTRA_DEVICE_ADDRESS , address );
		
		            // Set result and finish this Activity
		            setResult( Activity.RESULT_OK , intent );
		            
		            finish();
		        }
		    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver m_Receiver = 
    		new BroadcastReceiver()
    		{
		        @Override
		        public void onReceive( Context context , Intent intent )
		        {
		            String action = intent.getAction();
		
		            // When discovery finds a device
		            if ( BluetoothDevice.ACTION_FOUND.equals( action ) )
		            {
		                // Get the BluetoothDevice object from the Intent
		                BluetoothDevice device = intent.getParcelableExtra( BluetoothDevice.EXTRA_DEVICE );
		                // If it's already paired, skip it, because it's been listed already
		                if ( device.getBondState() != BluetoothDevice.BOND_BONDED )
		                {
		                    m_NewDevicesArrayAdapter.add( device.getName() + "\n" + device.getAddress() );
		                }
		            }
		            // When discovery is finished, change the Activity title
		            else if ( BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals( action ) )
		            {
		                setProgressBarIndeterminateVisibility( false );
		                setTitle( com.golfzon.gbeacon.R.string.select_device );
		                
		                if ( m_NewDevicesArrayAdapter.getCount() == 0 )
		                {
		                    String noDevices = getResources().getText( com.golfzon.gbeacon.R.string.none_found ).toString();
		                    m_NewDevicesArrayAdapter.add( noDevices );
		                }
		            }
		        }
		    };
}
