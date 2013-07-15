package com.linsx.webserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;
import android.widget.Toast;

public class Receiver extends BroadcastReceiver {

	private static final String TAG = "Webserver.Receiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){
			Log.d(TAG, "boot completed");
			Settings.init(context);
			if(!Settings.isServerAutoStart()){
				Log.d(TAG, "webserver will not auto start because is forbidden");
				return;
			}
			if(!WebFileInstaller.isWebfileExist(context)){
				Log.d(TAG, "webserver will not auto start because is web file is not exist");
				return;
			}
			
			Utils.setGlobalContext(context);

			ConnectivityManager connMgr=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
			if(Utils.isNetworkActive(connMgr)){
				
				if (!HttpService.isRunning()) {
					Log.d(TAG, "webserver will auto start");
					Intent service = new Intent();
					service.setClass(context, HttpService.class);
					context.startService(service);

				}else {
					Log.d(TAG, "webserver will not auto start because it is already running");
				}
			}else {
				Log.d(TAG, "webserver will not auto start because network is not active");
			}
			
			
		}
		
	}

}
