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
import java.util.Hashtable;
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
	private DeviceManager dm;
	private List<String> mDeviceList;

	private void mkdir(String uri, File homeDir, String dir) {
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
		dm = new DeviceManager(mContext);
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
			Toast.makeText(mContext, mContext.getResources().getString(R.string.server_port_occupied, Settings.getPort()), Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		} finally {

			Intent intent = new Intent(Intents.ACTION_SERVER_STATE_CHANGE);
			mContext.sendBroadcast(intent);

		}

	}

	public void stop() {
		try {
			myServerSocket.close();
			myThread.join();

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
				// Log.d(TAG, "pre.getProperty(\"uri\")=" + uri);

				long size = 0x7FFFFFFFFFFFFFFFl;
				String contentLength = header.getProperty("content-length");
				if (contentLength != null) {
					try {
						size = Integer.parseInt(contentLength);
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

				// Now read all the body and write it to f
				buf = new byte[bufsize];

				File homeDir = null;
				boolean webRoot = Boolean.parseBoolean(parms
						.getProperty("webroot"));

				if (webRoot) {
					homeDir = wwwroot;
				} else {
					mDeviceList = dm.getMountedDevicesList();
					homeDir = getHomeDir(uri);

				}
				Log.d(TAG, "webRoot=" + webRoot + ",homeDir=" + homeDir
						+ ",uri=" + uri);

				if (method.equalsIgnoreCase("POST")) {

					String contentType = "";
					String contentTypeHeader = header
							.getProperty("content-type");
					StringTokenizer st = new StringTokenizer(contentTypeHeader,
							"; ");
					if (st.hasMoreTokens()) {
						contentType = st.nextToken();
					}

					Log.d(TAG, "post uri=" + uri);

					if (contentType.equalsIgnoreCase("multipart/form-data")) {
						Log.d(TAG, "post file");

						if (size > 0) {
							rlen = is.read(buf, 0, bufsize);
							size -= rlen;
							if (rlen > 0) {
								f.write(buf, 0, rlen);
								Log.d(TAG, "fbuf write=" + rlen);

							}
						}

						byte[] fbuf = f.toByteArray();
						FileOutputStream out = null;
						if (fbuf.length > 0) {
							int offset = stripMultipartHeaders(fbuf, 0);
							ByteArrayInputStream bin = new ByteArrayInputStream(
									fbuf, 0, offset);
							BufferedReader in = new BufferedReader(
									new InputStreamReader(bin));
							String firstLine = in.readLine();
							Long fileSize = (size + fbuf.length - offset
									- firstLine.length() - 5);

							Log.d(TAG, "firstLine=" + firstLine);
							Log.d(TAG, "fbuf length=" + fbuf.length
									+ ",offset=" + offset);
							Log.d(TAG, "fileSize=" + String.valueOf(fileSize));
							String fileName = "";
							while (true) {
								String line = in.readLine();

								Log.d(TAG, "line=" + line);
								if (line == null) {
									break;
								}
								if (line.contains("filename=")) {
									String[] temps = line.split(";");
									for (int i = 0; i < temps.length; i++) {
										if (temps[i].trim().startsWith(
												"filename=")) {
											fileName = temps[i].trim()
													.substring(10)
													.replace("\"", "");
										}

									}
								}
							}
							Log.d(TAG, "fileName=" + fileName);
							File rootDir = new File(homeDir, uri);
							if (!rootDir.exists()) {
								rootDir.mkdirs();
							}
							File outFile = new File(rootDir, fileName);

							out = new FileOutputStream(outFile);

							out.write(fbuf, offset, fbuf.length - offset);

							size = size - (firstLine.length() + 6);
							while (size > 0) {
								rlen = is.read(buf, 0, bufsize);

								size -= rlen;
								if (rlen > 0) {
									if (size <= 0) {
										rlen -= (firstLine.length() + 6);
									}
									out.write(buf, 0, rlen);

								} else {
									break;
								}
							}

							if (out != null) {

								out.close();

							}

							if (outFile.length() != fileSize) {
								// outFile.delete();
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

						String mkdir = parms.getProperty("mkdir");

						mkdir(uri, homeDir, mkdir);

					}
				}

				Response r = serveFile(uri, method, header, homeDir);
				if (r == null) {
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
				}
			} catch (InterruptedException ie) {

			}
		}

		private File getHomeDir(String uri) {

			if (uri.equals("/") || uri.equals("")) {
				return null;
			}

			if (mDeviceList != null) {
				for (String volume : mDeviceList) {
					File file = new File(volume);
					if (uri.startsWith("/" + file.getName())) {
						return file.getParentFile();
					}
				}
			}

			return null;
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

		/**
		 * It returns the offset separating multipart file headers from the
		 * file's data.
		 **/
		private int stripMultipartHeaders(byte[] b, int offset) {
			int i = 0;
			for (i = offset; i < b.length; i++) {
				if (b[i] == '\r' && b[++i] == '\n' && b[++i] == '\r'
						&& b[++i] == '\n')
					break;
			}
			return i + 1;
		}

		/**
		 * Decodes the percent encoding scheme. <br/>
		 * For example: "an+example%20string" -> "an example string"
		 */
		private String decodePercent(String str) throws InterruptedException {
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

	public String getFileListString(List<File> files, String uri) {
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

				String extensionName = getExtensionName(file.getName());

				String mime = theMimeTypes.get(extensionName.toLowerCase());
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

			sb.append(String.format(
					"<li onclick='FileitemClick(this)' isDirectory='%b'"
							+ " fileSize='%d'" + " data-icon='%s' >"
							+ "<a href='%s' " + " data-transition='none' "
							+ "><img src='%s'"
							+ " alt='%s' class='ui-li-icon' />%s</a></li>\n",
					file.isDirectory(),
					file.isDirectory() ? -1 : file.length(), data_icon, href,
					img_src, file.getName(), file.getName()));

		}
		return sb.toString();
	}

	private List<File> getDeviceVolumes() {

		ArrayList<File> files = new ArrayList<File>();

		if (mDeviceList != null) {

			for (String s : mDeviceList) {
				files.add(new File(s));
			}
		}

		return files;
	}

	/**
	 * Serves file from homeDir and its' subdirectories (only). Uses only URI,
	 * ignores all headers and HTTP parameters.
	 */
	public Response serveFile(String uri, String method, Properties header,
			File homeDir) {
		Response res = null;

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

			File indexFile = new File(wwwroot, "index.html");

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
					files = getDeviceVolumes();
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
						sb.append(getFileListString(files, uri));
					} else {
						BufferedReader br = new BufferedReader(new FileReader(
								indexFile));
						while (true) {
							String r = br.readLine();
							if (r == null) {
								break;
							}

							if (r.trim().equalsIgnoreCase("{file_list_data}")) {
								r = getFileListString(files, uri);

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
		} else {
			try {

				// Get MIME type from file name extension, if possible
				String mime = null;

				int dot = f.getCanonicalPath().lastIndexOf('.');
				if (dot >= 0)
					mime = theMimeTypes.get(f.getCanonicalPath()
							.substring(dot + 1).toLowerCase());
				if (mime == null)
					mime = MIME_DEFAULT_BINARY;
				Log.d(TAG, "mime=" + mime);
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

	private static String getExtensionName(String filename) {
		if ((filename != null) && (filename.length() > 0)) {
			int dot = filename.lastIndexOf('.');
			if ((dot > -1) && (dot < (filename.length() - 1))) {
				return filename.substring(dot + 1);
			}
		}
		return filename;
	}

	/**
	 * Hashtable mapping (String)FILENAME_EXTENSION -> (String)MIME_TYPE
	 */
	private static Hashtable<String, String> theMimeTypes = new Hashtable<String, String>();
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
