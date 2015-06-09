/*
 * Copyright (C) 2011-2013 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of Spydroid (http://code.google.com/p/spydroid-ipcamera/)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.raygo.spydroid.ui;

import java.util.Locale;
import com.raygo.spydroid.R;
import com.raygo.spydroid.SpydroidApplication;
import com.raygo.spydroid.Utilities;
import com.raygo.streaming.rtsp.RtspServer;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class HandsetFragment extends Fragment {

	public final static String TAG = "HandsetFragment";

    private TextView mInfo, mAdvice, mTextBitrate;
    private Animation mPulseAnimation;
    private Button mShowInfo;
    private ToggleButton mStartService;
    
    private SpydroidApplication mApplication;
    private RtspServer mRtspServer;
    private Context mContext;
    private Intent intent;
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	mApplication  = (SpydroidApplication) getActivity().getApplication();
    	mContext = mApplication.getApplicationContext();
    	intent = new Intent(mContext,RtspServer.class);
    }
    
    
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View rootView = inflater.inflate(R.layout.main,container,false);
        mInfo = (TextView)rootView.findViewById(R.id.info);
        mAdvice = (TextView)rootView.findViewById(R.id.advice);
        mPulseAnimation = AnimationUtils.loadAnimation(mApplication.getApplicationContext(), R.anim.pulse);
        mTextBitrate = (TextView)rootView.findViewById(R.id.bitrate);
        mStartService = (ToggleButton)rootView.findViewById(R.id.StartService);
        //mShowInfo = (Button)rootView.findViewById(R.id.btInfo);
        mStartService.setOnClickListener(clicklistener);
        //mShowInfo.setOnClickListener(clicklistener);
        return rootView ;
    }
	
	private OnClickListener clicklistener = new OnClickListener(){
		@Override
		public void onClick(View view) {
			// TODO Auto-generated method stub
			int btid = view.getId();
			if(btid == R.id.StartService){
				if (mRtspServer == null){
					mApplication.startService(intent);
				}
				else{
					mApplication.stopService(intent);
					mRtspServer= null;
				}
			}
			else
			{	
				if (mRtspServer != null) {
					if (mRtspServer.isStreaming()){
						Toast.makeText(mContext, "Is Streaming", Toast.LENGTH_LONG).show();
					}else{
						Toast.makeText(mContext, "Not Streaming", Toast.LENGTH_LONG).show();
					}
				}else{
					Toast.makeText(mContext, "Null", Toast.LENGTH_LONG).show();
				}
			}
		}};
	
	
	@Override
    public void onStart() {
    	super.onStart();
    	mApplication.bindService(intent, mRtspServiceConnection, Context.BIND_AUTO_CREATE);
		//Toast.makeText(mContext, "Bind service", Toast.LENGTH_SHORT).show();
    }
    
	@Override
	public void onStop(){
		super.onStop();
		
		//if (mRtspServer != null) mRtspServer.removeCallbackListener(mRtspCallbackListener);
		mApplication.unbindService(mRtspServiceConnection);
	}
	
	@Override
    public void onPause() {
    	super.onPause();
    	update();
    	getActivity().unregisterReceiver(mWifiStateReceiver);
    	getActivity().unbindService(mRtspServiceConnection);
    }
	
	@Override
    public void onResume() {
    	super.onResume();
		getActivity().bindService(new Intent(getActivity(),RtspServer.class), mRtspServiceConnection, Context.BIND_AUTO_CREATE);
    	getActivity().registerReceiver(mWifiStateReceiver,new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }
	
	private final ServiceConnection mRtspServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mRtspServer = (RtspServer) ((RtspServer.LocalBinder)service).getService();
			mRtspServer.addCallbackListener(mRtspCallbackListener);
			mRtspServer.start();
			update();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			update();
		}
		
	};
    
	
	private RtspServer.CallbackListener mRtspCallbackListener = new RtspServer.CallbackListener() {

		@Override
		public void onError(RtspServer server, Exception e, int error) {
			// We alert the user that the port is already used by another app.
			if (error == RtspServer.ERROR_BIND_FAILED) {
				new AlertDialog.Builder(mContext)
				.setTitle(R.string.port_used)
				.setMessage(getString(R.string.bind_failed, "RTSP"))
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int id) {
						startActivityForResult(new Intent(mContext, OptionsActivity.class),0);
					}
				})
				.show();
			}
		}

		@Override
		public void onMessage(RtspServer server, int message) {
			if (message==RtspServer.MESSAGE_STREAMING_STARTED) {
				update();
			} else if (message==RtspServer.MESSAGE_STREAMING_STOPPED) {
				update();
			}
		}

	};
	
    // BroadcastReceiver that detects wifi state changements
    private final BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	String action = intent.getAction();
        	// This intent is also received when app resumes even if wifi state hasn't changed :/
        	if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
        		update();
        	}
        } 
    };
    
	public void update() {
		getActivity().runOnUiThread(new Runnable () {
			@Override
			public void run() {
				if (mRtspServer != null) {
					if (!mRtspServer.isStreaming()) 
						displayIpAddress();
					else 
						streamingState(1);
				}
				else{
					streamingState(3);
				}
			}
		});
	}
	
	private void streamingState(int state) {
		if (state==0) {
			// Not streaming
			mAdvice.setVisibility(View.VISIBLE);
			mAdvice.clearAnimation();
			mAdvice.setText(R.string.showrtspurl);			
			mTextBitrate.setVisibility(View.INVISIBLE);
		} else if (state==1) {
			// Streaming
			//mAdvice.clearAnimation();
			mAdvice.setVisibility(View.GONE);
			mTextBitrate.setVisibility(View.VISIBLE);
			mHandler.post(mUpdateBitrate);
		} else if (state==2) {
			// No wifi !
			mAdvice.setVisibility(View.VISIBLE);
			mAdvice.setText(R.string.nonet);			
			mAdvice.startAnimation(mPulseAnimation);
			mTextBitrate.setVisibility(View.INVISIBLE);
		}
		else if (state==3) {
			// No Server !
			mAdvice.setVisibility(View.VISIBLE);
			mAdvice.setText(R.string.noserver);
			mAdvice.startAnimation(mPulseAnimation);
			mTextBitrate.setVisibility(View.INVISIBLE);
		}
	}
	
    private void displayIpAddress() {
		WifiManager wifiManager = (WifiManager) mApplication.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifiManager.getConnectionInfo();
		String ipaddress = null;
    	if (info!=null && info.getNetworkId()>-1) {
	    	int i = info.getIpAddress();
	        String ip = String.format(Locale.ENGLISH,"%d.%d.%d.%d", i & 0xff, i >> 8 & 0xff,i >> 16 & 0xff,i >> 24 & 0xff);
	    	mInfo.setText("rtsp://");
	    	mInfo.append(ip);
	    	mInfo.append(":"+mRtspServer.getPort());
	    	streamingState(0);
    	} else if((ipaddress = Utilities.getLocalIpAddress(true)) != null) {
	    	mInfo.setText("rtsp://");
	    	mInfo.append(ipaddress);
	    	mInfo.append(":"+mRtspServer.getPort());
	    	streamingState(0);
    	} else {
    		streamingState(2);
    	}
    	
    }    
    
	private final Handler mHandler = new Handler();
    
	private Runnable mUpdateBitrate = new Runnable() {
		@Override
		public void run() {
			if ((mRtspServer != null && mRtspServer.isStreaming()) ) {
				long bitrate = 0;
				bitrate += mRtspServer!=null?mRtspServer.getBitrate():0;
				//bitrate += mHttpServer!=null?mHttpServer.getBitrate():0;
				mTextBitrate.setText(""+bitrate/1000+" kbps");
				mHandler.postDelayed(mUpdateBitrate, 1000);
			} else {
				mTextBitrate.setText("0 kbps");
			}
		}
	};
	
}
