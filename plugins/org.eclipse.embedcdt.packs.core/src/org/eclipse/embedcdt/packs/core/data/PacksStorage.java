/*******************************************************************************
 * Copyright (c) 2014, 2020 Liviu Ionescu and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Liviu Ionescu - initial implementation.
 *     Alexander Fedorov (ArSysOp) - UI part extraction.
 *     Liviu Ionescu - UI part extraction.
 *******************************************************************************/

package org.eclipse.embedcdt.packs.core.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.embedcdt.internal.packs.core.Activator;
import org.eclipse.embedcdt.packs.core.IConsoleStream;
import org.eclipse.embedcdt.packs.core.Preferences;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class PacksStorage {

	public static final String CACHE_FOLDER = ".cache";

	public static final String CONTENT_FILE_NAME_PREFIX = ".content_";
	public static final String CONTENT_FILE_NAME_SUFFIX = ".xml";
	public static final String CONTENT_XML_VERSION = "1.1";

	public static final String INSTALLED_DEVICES_FILE_NAME = ".installed_devices_boards_books.xml";
	/**
	 * @since 3.1
	 */
	public static final String INSTALLED_DEVICES_XML_VERSION = "1.2";

	private static IPath fgFolderPath = null;

	private final static int TIME_OUT = 60 * 000;

	// ------------------------------------------------------------------------

	// Return a file object in Packages
	public static File getFileObject(String name) throws IOException {

		IPath path = getFolderPath().append(name);
		File file = path.toFile();
		if (file == null) {
			throw new IOException(name + " File object null.");
		}
		return file; // Cannot return null
	}

	// Return a file object in Packages/.cache
	public static File getCachedFileObject(String name) throws IOException {

		IPath path = getFolderPath().append(CACHE_FOLDER).append(name);
		File file = path.toFile();
		if (file == null) {
			throw new IOException(name + " File object null.");
		}
		return file; // Cannot return null
	}

	public static File getPackageFileObject(String vendor, String packageName, String version, String name)
			throws IOException {

		IPath path = getFolderPath().append(vendor).append(packageName).append(version).append(name);
		File file = path.toFile();
		if (file == null) {
			throw new IOException(name + " File object null.");
		}
		return file; // Cannot return null
	}

	// Return the absolute 'Packages' path.
	public static synchronized IPath getFolderPath() throws IOException {

		if (fgFolderPath == null) {

			fgFolderPath = new Path(getFolderPathString());
		}

		return fgFolderPath;
	}

	// Return a string with the absolute full path of the folder used
	// to store packages
	public static String getFolderPathString() throws IOException {

		String folderPath = Platform.getPreferencesService().getString(Activator.PLUGIN_ID,
				Preferences.PACKS_CMSIS_FOLDER_PATH, null, null);
		if (folderPath == null) {
			throw new IOException("Missing folder path.");
		}
		folderPath = folderPath.trim();

		// Remove the terminating separator
		if (folderPath.endsWith(String.valueOf(IPath.SEPARATOR))) {
			folderPath = folderPath.substring(0, folderPath.length() - 1);
		}

		if (folderPath.isEmpty()) {
			throw new IOException("Missing folder path.");
		}
		return folderPath;
	}

	public static String makeCachedPdscName(String pdscName, String version) {

		String s;

		s = pdscName;
		int ix = s.lastIndexOf('.');
		if (ix > 0) {
			// Insert .version before extension
			s = s.substring(0, ix) + "." + version + s.substring(ix);
		}
		return s;
	}

	// ------------------------------------------------------------------------

	public static long getPackSize(String packName, URL url, IConsoleStream out) throws IOException {
		// Check if the .pack file is present (i.e. it was installed).
		File f = getCachedFileObject(packName);
		if (f.isFile()) {
			// If present, the size is computed from the existing file.
			long sz = f.length();
			out.println("\"" + packName + "\" already installed, size is " + String.valueOf(sz) + " bytes.");
			return sz;
		}
		// Check if there is cached .pack.size file.
		String cachedName = "." + packName + ".size";
		f = getCachedFileObject(cachedName);
		if (f.isFile()) {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String str = br.readLine();
			br.close();

			// String str = new String(data);
			if (str != null && !str.isEmpty()) {
				try {
					long sz = Long.valueOf(str);
					out.println(
							"Size of \"" + packName + "\" retrieved from cache is " + String.valueOf(sz) + " bytes.");
					return sz;
				} catch (NumberFormatException e) {
					// Fall through to get actual file size.
				}
			}
		}

		// Finally do a HTTP GET
		long sz = getRemoteFileSize(packName, url, out);

		if (sz >= 0) {
			// Cache the size as the ASCII content of the .pack.size file.
			FileWriter fr = new FileWriter(f);
			fr.write(String.valueOf(sz));
			fr.close();

			out.println("Value " + String.valueOf(sz) + " cached into \"" + f.getCanonicalPath() + "\".");
		}
		return sz;
	}

	public static long getRemoteFileSize(String packName, URL url, IConsoleStream out) throws IOException {

		URLConnection connection;
		while (true) {
			out.println("Getting size of \"" + url + "\"...");
			connection = url.openConnection();
			if (connection instanceof HttpURLConnection) {
				connection.setConnectTimeout(TIME_OUT);
				connection.setReadTimeout(TIME_OUT);
				HttpURLConnection httpURLConnection = (HttpURLConnection) connection;

				int responseCode = httpURLConnection.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK) {
					break;
				} else if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP
						|| responseCode == HttpURLConnection.HTTP_MOVED_PERM
						|| responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
					String newUrl = connection.getHeaderField("Location");
					url = new URL(newUrl);
					continue;
					// System.out.println("Redirect to URL : " + newUrl);
				} else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
					httpURLConnection.disconnect();
					throw new FileNotFoundException("File \"" + url + "\" not found (" + responseCode + ").");
				} else {
					httpURLConnection.disconnect();
					throw new FileNotFoundException("Failed to open connection, response code " + responseCode);
				}

			}
			break; // When non http protocol, for example.
		}

		long length = connection.getContentLength();
		if (length < 0) {
			// conn.getContentLength() returns -1 when the file is sent in
			// chunks, so it cannot be used; instead it is computed.
			InputStream input = connection.getInputStream();

			OutputStream output = null;
			if (packName != null && !packName.isEmpty()) {
				File f = getCachedFileObject(packName);
				output = new FileOutputStream(f);
				out.println("Writing \"" + f.getCanonicalPath() + "\"...");
			}

			int totalBytes = 0;
			byte[] buf = new byte[1024];
			int bytesRead;

			while ((bytesRead = input.read(buf)) > 0) {
				totalBytes += bytesRead;
				if (output != null) {
					output.write(buf, 0, bytesRead);
				}
			}
			input.close();
			if (output != null) {
				output.close();
			}

			length = totalBytes;
		}

		if (connection instanceof HttpURLConnection) {
			((HttpURLConnection) connection).disconnect();
		}

		return length;
	}

	public static int getRemoteFileSize(URL url, IConsoleStream out) throws IOException {
		return (int) getRemoteFileSize(null, url, out);
	}

	// ------------------------------------------------------------------------
}
