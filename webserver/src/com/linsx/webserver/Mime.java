package com.linsx.webserver;

import java.util.Hashtable;
import java.util.StringTokenizer;

public class Mime {
	/**
	 * Hashtable mapping (String)FILENAME_EXTENSION -> (String)MIME_TYPE
	 */
	public static Hashtable<String, String> theMimeTypes = new Hashtable<String, String>();
	static {
		StringTokenizer st = new StringTokenizer("css		text/css "
				+ "htm		text/html " + "html		text/html " + "xml		text/xml "
				+ "txt		text/plain " + "asc		text/plain " + "gif		image/gif "
				+ "jpg		image/jpeg " + "jpeg		image/jpeg " + "png		image/png "
				+ "bmp image/bmp " + "mp3		audio/mpeg " + "wma audio/wma "
				+ "m3u		audio/mpeg-url " + "mp4		video/mp4 "
				+ "ogv		video/ogg " + "flv		video/x-flv "
				+ "mov		video/quicktime " + "3gp  video/3gp "
				+ "rmvb  video/rmvb " + "swf		application/x-shockwave-flash "
				+ "js			application/javascript " + "pdf		application/pdf "
				+ "doc		application/msword " + "ogg		application/x-ogg "
				+ "zip		application/octet-stream "
				+ "exe		application/octet-stream "
				+ "class		application/octet-stream ");
		while (st.hasMoreTokens())
			theMimeTypes.put(st.nextToken(), st.nextToken());
	}

}
