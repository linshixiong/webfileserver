package com.linsx.webserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class WebFileInstaller {
	private static final String TAG = "WebFileInstaller";

	public static boolean isWebfileInstalled(Context context) {

		File fileDir = context.getFilesDir();
		File webFile = new File(fileDir, "web/index.html");
		Log.d(TAG, "check web file dir " + webFile.getAbsolutePath()
				+ ",exist=" + webFile.exists());
		if(! webFile.exists())
		{
			return false;
		}	
		String md5=getWebFileMd5(context);
	    return	Settings.getWebFileMd5().equals(md5);
		
	}

	public static void installWebfile(Context context, Handler handler) {
		String folderPath = context.getFilesDir().getAbsolutePath() + "/web/";
		InputStream is = context.getResources().openRawResource(R.raw.web);
	   if(unzip(is, folderPath, handler))
	   {
		   String md5=getWebFileMd5(context);
		   Settings.setWebFileMd5(md5);
	   }

	}

	private static String getWebFileMd5(Context context) {
		
		MessageDigest digest = null;
		InputStream in = context.getResources().openRawResource(R.raw.web);
		byte buffer[] = new byte[1024];
		int len;
		try {
			digest = MessageDigest.getInstance("MD5");
			while ((len = in.read(buffer, 0, 1024)) != -1) {
				digest.update(buffer, 0, len);
			}
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		BigInteger bigInt = new BigInteger(1, digest.digest());
		return bigInt.toString(16);

	}

	private static  boolean unzip(InputStream zipFileName, String outputDirectory,
			Handler handler) {
		try {
			ZipInputStream in = new ZipInputStream(zipFileName);
			// ��ȡZipInputStream�е�ZipEntry��Ŀ��һ��zip�ļ��п��ܰ������ZipEntry��
			// ��getNextEntry�����ķ���ֵΪnull�������ZipInputStream��û����һ��ZipEntry��
			// ��������ȡ��ɣ�
			ZipEntry entry = in.getNextEntry();
			while (entry != null) {

				// ������zip���ļ���ΪĿ¼���ĸ�Ŀ¼
				File file = new File(outputDirectory);
				if(!file.exists())
				{
					file.mkdir();
				}
				
				if (entry.isDirectory()) {
					String name = entry.getName();
					name = name.substring(0, name.length() - 1);

					file = new File(outputDirectory + File.separator + name);
					file.mkdir();

				} else {
					file = new File(outputDirectory + File.separator
							+ entry.getName());
					file.createNewFile();
					FileOutputStream out = new FileOutputStream(file);
					int b;
					while ((b = in.read()) != -1) {
						out.write(b);
					}
					out.close();
				}
				// ��ȡ��һ��ZipEntry
				entry = in.getNextEntry();
			}
			in.close();
			
			
			Message msg = new Message();
			msg.what = 0;
			handler.sendMessage(msg);
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Message msg = new Message();
			msg.what = 1;
			handler.sendMessage(msg);
			return false;
		} catch (IOException e) {
			// TODO �Զ����� catch ��
			e.printStackTrace();
			Message msg = new Message();
			msg.what = 1;
			handler.sendMessage(msg);
			return false;
		}
	}

}
