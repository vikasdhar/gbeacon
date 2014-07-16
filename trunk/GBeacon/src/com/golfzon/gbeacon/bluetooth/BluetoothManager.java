
package com.golfzon.gbeacon.bluetooth;

import com.golfzon.gbeacon.GBeacon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

public class BluetoothManager
{
	private static final String RTTI = "BluetoothService";
	private static final UUID GBEACON_UUID = UUID.fromString( "00001101-0000-1000-8000-00805F9B34FB" );
    
	private Activity m_Activity;
	private Handler m_Handler;
	
	// 블루투스 메인
	private BluetoothAdapter m_BluetoothAdapter;
	
	// thread 상태
	private int m_iState;
	// we're doing nothing
	private static final int STATE_NONE = 0;
	// now listening for incoming connections
	private static final int STATE_LISTEN = 1;
	// now initiating an outgoing connection
	private static final int STATE_CONNECTING = 2;
	// now connected to a remote device
	private static final int STATE_CONNECTED = 3;
	
	// 연결 thread
	private ConnectThread m_ConnectThread;
	// 연결된 thread
	private ConnectedThread m_ConnectedThread;

	// constructor
	public BluetoothManager( Activity activity , Handler handler )
	{
		m_Activity = activity;
		m_Handler = handler;
		
		m_BluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	// 블루투스 지원 여부 체크
	public boolean GetDeviceState()
	{
		if ( m_BluetoothAdapter == null )
		{
			return false;
		}
		
		return true;
	}
	
	// 기기의 플루투스를 켜기
	public void EnableBluetooth()
	{
		if ( m_BluetoothAdapter.isEnabled() == false )
		{
			Log.d( RTTI , "Request to enable Bluetooth" );
			
			Intent i = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
			m_Activity.startActivityForResult( i , GBeacon.REQUEST_ENABLE_BLUETOOTH );
		}
	}
	
	// 블루투스 기기 검색
	public void ScanDevice()
	{
		Log.d( RTTI , "Scan Device" );
		
		Intent serverIntent = new Intent( m_Activity , DeviceListActivity.class );
		m_Activity.startActivityForResult( serverIntent , GBeacon.REQUEST_CONNECT_DEVICE );
	}
	
	//
	public void GetDeviceInfo( Intent data )
	{
	    // Get the device MAC address
	    String address = data.getExtras().getString( DeviceListActivity.EXTRA_DEVICE_ADDRESS );
	    // Get the BluetoothDevice object
	    //BluetoothDevice device = btAdapter.getRemoteDevice(address);
	    BluetoothDevice device = m_BluetoothAdapter.getRemoteDevice( address );
	     
	    Log.d( RTTI , "Get Device Info \n" + "address : " + address );
	 
	    Connect( device );
	}
	
	private synchronized void SetState( int iState )
	{
		Log.d( RTTI , "Change state" + m_iState + " -> " + iState );
		
		m_iState = iState;
	}
	
	public synchronized int GetState()
	{
		return m_iState;
	}
	
	public synchronized void Reset()
	{
		// Cancel any thread attempting to make a connection
		if ( m_ConnectThread != null )
		{
			m_ConnectThread.Cancel();
			m_ConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if ( m_ConnectedThread != null )
		{
			m_ConnectedThread.Cancel();
			m_ConnectedThread = null;
		}
	}
	
	public synchronized void Start()
	{
		Log.d( RTTI , "Start" );
		
		Reset();
	}
	
	// ConnectThread 초기화 device의 모든 연결 제거
	public synchronized void Connect( BluetoothDevice device )
	{
		Log.d( RTTI , "Connect to : " + device );
		
		// Cancel any thread attempting to make a connection
		if ( m_iState == STATE_CONNECTING )
		{
			if ( m_ConnectThread != null )
			{
				m_ConnectThread.Cancel();
				m_ConnectThread = null;
			}
		}

		// Cancel any thread currently running a connection
		if ( m_ConnectedThread != null )
		{
			m_ConnectedThread.Cancel();
			m_ConnectedThread = null;
		}
		
		// Start the thread to connect with the given device
		m_ConnectThread = new ConnectThread( device );
		m_ConnectThread.start();
		
		SetState( STATE_CONNECTING );		
	}
	
	// ConnectedThread 초기화
	public synchronized void Connected( BluetoothSocket socket , BluetoothDevice device )
	{
		Log.d( RTTI , "Connected" );
		
		// Cancel the thread that completed the connection
		if ( m_ConnectThread != null )
		{
			m_ConnectThread.Cancel();
			m_ConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if ( m_ConnectedThread != null )
		{
			m_ConnectedThread.Cancel();
			m_ConnectedThread = null;
		}
		
		// Start the thread to manage the connection and perform transmissions
		m_ConnectedThread = new ConnectedThread( socket );
		m_ConnectedThread.start();
		
		SetState( STATE_CONNECTED );
	}
	
	// 모든 thread stop
	public synchronized void Stop()
	{
		Log.d( RTTI , "Stop thread" );

		Reset();
		
		SetState( STATE_NONE );
	}
	
	// 값을 쓰는 부분(보내는 부분)
	public void Write( byte [] buffer )
	{
		// Create temporary object
		ConnectedThread t;
		
		// Synchronize a copy of the ConnectedThread
		synchronized ( this )
		{
			if ( m_iState != STATE_CONNECTED )
			{
				return;
			}
			
			t = m_ConnectedThread;
		}
		
		// Perform the write unsynchronized
		t.Write( buffer );
	}
	
	// 연결 실패했을때
	private void ConnectionFailed()
	{
		SetState( STATE_LISTEN );
	}
	
	// 연결을 잃었을 때 
	private void ConnectionLost()
	{
		SetState( STATE_LISTEN );
	}
	
	private class ConnectThread extends Thread
	{
		private final BluetoothSocket m_Socket;
		private final BluetoothDevice m_Device;
		
		public ConnectThread( BluetoothDevice device )
		{
			m_Device = device;
			
			BluetoothSocket socket = null;
			
			try
			{
				socket = device.createRfcommSocketToServiceRecord( GBEACON_UUID );
			}
			catch ( IOException e )
			{
				Log.e( RTTI , "Can't create socket" , e );
			}
			
			m_Socket = socket;
		}
		
		@Override
		public void run()
		{
			Log.i( RTTI , "Begin run" );
			setName( "ConnectThread" );
			
			// 연결을 시도하기 전에는 항상 기기 검색을 중지한다.
			// 기기 검색이 계속되면 연결속도가 느려지기 때문이다.
			m_BluetoothAdapter.cancelDiscovery();
			
			// BluetoothSocket 연결 시도
			try
			{
				m_Socket.connect();
				
				Log.d( RTTI , "Success connection" );
			}
			catch ( IOException eConnect )
			{
				// BluetoothSocket 연결 시도에 대한 return 값은 succes 또는 exception이다.
				Log.d( RTTI , "Can't connect" );
				
				ConnectionFailed();
				
				// socket을 닫는다.
				try
				{
					m_Socket.close();
				}
				catch ( IOException eClose )
				{
					Log.e( RTTI , "Can't close socket" , eClose );
				}
				
				// 연결중 혹은 연결 대기상태인 메소드를 호출
				BluetoothManager.this.Start();
				
				return;
			}
			
			//
			synchronized ( BluetoothManager.this )
			{
				m_ConnectThread = null;
			}
			
			// 
			Connected( m_Socket , m_Device );
		}
		
		public void Cancel()
		{
			try
			{
				m_Socket.close();
			}
			catch ( IOException e )
			{
				Log.e( RTTI , "Can't close socket" , e );
			}
		}
	}
	
	private class ConnectedThread extends Thread
	{
		private final BluetoothSocket m_Socket;
		private final InputStream m_InputStream;
		private final OutputStream m_OutputStream;
		private static final int BUFFER_SIZE = 1024;
		
		public ConnectedThread( BluetoothSocket socket )
		{
			Log.d( RTTI , "Create ConnectedThread" );
			
			m_Socket = socket;
			
			InputStream input = null;
			
			try
			{
				input = socket.getInputStream();
			}
			catch ( IOException e )
			{
				Log.e( RTTI , "Can't get inputstream" , e );
			}
			
			OutputStream output = null;

			try
			{
				output = socket.getOutputStream();
			}
			catch ( IOException e )
			{
				Log.e( RTTI , "Can't get outputstream" , e );
			}
			
			m_InputStream = input;
			m_OutputStream = output;
		}
		
		@Override
		public void run()
		{
			// TODO Auto-generated method stub
			
			Log.i( RTTI , "Begin run" );

			byte [] buffer = new byte[BUFFER_SIZE];
			int iBytes;
			
			// Keep listening to the InputStream while connected
			while ( true )
			{
				try
				{
					// InputStream으로부터 값을 받는 읽는 부분(값을 받는다)
					iBytes = m_InputStream.read( buffer );
				}
				catch ( IOException e )
				{
					Log.e( RTTI , "Disconnected" , e );
					
					ConnectionLost();
					
					break;
				}
			}
		}
		
		public void Write( byte [] buffer )
		{
			try
			{
				m_OutputStream.write( buffer );
			}
			catch ( IOException e )
			{
				Log.e( RTTI , "Exception during write" , e );
			}
		}

		public void Cancel()
		{
			try
			{
				m_Socket.close();
			}
			catch ( IOException e )
			{
				Log.e( RTTI , "Can't close socket" , e );
			}
		}
	}
}
