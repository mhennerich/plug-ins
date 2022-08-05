/*******************************************************************************
 * Copyright (c) 2014 Liviu Ionescu.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Liviu Ionescu - initial version
 *******************************************************************************/

package org.eclipse.embedcdt.templates.core.processes;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.eclipse.cdt.core.templateengine.TemplateCore;
import org.eclipse.cdt.core.templateengine.process.ProcessArgument;
import org.eclipse.cdt.core.templateengine.process.ProcessFailureException;
import org.eclipse.cdt.core.templateengine.process.ProcessRunner;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.embedcdt.internal.templates.core.Activator;
import org.osgi.framework.Bundle;

public class SetPropertyToPluginResource extends ProcessRunner {

	@Override
	public void process(TemplateCore template, ProcessArgument[] args, String processId, IProgressMonitor monitor)
			throws ProcessFailureException {

		String bundleId = args[0].getSimpleValue();
		String relativePath = args[1].getSimpleValue();
		String propertyName = args[2].getSimpleValue();

		Bundle bundle = Platform.getBundle(bundleId);
		URL url = FileLocator.find(bundle, new Path(relativePath), null);
		if (url == null) {
			throw new ProcessFailureException(getProcessMessage(processId, IStatus.ERROR,
					"Bundle resource not found " + bundle + " " + relativePath)); //$NON-NLS-1$
		}
		String location;
		try {
			location = FileLocator.resolve(url).getPath();

			Map<String, String> values = template.getValueStore();
			if (values.containsKey(propertyName)) {
				values.put(propertyName, location);
			} else {
				Activator.log("Property " + propertyName + " not defined.");
			}
		} catch (IOException e) {
			throw new ProcessFailureException(getProcessMessage(processId, IStatus.ERROR, "Cannot resolve url " + url)); //$NON-NLS-1$
		}

	}

}
