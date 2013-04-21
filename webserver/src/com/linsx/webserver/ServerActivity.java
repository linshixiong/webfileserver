package com.linsx.webserver;

import java.lang.reflect.Array;
import java.util.ArrayList;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class ServerActivity extends Activity implements OnClickListener,
		android.content.DialogInterface.OnClickListener, OnCancelListener {

	private static final String TAG = "ServerActivity";
	private ToggleButton toggleButton;
	private TextView textView;
	private TextView textUrl;
	private ConnectivityManager connMgr;
	private String ipAddress;
	private ProgressDialog progress;
	private ImageView img;
	private ImageView imgQrcode;
	private IntentFilter filter;
	private Bitmap mQRbitmap;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_server);

		toggleButton = (ToggleButton) findViewById(R.id.toggleButton1);
		toggleButton.setOnClickListener(this);

		textView = (TextView) findViewById(R.id.textView1);
		textUrl = (TextView) findViewById(R.id.textView2);
		img = (ImageView) findViewById(R.id.imageView1);
		imgQrcode= (ImageView) findViewById(R.id.imageView_QRcode);
		connMgr = (ConnectivityManager) this
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		Settings.init(this);
		Utils.setGlobalContext(this);
		if (!WebFileInstaller.isWebfileInstalled(this)) {
			showInstallProgress();
			installWebFiles();
		}
		filter = new IntentFilter();
		filter.addAction(Intents.ACTION_SERVER_STATE_CHANGE);
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
	}

	@Override
	protected void onResume() {

		this.registerReceiver(receiver, filter);
		refreshUIState();

		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.server, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			Intent intent=new Intent();
			intent.setClass(this, SettingsActivity.class);
			this.startActivity(intent);
			break;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	private void refreshUIState() {
		if (!Utils.isSdCardMounted()) {
			Toast.makeText(this, R.string.storage_off, Toast.LENGTH_LONG)
					.show();
		}

		if (!Utils.isNetworkActive(connMgr)) {

			textView.setText(R.string.wifi_off);
			textView.setTextColor(Color.RED);
			img.setImageResource(R.drawable.signal_off);
			toggleButton.setEnabled(false);
			textUrl.setVisibility(View.GONE);
			imgQrcode.setVisibility(View.GONE);
			return;

		}
		toggleButton.setEnabled(true);
		if (HttpService.isRunning()) {
			ipAddress = Utils.getLocalIpAddress();

			textView.setText(R.string.server_on);
			textView.setTextColor(Color.argb(255, 0x22, 0x8b, 0x22));
			img.setImageResource(R.drawable.signal_on);
			toggleButton.setChecked(true);
			textUrl.setVisibility(View.VISIBLE);
			String url=String.format("http://%s:%d", ipAddress,Settings.getPort());
			textUrl.setText(url);
			createQRcode(url);

		} else {
			textView.setText(R.string.server_off);
			textView.setTextColor(Color.RED);
			img.setImageResource(R.drawable.signal_off);
			toggleButton.setChecked(false);
			textUrl.setVisibility(View.GONE);
			imgQrcode.setVisibility(View.GONE);
		}

	}

	@Override
	protected void onPause() {
		this.unregisterReceiver(receiver);
		super.onPause();
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			if (Intents.ACTION_SERVER_STATE_CHANGE.equals(intent.getAction())) {
				refreshUIState();

			}

			else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent
					.getAction())) {

				refreshUIState();

			}
		}

	};

	private void showInstallProgress() {
		progress = new ProgressDialog(this);
		progress.setTitle(R.string.web_file_extract);
		progress.setCanceledOnTouchOutside(false);
		progress.setCancelable(false);
		progress.setOnCancelListener(this);
		progress.show();
	}

	@Override
	public void onCancel(DialogInterface dialog) {

		// Toast.makeText(this, "cancel", Toast.LENGTH_SHORT).show();
	}

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				if (progress != null) {
					progress.dismiss();
				}
				break;
			case 1:
				ServerActivity.this.finish();
				break;
			case 2:
				if(HttpService.isRunning())
				{
					imgQrcode.setVisibility(View.VISIBLE);
					imgQrcode.setImageBitmap(mQRbitmap);
				}
				break;
			default:
				break;
			}
		}

	};

	private void installWebFiles() {
		new Thread() {

			@Override
			public void run() {
				WebFileInstaller.installWebfile(ServerActivity.this, handler);
			}

		}.start();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {

		this.finish();

	}



	@Override
	public void onClick(View v) {

		Intent intent = new Intent();
		intent.setClass(this, HttpService.class);
		if (!HttpService.isRunning()) {
			startService(intent);
		} else {
			stopService(intent);

		}
	}
	
	
	private void createQRcode(final String url){
		new Thread(){

		

			@Override
			public void run() {
				
				mQRbitmap=Utils.createQRImage(url,250,250);
				
				if(mQRbitmap!=null){
					Message msg=new Message();
					msg.what=2;
					handler.sendMessage(msg);
				}
				super.run();
			}
			
			
			
		}.start();
	}

}
