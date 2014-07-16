
package com.golfzon.gbeacon;

import com.golfzon.gbeacon.bluetooth.BluetoothManager;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity implements OnClickListener
{
	private static final String RTTI = "MainActivity";
	
	int m_iCurrentFragment;
	
	final int FRAGMENT_MAIN = 0;
	final int FRAGMENT_CONNECT = 1;
	
	//
	// Bluetooth
	//
	

    BluetoothManager m_BluetoothMgr;
	private final Handler m_Handler = new Handler()
	{
		public void handleMessage( Message msg )
		{
			super.handleMessage( msg );
		}
	};

	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		m_iCurrentFragment = FRAGMENT_MAIN;
		
		// Menu
		Button btnMenuMain = ( Button )findViewById( R.id.menu_main_fragment );
		btnMenuMain.setOnClickListener( this );

		Button btnMenuConnect = ( Button )findViewById( R.id.menu_connect_fragment );
		btnMenuConnect.setOnClickListener( this );

		if ( savedInstanceState == null )
		{
			ChangeFragment( m_iCurrentFragment );
		}
		
		if ( m_BluetoothMgr == null )
		{
			m_BluetoothMgr = new BluetoothManager( this , m_Handler );
			// 기기의 블루투스 켜기
			m_BluetoothMgr.EnableBluetooth();
		}
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		
		int id = item.getItemId();
		
		if ( id == R.id.action_settings )
		{
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onActivityResult( int requestCode , int resultCode , Intent data )
	{
		// TODO Auto-generated method stub
		
		switch ( requestCode )
		{
			case GBeacon.REQUEST_ENABLE_BLUETOOTH:
				
				if ( resultCode == Activity.RESULT_OK )
				{
					Log.d( RTTI , "Bluetooth is enabled" );
				}
				else
				{
					Log.d( RTTI , "Bluetooth is not enabled" );
				}
				
				break;
				
			case GBeacon.REQUEST_CONNECT_DEVICE:
				
				if ( resultCode == Activity.RESULT_OK )
				{
					m_BluetoothMgr.GetDeviceInfo( data );
				}
				
				break;
		}
	}

	public void ChangeFragment( int iFragment )
	{
		Fragment fragment = GetFragment( iFragment );
		
		if ( fragment != null )
		{
			/*
			FragmentTransaction t = getFragmentManager().beginTransaction();
			// replace fragment
			t.replace( R.id.container , fragment );
			// Commit the transaction
			t.commit();
			*/
			getFragmentManager().beginTransaction().replace( R.id.container , fragment ).commit();
		}
	}
	
	public Fragment GetFragment( int i )
	{
		Fragment fragment = null;
		
		switch ( i )
		{
			case FRAGMENT_MAIN:
				
				fragment = new MainFragment();
				
				break;
				
			case FRAGMENT_CONNECT:
				
				fragment = new ConnectFragment();
				
				break;
		}
		
		return fragment;
	}
	
	public void ChangeToMainFragment()
	{
		m_iCurrentFragment = FRAGMENT_MAIN;
		ChangeFragment( m_iCurrentFragment );
	}

	public void ChangeToConnectFragment()
	{
		m_iCurrentFragment = FRAGMENT_CONNECT;
		ChangeFragment( m_iCurrentFragment );
	}

	@Override
	public void onClick( View arg0 )
	{
		// TODO Auto-generated method stub
		switch ( arg0.getId() )
		{
			case R.id.menu_main_fragment:
				
				ChangeToMainFragment();
				
				break;
				
			case R.id.menu_connect_fragment:
				
				ChangeToConnectFragment();
				
				
				
				break;
		}
	}
}
