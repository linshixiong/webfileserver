package com.linsx.webserver;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;

public class Utils {
	private static final String QRCODE_API = "http://chart.apis.google.com/chart?cht=qr&chs=250x250&chl=%s";

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

	// ����QRͼ
    public static Bitmap createQRImage(String text,int width,int height) {
        try {
            // ��Ҫ����core��
            QRCodeWriter writer = new QRCodeWriter();
 
            if (text == null || "".equals(text) || text.length() < 1) {
                return null;
            }

            // ��������ı�תΪ��ά��
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
		// httpGet���Ӷ���
		HttpGet httpRequest = new HttpGet(String.format(QRCODE_API,str));
		// ȡ��HttpClient ����
		HttpClient httpclient = new DefaultHttpClient();
		Bitmap bitmap = null;
		try {
			// ����httpClient ��ȡ��HttpRestponse
			HttpResponse httpResponse = httpclient.execute(httpRequest);
			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				// ȡ�������Ϣ ȡ��HttpEntiy
				HttpEntity httpEntity = httpResponse.getEntity();
				// ���һ��������
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

	public static Bitmap getimage(String srcPath) {
		BitmapFactory.Options newOpts = new BitmapFactory.Options();
		// ��ʼ����ͼƬ����ʱ��options.inJustDecodeBounds ���true��
		newOpts.inJustDecodeBounds = true;
		Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);// ��ʱ����bmΪ��

		newOpts.inJustDecodeBounds = false;
		int w = newOpts.outWidth;
		int h = newOpts.outHeight;
		// ���������ֻ��Ƚ϶���800*480�ֱ��ʣ����ԸߺͿ���������Ϊ
		float hh = 800f;// �������ø߶�Ϊ800f
		float ww = 480f;// �������ÿ��Ϊ480f
		// ���űȡ������ǹ̶��������ţ�ֻ�ø߻��߿�����һ�����ݽ��м��㼴��
		int be = 1;// be=1��ʾ������
		if (w > h && w > ww) {// �����ȴ�Ļ����ݿ�ȹ̶���С����
			be = (int) (newOpts.outWidth / ww);
		} else if (w < h && h > hh) {// ����߶ȸߵĻ����ݿ�ȹ̶���С����
			be = (int) (newOpts.outHeight / hh);
		}
		if (be <= 0)
			be = 1;
		newOpts.inSampleSize = be;// �������ű���
		// ���¶���ͼƬ��ע���ʱ�Ѿ���options.inJustDecodeBounds ���false��
		bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
		return bitmap;
	}
}
