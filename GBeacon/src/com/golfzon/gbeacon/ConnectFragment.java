
package com.golfzon.gbeacon;

import java.util.Set;

import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class ConnectFragment extends Fragment implements OnClickListener
{
	private static final String RTTI = "ConnectFragment";
	// 블루투스 메인
	private BluetoothAdapter m_BluetoothAdapter;
	// 페어링된 블루투스 장비
	private ArrayAdapter<String> m_PairedDevicesArrayAdapter;
	// 검색된 블루투스 장비
	private ArrayAdapter<String> m_NewDevicesArrayAdapter;
	
	@Override
	public View onCreateView( LayoutInflater inflater , ViewGroup container , Bundle savedInstanceState )
	{
		// TODO Auto-generated method stub
		View v = inflater.inflate( R.layout.fragment_connect , container , false );
		
        // Initialize array adapters. One for already paired devices and one for newly discovered devices
		m_PairedDevicesArrayAdapter = new ArrayAdapter<String>( getActivity() , R.layout.device_name );
        m_NewDevicesArrayAdapter = new ArrayAdapter<String>( getActivity() , R.layout.device_name );
        
        // Find and set up the ListView for paired devices
        ListView pairedListView = ( ListView )v.findViewById( R.id.conn_paired_devices_list );
        pairedListView.setAdapter( m_PairedDevicesArrayAdapter );
        pairedListView.setOnItemClickListener( m_DeviceClickListener );

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = ( ListView )v.findViewById( R.id.conn_new_devices_list );
        newDevicesListView.setAdapter( m_NewDevicesArrayAdapter );
        newDevicesListView.setOnItemClickListener( m_DeviceClickListener );
        
        // Register for broadcasts when a device is discovered
        IntentFilter filterDiscovered = new IntentFilter( BluetoothDevice.ACTION_FOUND );
        getActivity().registerReceiver( m_Receiver , filterDiscovered );

        // Register for broadcasts when discovery has finished
        IntentFilter filterFinished = new IntentFilter( BluetoothAdapter.ACTION_DISCOVERY_FINISHED );
        getActivity().registerReceiver( m_Receiver , filterFinished );

        m_BluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = m_BluetoothAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if ( pairedDevices.size() > 0 )
        {
//        	v.findViewById( R.id.conn_paired_devices_text ).setVisibility( View.VISIBLE );
        	v.findViewById( R.id.conn_paired_devices_list ).setVisibility( View.VISIBLE );
            
            for ( BluetoothDevice device : pairedDevices )
            {
                m_PairedDevicesArrayAdapter.add( device.getName() + "\n" + device.getAddress() );
            }
        }
        else
        {
            String noDevices = getResources().getText( R.string.none_paired ).toString();
            m_PairedDevicesArrayAdapter.add( noDevices );
        }

        // Start device discover with the BluetoothAdapter
        DoDiscovery( v );
        
        return v;
	}

	@Override
	public void onDestroy()
	{
		// TODO Auto-generated method stub
		super.onDestroy();

        // Make sure we're not doing discovery anymore
		/*
        if ( m_BluetoothAdapter != null )
        {
            m_BluetoothAdapter.cancelDiscovery();
        }
        */

        // Unregister broadcast listeners
		getActivity().unregisterReceiver( m_Receiver );
	}

	@Override
	public void onClick( View v )
	{
		// TODO Auto-generated method stub
		
		switch ( v.getId() )
		{
		}		
	}


     // Start device discover with the BluetoothAdapter
    private void DoDiscovery( View v )
    {
       	Log.d( RTTI , "DoDiscovery()" );

        // Indicate scanning in the title
//        setProgressBarIndeterminateVisibility( true );
        //setTitle( com.golfzon.gbeacon.R.string.scanning );

        // Turn on sub-title for new devices
        v.findViewById( R.id.conn_new_devices_list ).setVisibility( View.VISIBLE );

        // If we're already discovering, stop it
        if ( m_BluetoothAdapter.isDiscovering() )
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
		        	/*
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
		            */
		        }
		    };

    // The BroadcastReceiver that listens for discovered devices and changes the title when discovery is finished
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
		                short rssi = intent.getShortExtra( BluetoothDevice.EXTRA_RSSI , Short.MIN_VALUE );
		                
		                // If it's already paired, skip it, because it's been listed already
		                if ( device.getBondState() != BluetoothDevice.BOND_BONDED )
		                {
		                    m_NewDevicesArrayAdapter.add( device.getName() + "\n" + device.getAddress() + "\t" + rssi + "db" );
		                }
		            }
		            // When discovery is finished, change the Activity title
		            else if ( BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals( action ) )
		            {
//		                setProgressBarIndeterminateVisibility( false );
//		                setTitle( com.golfzon.gbeacon.R.string.select_device );
		                
		                if ( m_NewDevicesArrayAdapter.getCount() == 0 )
		                {
		                    String noDevices = getResources().getText( R.string.none_found ).toString();
		                    m_NewDevicesArrayAdapter.add( noDevices );
		                }
		            }
		        }
		    };
}
