package com.linsx.webserver;

import java.io.*;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

/**
 * An example of subclassing NanoHTTPD to make a custom HTTP server.
 */
public class HttpService extends Service {
	protected static final String TAG = "HttpService";
	private static NanoHTTPD httpd;
	private PowerManager pm;
	private PowerManager.WakeLock wl;
	public String fileRoot;
	private NotificationManager mNotificationManager;
	//public static int port = 8080;
	private IntentFilter filter;
	private ConnectivityManager connMgr;
	private Builder builder;


	
	@Override
	public IBinder onBind(Intent intent) {

		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		fileRoot = getResources().getString(R.string.config_file_root);
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		if (pm != null) {
			wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NanoHTTPD");
		}
		mNotificationManager = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);
		connMgr = (ConnectivityManager) this
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		filter = new IntentFilter();
		filter.addAction(Intents.ACTION_SERVER_STATE_CHANGE);
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		filter.addAction(Intents.ACTION_RESTART_SERVER);

		this.registerReceiver(receiver, filter);
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@SuppressLint({ "NewApi", "DefaultLocale" })
		@Override
		public void onReceive(Context context, Intent intent) {

			if (Intents.ACTION_SERVER_STATE_CHANGE.equals(intent.getAction())) {

				if (isRunning()) {
					showNotificaction();
				} else {
					mNotificationManager.cancel(100);
				}

			}
			else if(Intents.ACTION_RESTART_SERVER.equals(intent.getAction())){
				if(httpd!=null){
					httpd.setPort(Settings.getPort());
					httpd.restart();
				}
			}
			else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent
					.getAction())) {

				if (!isRunning() || builder == null) {
					return;
				}

				if (Utils.isNetworkActive(connMgr)) {

					builder.setContentTitle(getString(R.string.notification_title));
					String ipAddress = Utils.getLocalIpAddress();
					String url = String.format("http://%s:%d", ipAddress, Settings.getPort());
					builder.setContentText(getString(
							R.string.notification_content, url));

					@SuppressWarnings("deprecation")
					Notification notification = builder.getNotification();

					mNotificationManager.notify(100, notification);

				} else {
					builder.setContentTitle(getString(R.string.server_suspend));

					builder.setContentText(getString(R.string.wifi_off));
					@SuppressWarnings("deprecation")
					Notification notification = builder.getNotification();

					mNotificationManager.notify(100, notification);

				}

			}

		
		}

	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		File wwwRoot = new File(this.getFilesDir(), "web");

		try {
		
			httpd = new NanoHTTPD(this, Settings.getPort(), wwwRoot);
		
			if (wl != null) {
				wl.acquire();

			}
		} catch (IOException e) {

			e.printStackTrace();
			this.stopSelf();
		}

		return super.onStartCommand(intent, flags, startId);
	}

	public static boolean isRunning() {
		return httpd == null ? false : httpd.isRunning();
	}

	@Override
	public void onDestroy() {
		httpd.stop();
		if (wl != null) {
			wl.release();
		}
		mNotificationManager.cancel(100);
		unregisterReceiver(receiver);
		super.onDestroy();

	}

	/**
	 * Ìí¼ÓÒ»¸önotification
	 */
	@SuppressLint("NewApi")
	private void showNotificaction() {

		builder = new Notification.Builder(this);

		builder.setSmallIcon(R.drawable.share);

		Intent intent = new Intent(this, ServerActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);

		builder.setOngoing(true);
		builder.setAutoCancel(false);

		builder.setContentTitle(getString(R.string.notification_title));
		String ipAddress = Utils.getLocalIpAddress();
		String url = String.format("http://%s:%d", ipAddress, Settings.getPort());
		builder.setContentText(getString(R.string.notification_content, url));
		builder.setContentIntent(pendingIntent);
		// builder.setDefaults( Notification.DEFAULT_SOUND);
		Notification notification = builder.getNotification();

		mNotificationManager.notify(100, notification);

	}

}
