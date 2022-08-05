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
 *     Liviu Ionescu - initial implementation.
 *******************************************************************************/

package org.eclipse.embedcdt.internal.core;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public abstract class AbstractActivator extends Plugin {

	// ------------------------------------------------------------------------

	private static AbstractActivator fgInstance;

	public static AbstractActivator getInstance() {
		return fgInstance;
	}

	// ------------------------------------------------------------------------

	public abstract String getBundleId();

	// ------------------------------------------------------------------------

	protected boolean fIsDebugging;

	// ------------------------------------------------------------------------

	public AbstractActivator() {

		super();
		fgInstance = this;

		fIsDebugging = "true".equalsIgnoreCase(Platform.getDebugOption(getBundleId() + "/debug"));
	}

	// ------------------------------------------------------------------------

	@Override
	public void start(BundleContext context) throws Exception {

		if (isDebugging()) {
			System.out.println(getBundleId() + ".start()");
		}
		super.start(context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {

		super.stop(context);
		if (isDebugging()) {
			System.out.println(getBundleId() + ".stop()");
		}
	}

	// ------------------------------------------------------------------------

	@Override
	public boolean isDebugging() {

		return fIsDebugging;
	}

	// ------------------------------------------------------------------------

	public static void log(IStatus status) {
		getInstance().getLog().log(status);
	}

	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, getInstance().getBundleId(), 1, "Internal Error", e)); //$NON-NLS-1$
	}

	public static void log(String message) {
		log(message, IStatus.ERROR); // $NON-NLS-1$
	}

	public static void log(String message, int status) {
		log(new Status(status, getInstance().getBundleId(), 1, message, null)); // $NON-NLS-1$
	}

	// ------------------------------------------------------------------------
}
