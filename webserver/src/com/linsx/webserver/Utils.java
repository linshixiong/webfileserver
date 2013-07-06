package com.linsx.webserver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.util.InetAddressUtils;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import android.R.raw;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.IWindowManager;
import android.os.ServiceManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.os.RemoteException;
import android.content.Intent;
import android.widget.Toast;

public class Utils {
	private static final String QRCODE_API = "http://chart.apis.google.com/chart?cht=qr&chs=250x250&chl=%s";
	private static final String TAG = "Utils";
	private static Context sContext;

	// private static IWindowManager sWindowManager;
	public static void setGlobalContext(Context context) {

		sContext = context;
	}

	public static Context getGlobalContext() {
		return sContext;
	}

	public static String getLocalIpAddress() {
		try {
			String ipv4;

			ArrayList<NetworkInterface> mylist = Collections
					.list(NetworkInterface.getNetworkInterfaces());

			for (NetworkInterface ni : mylist) {

				ArrayList<InetAddress> ialist = Collections.list(ni
						.getInetAddresses());
				for (InetAddress address : ialist) {
					if (!address.isLoopbackAddress()
							&& InetAddressUtils.isIPv4Address(ipv4 = address
									.getHostAddress())) {
						return ipv4;
					}
				}

			}

		} catch (SocketException ex) {

		}
		return null;
	}

