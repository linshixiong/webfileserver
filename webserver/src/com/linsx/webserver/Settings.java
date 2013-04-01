package com.linsx.webserver;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Settings {
	private static SharedPreferences prefs;
	
	public static void init(Context context)
	{
		prefs =PreferenceManager.getDefaultSharedPreferences(context) ;
	}
	
	
	public static String getWebFileMd5()
	{
		return prefs.getString("web_file_md5", "");
	}
	
	public static void setWebFileMd5(String md5)
	{
		prefs.edit().putString("web_file_md5", md5).commit();
	}
	
	public static int getPort()
	{
		return prefs.getInt("server_port", 8080);
	}
	
	public static void setPort(int port){
		prefs.edit().putInt("server_port", port).commit();
	}
}
