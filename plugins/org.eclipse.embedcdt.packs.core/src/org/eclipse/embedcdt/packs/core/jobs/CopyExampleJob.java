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
 *******************************************************************************/

package org.eclipse.embedcdt.packs.core.jobs;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.embedcdt.core.StringUtils;
import org.eclipse.embedcdt.internal.packs.core.Activator;
import org.eclipse.embedcdt.packs.core.IConsoleStream;
import org.eclipse.embedcdt.packs.core.data.DataUtils;
import org.eclipse.embedcdt.packs.core.data.PacksStorage;
import org.eclipse.embedcdt.packs.core.tree.Leaf;
import org.eclipse.embedcdt.packs.core.tree.Node;
import org.eclipse.embedcdt.packs.core.tree.PackNode;
import org.eclipse.embedcdt.packs.core.tree.Property;
import org.eclipse.embedcdt.packs.core.tree.Type;

public class CopyExampleJob extends Job {

	private static boolean fgRunning = false;

	private String fParam[];
	private IConsoleStream fOut;
	private List<PackNode> fSelection;
	private IPath fDestFolderPath;

	private IProgressMonitor fMonitor;

	private int fSizeOfPrefixToStrip;

	public CopyExampleJob(String name, List<PackNode> selection, String[] param) {

		super(name);

		fOut = Activator.getInstance().getConsoleOutput();

		fSelection = selection;
		fParam = param;

		fSizeOfPrefixToStrip = 0;

		fDestFolderPath = new Path(fParam[0]);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {

		if (fgRunning) {
			return Status.CANCEL_STATUS;
		}

		fgRunning = true;
		fMonitor = monitor;

		long beginTime = System.currentTimeMillis();

		fOut.println();
		fOut.println(org.eclipse.embedcdt.packs.core.Utils.getCurrentDateTime());

		int exampleCount = 0;

		for (PackNode exampleNode : fSelection) {

			if (monitor.isCanceled()) {
				break;
			}

			PackNode versionNode = (PackNode) exampleNode.getParent();

			Leaf outlineExampleNode = exampleNode.getOutline().findChild(Type.EXAMPLE);

			String packRelativeFolder = versionNode.getProperty(Property.DEST_FOLDER);
			String exampleRelativeFolder = outlineExampleNode.getProperty(Node.FOLDER_PROPERTY);

			fOut.println(
					"Copying example folder \"" + new Path(packRelativeFolder).append(exampleRelativeFolder) + "\"...");

			File destFolder = fDestFolderPath.append(exampleRelativeFolder).toFile();

			if (!destFolder.exists()) {
				if (!destFolder.mkdirs()) {
					fOut.println("Cannot create destination folder \"" + destFolder.toString() + "\".");
					return Status.CANCEL_STATUS;
				}
			}
			if (!destFolder.isDirectory()) {
				fOut.println("Destination \"" + destFolder.toString() + "\" is not a folder.");
				return Status.CANCEL_STATUS;
			}

			if (destFolder.listFiles().length > 0) {
				fOut.println("Destination \"" + destFolder.toString() + "\" is not an empty folder.");
				fOut.println("Deleting previous content...");
				int count = DataUtils.deleteFolderRecursive(destFolder);
				fOut.println(count + " files deleted.");
			}

			// Compute source folder

			IPath srcFolderPath;
			try {

				srcFolderPath = PacksStorage.getFolderPath();
				srcFolderPath = srcFolderPath.append(packRelativeFolder);
				fSizeOfPrefixToStrip = srcFolderPath.toString().length();
				if (!srcFolderPath.hasTrailingSeparator()) {
					fSizeOfPrefixToStrip++;
				}
				srcFolderPath = srcFolderPath.append(exampleRelativeFolder);

				int totalWork = (int) getFilesSizeRecursive(srcFolderPath.toFile());
				monitor.beginTask("Copy folder", totalWork);

				int count = 0;
				count = copyFolderRecursive(srcFolderPath.toFile());

				fOut.print("Example \"" + exampleNode.getName() + "\", ");
				fOut.print(StringUtils.convertSizeToString(totalWork) + " in ");
				if (count <= 1) {
					fOut.print("1 file");
				} else {
					fOut.print(count + " files");
				}
				fOut.println(" copied.");

				exampleCount++;

			} catch (IOException e) {
				fOut.print("Error: " + e.toString());
			}
		}

		IStatus status;

		if (monitor.isCanceled()) {

			fOut.println("Job cancelled.");
			status = Status.CANCEL_STATUS;

		} else {

			long endTime = System.currentTimeMillis();
			long duration = endTime - beginTime;
			if (duration == 0) {
				duration = 1;
			}

			fOut.print("Copy completed in ");
			if (duration < 1000) {
				fOut.print(duration + "ms");
			} else {
				fOut.print((duration + 500) / 1000 + "s");
			}
			if (exampleCount <= 1) {
				fOut.print(" (1 example).");
			} else {
				fOut.print(" (" + exampleCount + " examples).");
			}
			fOut.println();

			status = Status.OK_STATUS;
		}

		fgRunning = false;
		return status;
	}

	private long getFilesSizeRecursive(File folder) {

		File files[] = folder.listFiles();
		long countBytes = 0;
		for (File file : files) {
			if (file.isDirectory()) {
				countBytes += getFilesSizeRecursive(file);
			} else if (file.isFile()) {
				countBytes += file.length();
			} else {
				Activator.log("File " + file + " unaccounted.");
			}
		}

		return countBytes;
	}

	private int copyFolderRecursive(File folder) {

		File files[] = folder.listFiles();
		int countFiles = 0;
		for (File file : files) {

			if (fMonitor.isCanceled()) {
				break;
			}

			if (file.isDirectory()) {
				countFiles += copyFolderRecursive(file);
			} else if (file.isFile()) {

				try {
					copyFile(file);
				} catch (IOException e) {
					fOut.print("Error: " + e.toString());
				}
				countFiles += 1;
			}
		}

		return countFiles;
	}

	private void copyFile(File file) throws IOException {

		String path = file.getPath();
		String suffix = path.substring(fSizeOfPrefixToStrip);
		IPath destPath = fDestFolderPath.append(suffix);

		File destFile = destPath.toFile();

		DataUtils.copyFile(file, destFile, fOut, fMonitor);
	}
}