	public static boolean isSdCardMounted() {
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED))
			return true;
		else
			return false;
	}

	public static boolean isNetworkActive(ConnectivityManager connMgr) {

		if (HttpService.DEBUG) {
			return true;
		}

		NetworkInfo network = connMgr.getActiveNetworkInfo();
		if (network == null) {
			return false;
		}
		return network.isAvailable();
	}

	// 生成QR图
	public static Bitmap createQRImage(String text, int width, int height) {
		try {
			// 需要引入core包
			QRCodeWriter writer = new QRCodeWriter();

			if (text == null || "".equals(text) || text.length() < 1) {
				return null;
			}

			// 把输入的文本转为二维码
			BitMatrix martix = writer.encode(text, BarcodeFormat.QR_CODE,
					width, height);

			System.out.println("w:" + martix.getWidth() + "h:"
					+ martix.getHeight());

			Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
			hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
			BitMatrix bitMatrix = new QRCodeWriter().encode(text,
					BarcodeFormat.QR_CODE, width, height, hints);
			int[] pixels = new int[width * height];
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					if (bitMatrix.get(x, y)) {
						pixels[y * width + x] = 0xff000000;
					} else {
						pixels[y * width + x] = 0xffffffff;
					}

				}
			}

			Bitmap bitmap = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);

			bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
			return bitmap;
		} catch (WriterException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Bitmap Create2DCode(String str) {
		// httpGet连接对象
		HttpGet httpRequest = new HttpGet(String.format(QRCODE_API, str));
		// 取得HttpClient 对象
		HttpClient httpclient = new DefaultHttpClient();
		Bitmap bitmap = null;
		try {
			// 请求httpClient ，取得HttpRestponse
			HttpResponse httpResponse = httpclient.execute(httpRequest);
			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				// 取得相关信息 取得HttpEntiy
				HttpEntity httpEntity = httpResponse.getEntity();
				// 获得一个输入流
				InputStream is = httpEntity.getContent();

				bitmap = BitmapFactory.decodeStream(is);
				is.close();

			}

		} catch (ClientProtocolException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}

		return bitmap;

	}

	public static void mkdir(String uri, File homeDir, String dir) {
		Log.d(TAG, "mkdir uri=" + uri + ",dir=" + dir + ",home=" + homeDir);
		if (dir == null || dir.trim().equals("")) {
			return;
		}
		File root = new File(homeDir, uri);
		if (root.exists() && root.canWrite()) {
			File newDir = new File(root, dir);
			newDir.mkdirs();
		}
	}

	public static void deleteFile(String uri, File homeDir, String file) {

		Log.d(TAG, "delete uri=" + uri + ",file=" + file + ",home=" + homeDir);
		if (file == null || file.trim().equals("")) {
			return;
		}
		File root = new File(homeDir, uri);
		if (root.exists() && root.canWrite()) {
			File targetFile = new File(root, file);
			targetFile.delete();
		}
	}

	/**
	 * Send a single key event.
	 * 
	 * @param event
	 *            is a string representing the keycode of the key event you want
	 *            to execute.
	 */
	public static void sendKeyEvent(String event, Context context) {
		int eventCode = Integer.parseInt(event);
		if (eventCode == KeyEvent.KEYCODE_POWER) {
			powerOff(context);

			return;
		}
		if (eventCode == -KeyEvent.KEYCODE_HOME) {
			showAllAppList(context);
			return;
		}
        long now = SystemClock.uptimeMillis();
        Log.i("SendKeyEvent", event);
        try {
            KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, eventCode, 0);
            KeyEvent up = new KeyEvent(now, now, KeyEvent.ACTION_UP, eventCode, 0);
            (IWindowManager.Stub
                .asInterface(ServiceManager.getService("window")))
                .injectKeyEvent(down, true);
            (IWindowManager.Stub
                .asInterface(ServiceManager.getService("window")))
                .injectKeyEvent(up, true);
        } catch (RemoteException e) {
            Log.i("Input", "DeadOjbectException");
        }
	}
	public static void showAllAppList(Context context) {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.addCategory("com.softwinner.category.app");
		intent.setClassName("com.softwinner.launcher",
				"com.softwinner.launcher.Launcher");
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		try {
			context.startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public static void powerOff(Context context) {
		Intent intent = new Intent(Intent.ACTION_REQUEST_SHUTDOWN);// 之所以能够在源码中查看，但是调用的时候不显示，是因为这个不对上层开放

		intent.putExtra(Intent.EXTRA_KEY_CONFIRM, false);

		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		context.startActivity(intent);

	}

	public static boolean startActivitySafely(Context context, Intent intent) {
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		try {
			context.startActivity(intent);
			return true;
		} catch (ActivityNotFoundException e) {
			// Toast.makeText(context, R.string.unable_to_launch,
			// Toast.LENGTH_SHORT).show();
			Log.e(TAG, "Unable to launch intent=" + intent, e);
		} catch (SecurityException e) {
			// Toast.makeText(context, R.string.unable_to_launch,
			// Toast.LENGTH_SHORT).show();
			Log.e(TAG,
					"Launcher does not have the permission to launch "
							+ intent
							+ ". Make sure to create a MAIN intent-filter for the corresponding activity "
							+ "or use the exported attribute for this activity.intent="
							+ intent, e);
		}
		return false;
	}

	public static File getHomeDir(String uri) {

		if (sContext == null) {
			return null;
		}
		if (uri.equals("/") || uri.equals("")) {
			return null;
		}
		List<String> deviceList = DeviceManager.getInstance(sContext)
				.getMountedDevicesList();
		if (deviceList != null) {
			for (String volume : deviceList) {
				File file = new File(volume);
				if (uri.startsWith("/" + file.getName())) {
					return file.getParentFile();
				}
			}
		}

		return null;
	}

	public static List<File> getDeviceVolumes() {
		if (sContext == null) {
			return null;
		}
		ArrayList<File> files = new ArrayList<File>();
		List<String> deviceList = DeviceManager.getInstance(sContext)
				.getMountedDevicesList();
		if (deviceList != null) {

			for (String s : deviceList) {
				files.add(new File(s));
			}
		}

		return files;
	}

	public static String getExtensionName(String filename) {
		if ((filename != null) && (filename.length() > 0)) {
			int dot = filename.lastIndexOf('.');
			if ((dot > -1) && (dot < (filename.length() - 1))) {
				return filename.substring(dot + 1);
			}
		}
		return filename;
	}

	public static String getGalleryListString(List<File> files) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < files.size(); ++i) {

			File file = files.get(i);
			sb.append(String.format("{ url: '%s', caption: '%s'},\n",
					file.getName(), file.getName()));
		}

		return sb.toString();
	}

	public static String getFileListString(List<File> files, String uri,
			boolean isVolumeList) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < files.size(); ++i) {

			File file = files.get(i);
			String data_icon = file.isDirectory() ? "folder" : "file";
			if (uri.equals("/")) {
				uri = "";
			}

			String href = file.isDirectory() ? uri + "/"
					+ files.get(i).getName() + "/" : "#";
			href = href.replace("//", "/");
			String img_src = null;

			if (file.isDirectory()) {
				img_src = "/images/folder.png?webroot=true";
			} else {
				img_src = "/images/file.png?webroot=true";

				String extensionName = Utils.getExtensionName(file.getName());

				String mime = Mime.theMimeTypes
						.get(extensionName.toLowerCase());
				Log.d(TAG, "getMimeTypeFromExtension " + mime + " form "
						+ extensionName);
				if (mime != null) {

					if (mime.startsWith("video")) {
						img_src = "/images/video.png?webroot=true";
					} else if (mime.startsWith("audio")) {
						img_src = "/images/audio.png?webroot=true";
					} else if (mime.startsWith("image")) {
						img_src = "/images/image.png?webroot=true";
					}

				}

			}

			if (isVolumeList) {
				sb.append(String
						.format("<li><a href=\"%s\" data-ajax=\"false\"><img src=\"/images/folder.png?webroot=true\"  class=\"ui-li-icon ui-corner-none\">%s</a></li>",
								href, file.getName()));
			} else {
				sb.append(String
						.format("<li onclick='FileitemClick(this)' isDirectory='%b'"
								+ " fileSize='%d'"
								+ " data-icon='%s' >"
								+ "<a href='%s' "
								+ " data-transition='none' "
								+ "><img src='%s'"
								+ " alt='%s' class='ui-li-icon' />%s</a></li>\n",
								file.isDirectory(), file.isDirectory() ? -1
										: file.length(), data_icon, href,
								img_src, file.getName(), file.getName()));
			}

		}
		return sb.toString();
	}

}
