
package com.golfzon.gbeacon;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainFragment extends Fragment implements OnClickListener
{
	private static final String RTTI = "MainFragment";
	
	@Override
	public View onCreateView( LayoutInflater inflater , ViewGroup container , Bundle savedInstanceState )
	{
		// TODO Auto-generated method stub
		View v = inflater.inflate( R.layout.fragment_main , container , false );
		
		// ConnectFragment 로 가는 버는
		Button btn = ( Button )v.findViewById( R.id.btn_connect_fragment );
		btn.setOnClickListener( this );
		
		return v;
	}

	@Override
	public void onClick( View v )
	{
		// TODO Auto-generated method stub
		
		switch ( v.getId() )
		{
			case R.id.btn_connect_fragment:

				( ( MainActivity )getActivity() ).ChangeToConnectFragment();
				
				break;
		}
	}

}
