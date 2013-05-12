package com.linsx.webserver;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class NanoHTTPD {
	private int myTcpPort;
	private ServerSocket myServerSocket;
	private Thread myThread;
	private File wwwroot;
	private static final String TAG = "NanoHTTPD";
	private Context mContext;


	/**
	 * HTTP response. Return one of these from serve().
	 */
	public class Response {
		/**
		 * Default constructor: response = HTTP_OK, data = mime = 'null'
		 */
		public Response() {
			this.status = HTTP_OK;
		}

		/**
		 * Basic constructor.
		 */
		public Response(String status, String mimeType, InputStream data) {
			this.status = status;
			this.mimeType = mimeType;
			this.data = data;
		}

		/**
		 * Convenience method that makes an InputStream out of given text.
		 */
		public Response(String status, String mimeType, String txt) {
			this.status = status;
			this.mimeType = mimeType;
			try {
				this.data = new ByteArrayInputStream(txt.getBytes("UTF-8"));
			} catch (java.io.UnsupportedEncodingException uee) {
				uee.printStackTrace();
			}
		}

		/**
		 * Adds given line to the header.
		 */
		public void addHeader(String name, String value) {
			header.put(name, value);
		}

		/**
		 * HTTP status code after processing, e.g. "200 OK", HTTP_OK
		 */
		public String status;

		/**
		 * MIME type of content, e.g. "text/html"
		 */
		public String mimeType;

		/**
		 * Data of the response, may be null.
		 */
		public InputStream data;

		/**
		 * Headers for the HTTP response. Use addHeader() to add lines.
		 */
		public Properties header = new Properties();
	}

	/**
	 * Some HTTP response status codes
	 */
	public static final String HTTP_OK = "200 OK",
			HTTP_PARTIALCONTENT = "206 Partial Content",
			HTTP_RANGE_NOT_SATISFIABLE = "416 Requested Range Not Satisfiable",
			HTTP_REDIRECT = "301 Moved Permanently",
			HTTP_NOTMODIFIED = "304 Not Modified",
			HTTP_FORBIDDEN = "403 Forbidden", HTTP_NOTFOUND = "404 Not Found",
			HTTP_BADREQUEST = "400 Bad Request",
			HTTP_INTERNALERROR = "500 Internal Server Error",
			HTTP_NOTIMPLEMENTED = "501 Not Implemented";

	/**
	 * Common mime types for dynamic content
	 */
	public static final String MIME_PLAINTEXT = "text/plain",
			MIME_HTML = "text/html",
			MIME_DEFAULT_BINARY = "application/octet-stream",
			MIME_XML = "text/xml";

	public NanoHTTPD(Context context, int port, File wwwroot)
			throws IOException {
		myTcpPort = port;
		this.wwwroot = wwwroot;
		this.mContext = context;
		start();
	}

	public void start() {

		try {
			myServerSocket = new ServerSocket(myTcpPort);
			myThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						while (true)
							new HTTPSession(myServerSocket.accept());
					} catch (IOException ioe) {
					}
				}
			});
			myThread.setDaemon(true);
			myThread.start();

		} catch (IOException e) {
			Toast.makeText(
					mContext,
					mContext.getResources().getString(
							R.string.server_port_occupied, Settings.getPort()),
					Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		} finally {

			Intent intent = new Intent(Intents.ACTION_SERVER_STATE_CHANGE);
			mContext.sendBroadcast(intent);

		}

	}

	public void stop() {
		try {
			if (myServerSocket != null) {
				myServerSocket.close();
			}
			if (myThread != null) {
				myThread.join();
			}
			Intent intent = new Intent(Intents.ACTION_SERVER_STATE_CHANGE);

			mContext.sendBroadcast(intent);
		} catch (IOException ioe) {
		} catch (InterruptedException e) {
		}
	}

	public void restart() {
		Log.d(TAG, "restart");
		stop();
		try {
			Thread.sleep(500);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		start();

	}

	public boolean isRunning() {
		return myServerSocket == null ? false : !myServerSocket.isClosed();
	}

	public int getPort() {
		return myTcpPort;
	}

	public void setPort(int port) {

		if (this.myTcpPort != port) {
			this.myTcpPort = port;

		}

	}

	private class HTTPSession implements Runnable {
		public HTTPSession(Socket s) {
			mySocket = s;
			Thread t = new Thread(this);
			t.setDaemon(true);
			t.start();
		}

		@Override
		public void run() {
			try {

				long timeStart = System.currentTimeMillis();
				InputStream is = mySocket.getInputStream();
				if (is == null)
					return;

				final int bufsize = 8192;
				byte[] buf = new byte[bufsize];
				int splitbyte = 0;
				int rlen = 0;
				{
					int read = is.read(buf, 0, bufsize);
					while (read > 0) {
						rlen += read;
						splitbyte = findHeaderEnd(buf, rlen);
						if (splitbyte > 0) {
							break;
						}
						read = is.read(buf, rlen, bufsize - rlen);
					}
				}
				Log.d(TAG, "findHeaderEnd,splitbyte=" + splitbyte + ",rlen="
						+ rlen);
				// Create a BufferedReader for parsing the header.
				ByteArrayInputStream hbis = new ByteArrayInputStream(buf, 0,
						rlen);
				BufferedReader hin = new BufferedReader(new InputStreamReader(
						hbis));

				Properties pre = new Properties();
				Properties parms = new Properties();
				Properties header = new Properties();

				// Decode the header into parms and header java properties
				decodeHeader(hin, pre, parms, header);
				String method = pre.getProperty("method");
				if (method == null) {
					sendError(HTTP_INTERNALERROR,
							"SERVER INTERNAL ERROR: method is undefined.");
					return;
				}
				String uri = pre.getProperty("uri");
				Log.d(TAG, "pre.getProperty(\"uri\")=" + uri);
							 
				long size = 0x7FFFFFFFFFFFFFFFl;
				String contentLength = header.getProperty("content-length");

				if (contentLength != null) {
					try {
						size = Long.parseLong(contentLength);
					} catch (NumberFormatException ex) {
					}
				}

				// Write the part of body already read to ByteArrayOutputStream
				ByteArrayOutputStream f = new ByteArrayOutputStream();
				if (splitbyte < rlen) {
					f.write(buf, splitbyte, rlen - splitbyte);
					Log.d(TAG, "fbuf write=" + f.size());

				}
				if (splitbyte < rlen) {
					size -= rlen - splitbyte + 1;
				} else if (splitbyte == 0 || size == 0x7FFFFFFFFFFFFFFFl) {
					size = 0;
				}

				//buf = new byte[bufsize];

				File homeDir = null;
				boolean webRoot = Boolean.parseBoolean(parms
						.getProperty("webroot"));
				String action = parms.getProperty("action", "").trim()
						.toLowerCase();

				if (webRoot) {
					homeDir = wwwroot;
				} else {
					homeDir = Utils.getHomeDir(uri);
				}

				if (method.equalsIgnoreCase("POST")) {

					String contentType = "";
					String contentTypeHeader = header
							.getProperty("content-type");
					Log.d(TAG, "contentTypeHeader=" + contentTypeHeader);
					if (contentTypeHeader == null) {
						contentType = "multipart/form-data";
					} else {
						StringTokenizer st = new StringTokenizer(
								contentTypeHeader, "; ");
						if (st.hasMoreTokens()) {
							contentType = st.nextToken();
						}
					}
					Log.d(TAG, "post uri=" + uri);

					if (contentType.equalsIgnoreCase("multipart/form-data")) {
						Log.d(TAG, "post file");
						Log.d(TAG, "contentLength=" + contentLength + "size="
								+ size);
						
						if (size > 0) {
							rlen = is.read(buf, 0, bufsize);
							size -= rlen;
							if (rlen > 0) {
								f.write(buf, 0, rlen);

							}
						}
						byte[] fbuf = f.toByteArray();
						if (fbuf.length > 0) {
							Properties fileInfos = new Properties();
							int offset = stripMultipartHeaders(fbuf, fileInfos);

							String fileName = fileInfos.getProperty("filename",
									"unknown.file");
							Log.d(TAG, "Multipart Header offset=" + offset);
							String boundary = contentTypeHeader
									.contains("boundary=") ? contentTypeHeader
									.substring(contentTypeHeader
											.lastIndexOf("boundary=") + 9)
									: null;
							Log.d(TAG, "boundary=" + boundary);
							File rootDir = new File(homeDir, uri);
							if (!rootDir.exists()) {
								rootDir.mkdirs();
							}
							File outFile = new File(rootDir, fileName);

							FileOutputStream out = new FileOutputStream(outFile);

							int endOffset = findMultipartEnd(f, offset,
									boundary);
							Log.d(TAG, "endOffset=" + endOffset);
							if (endOffset < 0) {
								out.write(f.toByteArray(), offset, f.size()
										- offset);
							} else {
								out.write(f.toByteArray(), offset, endOffset
										- offset);
							}
							final int fileBufSize = 81920;
							byte[] fileBuf = new byte[fileBufSize];
							//long sizeUnreaded = size - f.size();
							ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

							while (true) {
								if (size <= 0) {
									break;
								}

								if (size <= fileBufSize * 2) {

									int read = is.read(fileBuf, 0, fileBufSize);
									if (read > 0) {
										byteArrayOutputStream.write(fileBuf, 0,
												read);
										size -= read;
										Log.d(TAG, "size readed 2:" + read);
									} else {
										break;
									}

								} else {

									int read = is.read(fileBuf, 0, fileBufSize);
									if (read > 0) {
										out.write(fileBuf, 0, read);
										size -= read;
										Log.d(TAG, "size readed 1:" + read);
									} else {
										break;
									}
								}
							}

							if (byteArrayOutputStream.size() > 0) {
								endOffset = findMultipartEnd(
										byteArrayOutputStream, 0, boundary);
								out.write(byteArrayOutputStream.toByteArray(),
										0, endOffset);
							}
							out.close();
							if (size > 0) {
								Log.d(TAG,
										"Upload canceled:" + outFile.getName());
								outFile.delete();
							} else {
								Log.d(TAG,
										"Upload finished:" + outFile.getName());
							}
						}
					} else {
						// Handle application/x-www-form-urlencoded
						while (rlen >= 0 && size > 0) {
							rlen = is.read(buf, 0, 512);
							size -= rlen;
							if (rlen > 0) {
								f.write(buf, 0, rlen);
							}
						}

						// Get the raw body as a byte []
						byte[] fbuf = f.toByteArray();

						// Create a BufferedReader for easily reading it as
						// string.
						ByteArrayInputStream bin = new ByteArrayInputStream(
								fbuf);
						BufferedReader in = new BufferedReader(
								new InputStreamReader(bin));

						String postLine = "";
						char pbuf[] = new char[512];
						int read = in.read(pbuf);
						while (read >= 0 && !postLine.endsWith("\r\n")) {
							postLine += String.valueOf(pbuf, 0, read);
							read = in.read(pbuf);
						}
						postLine = postLine.trim();
						decodeParms(postLine, parms);

						String mkdir = parms.getProperty("mkdir", "").trim();
						if (!mkdir.equals("")) {
							Utils.mkdir(uri, homeDir, mkdir);
						}
						String deleteFile = parms.getProperty("delete", "")
								.trim();
						if (!deleteFile.equals("")) {
							Utils.deleteFile(uri, homeDir, deleteFile);
						}
					}
				}

				Response r = serveFile(uri, method, header, homeDir, action);
				if (r == null) {
					Log.d(TAG, "HTTP_INTERNALERROR");
					sendError(HTTP_INTERNALERROR,
							"SERVER INTERNAL ERROR: Serve() returned a null response.");
				} else {

					sendResponse(r.status, r.mimeType, r.header, r.data);
				}
				is.close();
				long timeSpan = System.currentTimeMillis() - timeStart;

				Log.d(TAG, "session end ,method=" + method + ", uri=" + uri
						+ ",timeSpan=" + timeSpan);
			} catch (IOException ioe) {
				try {

					sendError(
							HTTP_INTERNALERROR,
							"SERVER INTERNAL ERROR: IOException: "
									+ ioe.getMessage());
				} catch (Throwable t) {
					t.printStackTrace();
				}
				ioe.printStackTrace();
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}

		

		/**
		 * Decodes the sent headers and loads the data into java Properties' key
		 * - value pairs
		 **/
		private void decodeHeader(BufferedReader in, Properties pre,
				Properties parms, Properties header)
				throws InterruptedException {
			try {
				// Read the request line
				String inLine = in.readLine();
				if (inLine == null)
					return;
				StringTokenizer st = new StringTokenizer(inLine);
				if (!st.hasMoreTokens())
					sendError(HTTP_BADREQUEST,
							"BAD REQUEST: Syntax error. Usage: GET /example/file.html");

				String method = st.nextToken();
				pre.put("method", method);

				if (!st.hasMoreTokens())
					sendError(HTTP_BADREQUEST,
							"BAD REQUEST: Missing URI. Usage: GET /example/file.html");

				String uri = st.nextToken();

				// Decode parameters from the URI
				int qmi = uri.indexOf('?');
				if (qmi >= 0) {
					decodeParms(uri.substring(qmi + 1), parms);
					uri = decodePercent(uri.substring(0, qmi));
				} else
					uri = decodePercent(uri);

				// If there's another token, it's protocol version,
				// followed by HTTP headers. Ignore version but parse headers.
				// NOTE: this now forces header names lowercase since they are
				// case insensitive and vary by client.
				if (st.hasMoreTokens()) {
					String line = in.readLine();
					while (line != null && line.trim().length() > 0) {
						int p = line.indexOf(':');
						if (p >= 0)
							header.put(line.substring(0, p).trim()
									.toLowerCase(), line.substring(p + 1)
									.trim());
						line = in.readLine();
					}
				}

				pre.put("uri", uri);
			} catch (IOException ioe) {
				sendError(
						HTTP_INTERNALERROR,
						"SERVER INTERNAL ERROR: IOException: "
								+ ioe.getMessage());
			}
		}

		/**
		 * Find byte index separating header from body. It must be the last byte
		 * of the first two sequential new lines.
		 **/
		private int findHeaderEnd(final byte[] buf, int rlen) {
			int splitbyte = 0;
			while (splitbyte + 3 < rlen) {
				if (buf[splitbyte] == '\r' && buf[splitbyte + 1] == '\n'
						&& buf[splitbyte + 2] == '\r'
						&& buf[splitbyte + 3] == '\n')
					return splitbyte + 4;
				splitbyte++;
			}
			return 0;
		}

		private int findMultipartEnd(
				final ByteArrayOutputStream byteArrayOutputStream, int offset,
				String boundy) {
			Log.d(TAG,
					"removeMultipartEnd size " + byteArrayOutputStream.size());

			int[] bpositions = getBoundaryPositions(
					byteArrayOutputStream.toByteArray(), offset,
					boundy.getBytes());

			if (bpositions != null && bpositions.length > 0) {
				for (int i : bpositions) {
					Log.d(TAG, "bpositions " + i);
				}

				return bpositions[0] - 4;
			} else {
				return -1;
			}

		}

		/**
		 * Find the byte positions where multipart boundaries start.
		 */
		public int[] getBoundaryPositions(byte[] b, int offset, byte[] boundary) {
			int matchcount = 0;
			int matchbyte = -1;
			List<Integer> matchbytes = new ArrayList<Integer>();
			for (int i = offset; i < b.length; i++) {
				if (b[i] == boundary[matchcount]) {
					if (matchcount == 0)
						matchbyte = i;
					matchcount++;
					if (matchcount == boundary.length) {
						matchbytes.add(matchbyte);
						matchcount = 0;
						matchbyte = -1;
					}
				} else {
					i -= matchcount;
					matchcount = 0;
					matchbyte = -1;
				}
			}
			int[] ret = new int[matchbytes.size()];
			for (int i = 0; i < ret.length; i++) {
				ret[i] = matchbytes.get(i);
			}
			return ret;
		}

		/**
		 * It returns the offset separating multipart file headers from the
		 * file's data.
		 **/
		private int stripMultipartHeaders(byte[] b, Properties fileInfos) {

			ByteArrayInputStream hbis = new ByteArrayInputStream(b, 0, b.length);

			BufferedReader hin = new BufferedReader(new InputStreamReader(hbis));
			int index = 0;

			while (true) {
				String line = null;

				try {
					line = hin.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}

				if (line == null) {

					break;
				} else {
					Log.d(TAG,
							"line [" + line + "]" + ",length=" + line.length());

					index += line.getBytes().length + 2;
					if (line.contains("Content-Type:")) {
						break;
					}
					if (line.contains("filename=")) {

						String[] splits = line.split(";");
						for (int i = 0; i < splits.length; i++) {
							String fileName = splits[i];
							if (fileName.contains("filename=")) {
								fileName = fileName.substring(11,
										fileName.length() - 1);
								fileInfos.put("filename", fileName);
								Log.d(TAG, "upload file name=" + fileName);
								break;
							}
						}

					}
				}

			}
			return index + 2;

		}

		/**
		 * Decodes the percent encoding scheme. <br/>
		 * For example: "an+example%20string" -> "an example string"
		 */
		private String decodePercent(String str) throws InterruptedException {
			
			Log.d(TAG, "decodePercent "+str);
			
			return URLDecoder.decode(str);
			
		}

		/**
		 * Decodes parameters in percent-encoded URI-format ( e.g.
		 * "name=Jack%20Daniels&pass=Single%20Malt" ) and adds them to given
		 * Properties. NOTE: this doesn't support multiple identical keys due to
		 * the simplicity of Properties -- if you need multiples, you might want
		 * to replace the Properties with a Hashtable of Vectors or such.
		 */
		private void decodeParms(String parms, Properties p)
				throws InterruptedException {
			if (parms == null)
				return;

			StringTokenizer st = new StringTokenizer(parms, "&");
			while (st.hasMoreTokens()) {
				String e = st.nextToken();
				int sep = e.indexOf('=');
				if (sep >= 0)
					p.put(decodePercent(e.substring(0, sep)).trim(),
							decodePercent(e.substring(sep + 1)));
			}
		}

		/**
		 * Returns an error message as a HTTP response and throws
		 * InterruptedException to stop further request processing.
		 */
		private void sendError(String status, String msg)
				throws InterruptedException {
			sendResponse(status, MIME_PLAINTEXT, null,
					new ByteArrayInputStream(msg.getBytes()));
			throw new InterruptedException();
		}

		/**
		 * Sends given response to the socket.
		 */
		private void sendResponse(String status, String mime,
				Properties header, InputStream data) {
			try {
				if (status == null)
					throw new Error("sendResponse(): Status can't be null.");

				OutputStream out = mySocket.getOutputStream();
				PrintWriter pw = new PrintWriter(out);
				pw.print("HTTP/1.0 " + status + " \r\n");

				if (mime != null)
					pw.print("Content-Type: " + mime + "\r\n");

				if (header == null || header.getProperty("Date") == null)
					pw.print("Date: " + gmtFrmt.format(new Date()) + "\r\n");

				if (header != null) {
					Enumeration e = header.keys();
					while (e.hasMoreElements()) {
						String key = (String) e.nextElement();
						String value = header.getProperty(key);
						pw.print(key + ": " + value + "\r\n");
					}
				}

				pw.print("\r\n");
				pw.flush();

				if (data != null) {
					int pending = data.available(); // This is to support
													// partial sends, see
													// serveFile()
					byte[] buff = new byte[theBufferSize];
					while (pending > 0) {
						int read = data.read(buff, 0,
								((pending > theBufferSize) ? theBufferSize
										: pending));
						if (read <= 0)
							break;
						out.write(buff, 0, read);
						pending -= read;
					}
				}
				out.flush();
				out.close();
				if (data != null)
					data.close();
			} catch (IOException ioe) {
				// Couldn't write? No can do.
				try {
					mySocket.close();
				} catch (Throwable t) {
				}
			}
		}

		private Socket mySocket;
	}


	


	/**
	 * Serves file from homeDir and its' subdirectories (only). Uses only URI,
	 * ignores all headers and HTTP parameters.
	 */
	public Response serveFile(String uri, String method, Properties header,
			File homeDir, String action) {
		Response res = null;
		Log.d(TAG, "serveFile");
		boolean isIEBrowser = false;
		String userAngentString = header.getProperty("user-agent");
		
		Log.d(TAG, "userAngentString:" + userAngentString);

		if (userAngentString != null) {
			isIEBrowser = userAngentString.toLowerCase().contains("msie");
		}
		
		
		String userLanguageString=Locale.getDefault().getLanguage();
		Log.d(TAG, "userLanguageString:" + userLanguageString);
		String language="";
		if(userLanguageString!=null){
			
			
			if(userLanguageString.equals("zh")){
				if("cn".equalsIgnoreCase(Locale.getDefault().getCountry()))
				{	
					language="zh_CN";//简体中文
				}else {
					language="zh_TW";//繁体中文
				}
			}
			else if(userLanguageString.equals("ja")) {
				language="ja";//日文
			}
		}
		
		
		Log.d(TAG, "isIEBrowser:" + isIEBrowser);
		Log.d(TAG, "language :"+language);
		// Make sure we won't die of an exception later
		if (homeDir != null && !homeDir.isDirectory()) {
			res = new Response(HTTP_INTERNALERROR, MIME_PLAINTEXT,
					"INTERNAL ERRROR: serveFile(): given homeDir is not a directory.");
			return res;
		}

		// Remove URL arguments
		uri = uri.trim().replace(File.separatorChar, '/');
		if (uri.indexOf('?') >= 0) {
			uri = uri.substring(0, uri.indexOf('?'));
		}

		// Prohibit getting out of current directory
		if (uri.startsWith("..") || uri.endsWith("..")
				|| uri.indexOf("../") >= 0) {
			res = new Response(HTTP_FORBIDDEN, MIME_PLAINTEXT,
					"FORBIDDEN: Won't serve ../ for security reasons.");
			return res;
		}
		File f = null;
		if (homeDir != null) {
			f = new File(homeDir, uri);
			if (!f.exists()) {
				res = new Response(HTTP_NOTFOUND, MIME_PLAINTEXT,
						"Error 404, file not found.");
				return res;
			}
		}

		// List the directory, if necessary
		if (homeDir == null || f.isDirectory()) {

			if (homeDir == null && !uri.equals("/")) {

				res = new Response(HTTP_REDIRECT, MIME_HTML,
						"<html><body>Redirected: <a href=\"/\"</a></body></html>");
				res.addHeader("Location", "/");
				return res;

			}
			if (!uri.endsWith("/")) {
				uri += "/";

				res = new Response(HTTP_REDIRECT, MIME_HTML,
						"<html><body>Redirected: <a href=\"" + uri + "\">"
								+ uri + "</a></body></html>");
				res.addHeader("Location", uri);
				return res;

			}
			File indexFileRoot=new File(wwwroot,language);
			File indexFile = new File(indexFileRoot, isIEBrowser ? "index_ie.html"
					: "index.html");

			String msg = null;

			if (indexFile.exists()) {
				FileFilter filter = new FileFilter() {

					@Override
					public boolean accept(File pathname) {
						if (pathname.getName().startsWith(".")) {
							return false;
						} else {
							return true;
						}
					}
				};

				List<File> files = null;
				if (f == null) {
					files =Utils.getDeviceVolumes();
				} else if (f.canRead()) {
					files = Arrays.asList(f.listFiles(filter));

					Collections.sort(files, new Comparator<File>() {
						@Override
						public int compare(File o1, File o2) {
							if (o1.isDirectory() && o2.isFile())
								return -1;
							if (o1.isFile() && o2.isDirectory())
								return 1;
							return o1.getName().toLowerCase()
									.compareTo(o2.getName().toLowerCase());
						}
					});
				} else {
					files = new ArrayList<File>();
				}

				try {

					StringBuilder sb = new StringBuilder();

					if (method.equalsIgnoreCase("post")) {
						sb.append(Utils.getFileListString(files, uri));
					} else {
						BufferedReader br = new BufferedReader(new FileReader(
								indexFile));
						while (true) {
							String r = br.readLine();
							if (r == null) {
								break;
							}

							if (r.trim().equalsIgnoreCase("{file_list_data}")) {
								r = Utils.getFileListString(files, uri);

							}
							sb.append(r);
							sb.append("\n");

						}

					}

					msg = sb.toString();

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

			} else {
				msg = "index.html not exist";
			}
			res = new Response(HTTP_OK, MIME_HTML, msg);

			return res;
		}

		else {
			try {

				// Get MIME type from file name extension, if possible
				String mime = null;

				int dot = f.getCanonicalPath().lastIndexOf('.');
				if (dot >= 0)
					mime = Mime.theMimeTypes.get(f.getCanonicalPath()
							.substring(dot + 1).toLowerCase());
				if (mime == null)
					mime = MIME_DEFAULT_BINARY;
				Log.d(TAG, "mime=" + mime);

				if (action.endsWith("play")) {

					if (mime.contains("image")) {
						File indexFileRoot=new File(wwwroot,language);
						File galleryHtml = new File(indexFileRoot, "gallery.html");

						if (galleryHtml.exists()) {
							FileFilter filter = new FileFilter() {

								@Override
								public boolean accept(File pathname) {
									int dot = 0;
									try {
										dot = pathname.getCanonicalPath()
												.lastIndexOf('.');
										if (dot <= 0) {
											return false;
										}
										String type = Mime.theMimeTypes.get(pathname
												.getCanonicalPath()
												.substring(dot + 1)
												.toLowerCase());
										if (type == null) {
											return false;
										}
										if (type.contains("image")) {
											return true;
										} else {
											return false;
										}
									} catch (IOException e) {

										e.printStackTrace();
										return false;
									}

								}
							};

							List<File> files = null;
							if (f.canRead()) {
								files = Arrays.asList(f.getParentFile()
										.listFiles(filter));

								Collections.sort(files, new Comparator<File>() {
									@Override
									public int compare(File o1, File o2) {
										if (o1.isDirectory() && o2.isFile())
											return -1;
										if (o1.isFile() && o2.isDirectory())
											return 1;
										return o1
												.getName()
												.toLowerCase()
												.compareTo(
														o2.getName()
																.toLowerCase());
									}
								});
							} else {
								files = new ArrayList<File>();
							}

							String msg = null;
							try {

								StringBuilder sb = new StringBuilder();

								BufferedReader br = new BufferedReader(
										new FileReader(galleryHtml));
								while (true) {
									String r = br.readLine();
									if (r == null) {
										break;
									}

									if (r.trim().contains("{gallery_items}")) {

										r = Utils.getGalleryListString(files);
									}
									if (r.trim().contains("instance.show")) {

										for (int i = 0; i < files.size(); i++) {
											File file = files.get(i);
											if (file.getName().equals(
													f.getName())) {
												r = "instance.show(" + i + ")";
												break;
											}
										}

									}

									sb.append(r);
									sb.append("\n");

								}

								msg = sb.toString();

							} catch (FileNotFoundException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
							res = new Response(HTTP_OK, MIME_HTML, msg);
							return res;
						}
					}

					else if (mime.contains("audio/mpeg")
							|| mime.contains("mp4")) {
						File indexFileRoot=new File(wwwroot,language);
						File playerHtml = new File(indexFileRoot, "player.html");

						if (playerHtml.exists()) {
							String msg = null;

							try {

								StringBuilder sb = new StringBuilder();

								BufferedReader br = new BufferedReader(
										new FileReader(playerHtml));
								while (true) {
									String r = br.readLine();
									if (r == null) {
										break;
									}

									if (r.trim().equalsIgnoreCase(
											"{player_item}")) {
										String type = mime.contains("audio") ? "audio"
												: "video";
										r = String.format(
												" <%s  src=\"%s\" controls=\"controls\" autoplay=\"autoplay\">"
														+ mContext.getString(R.string.browser_no_html5_support)
														+ " </%s>", type,
												f.getName(), type, type);

									}
									sb.append(r);
									sb.append("\n");

								}

								msg = sb.toString();

							} catch (FileNotFoundException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
							res = new Response(HTTP_OK, MIME_HTML, msg);
							return res;

						}

					}
				}

				// Calculate etag
				String etag = Integer.toHexString((f.getAbsolutePath()
						+ f.lastModified() + "" + f.length()).hashCode());

				// Support (simple) skipping:
				long startFrom = 0;
				long endAt = -1;
				String range = header.getProperty("range");
				if (range != null) {
					if (range.startsWith("bytes=")) {
						range = range.substring("bytes=".length());
						int minus = range.indexOf('-');
						try {
							if (minus > 0) {
								startFrom = Long.parseLong(range.substring(0,
										minus));
								endAt = Long.parseLong(range
										.substring(minus + 1));
							}
						} catch (NumberFormatException nfe) {
						}
					}
				}

				// Change return code and add Content-Range header when skipping
				// is requested
				long fileLen = f.length();
				if (range != null && startFrom >= 0) {
					if (startFrom >= fileLen) {
						res = new Response(HTTP_RANGE_NOT_SATISFIABLE,
								MIME_PLAINTEXT, "");
						res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
						res.addHeader("ETag", etag);
					} else {
						if (endAt < 0)
							endAt = fileLen - 1;
						long newLen = endAt - startFrom + 1;
						if (newLen < 0)
							newLen = 0;

						final long dataLen = newLen;
						FileInputStream fis = new FileInputStream(f) {
							@Override
							public int available() throws IOException {
								return (int) dataLen;
							}
						};
						fis.skip(startFrom);

						res = new Response(HTTP_PARTIALCONTENT, mime, fis);
						res.addHeader("Content-Length", "" + dataLen);
						res.addHeader("Content-Range", "bytes " + startFrom
								+ "-" + endAt + "/" + fileLen);
						res.addHeader("ETag", etag);
					}
				} else {
					if (etag.equals(header.getProperty("if-none-match")))
						res = new Response(HTTP_NOTMODIFIED, mime, "");
					else {
						res = new Response(HTTP_OK, mime,
								new FileInputStream(f));
						res.addHeader("Content-Length", "" + fileLen);
						res.addHeader("ETag", etag);
					}
				}

			} catch (IOException ioe) {
				res = new Response(HTTP_FORBIDDEN, MIME_PLAINTEXT,
						"FORBIDDEN: Reading file failed.");
			}

			res.addHeader("Accept-Ranges", "bytes");
			return res;
		}
	}





	private static int theBufferSize = 16 * 1024;

	// Change these if you want to log to somewhere else than stdout
	protected static PrintStream myOut = System.out;
	protected static PrintStream myErr = System.err;

	/**
	 * GMT date formatter
	 */
	private static java.text.SimpleDateFormat gmtFrmt;
	static {
		gmtFrmt = new java.text.SimpleDateFormat(
				"E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
		gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

}
