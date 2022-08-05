/*******************************************************************************
 * Copyright (c) 2014 Liviu Ionescu.
 * Copyright (c) 2015-2016 Chris Reed.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Liviu Ionescu - initial version
 *     Chris Reed - pyOCD changes
 *******************************************************************************/

package org.eclipse.embedcdt.debug.gdbjtag.pyocd.core.dsf;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.cdt.dsf.concurrent.DsfRunnable;
import org.eclipse.cdt.dsf.concurrent.IDsfStatusConstants;
import org.eclipse.cdt.dsf.concurrent.ImmediateRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.concurrent.Sequence;
import org.eclipse.cdt.dsf.gdb.service.command.GDBControl.InitializationShutdownStep;
import org.eclipse.cdt.dsf.mi.service.IMIBackend.State;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.embedcdt.core.StringUtils;
import org.eclipse.embedcdt.debug.gdbjtag.core.dsf.GnuMcuGdbServerBackend;
import org.eclipse.embedcdt.debug.gdbjtag.pyocd.core.Configuration;
import org.eclipse.embedcdt.debug.gdbjtag.pyocd.core.ConfigurationAttributes;
import org.eclipse.embedcdt.debug.gdbjtag.pyocd.core.SemihostingProcess;
import org.eclipse.embedcdt.debug.gdbjtag.pyocd.core.preferences.DefaultPreferences;
import org.eclipse.embedcdt.internal.debug.gdbjtag.pyocd.core.Activator;
import org.osgi.framework.BundleContext;

public class GdbServerBackend extends GnuMcuGdbServerBackend {

	// ------------------------------------------------------------------------

	protected boolean fDoStartSemihostingConsole = false;
	protected Process fSemihostingProcess;
	protected SemihostingMonitorJob fSemihostingMonitorJob;

	// For synchronisation reasons, set/check this only on the DSF thread.
	protected State fSemihostingBackendState = State.NOT_INITIALIZED;

	protected int fSemihostingExitValue = 0;

	protected int fGdbServerLaunchTimeout = 15;

	// ------------------------------------------------------------------------

	public GdbServerBackend(DsfSession session, ILaunchConfiguration lc) {
		super(session, lc);

		if (Activator.getInstance().isDebugging()) {
			System.out.println("pyocd.GdbServerBackend(" + session + "," + lc.getName() + ")");
		}
	}

	// ------------------------------------------------------------------------

	@Override
	public void initialize(final RequestMonitor rm) {

		if (Activator.getInstance().isDebugging()) {
			System.out.println("pyocd.GdbServerBackend.initialize()");
		}

		try {
			// Update parent data member before calling initialise.
			fDoStartGdbServer = Configuration.getDoStartGdbServer(fLaunchConfiguration);

			fDoStartSemihostingConsole = Configuration.getDoAddSemihostingConsole(fLaunchConfiguration);
		} catch (CoreException e) {
			rm.setStatus(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, "Cannot get configuration", e)); //$NON-NLS-1$
			rm.done();
			return;
		}

		// Initialise the super class, and, when ready, perform the local
		// initialisations.
		super.initialize(new RequestMonitor(getExecutor(), rm) {

			@Override
			protected void handleSuccess() {
				doInitialize(rm);
			}
		});
	}

	private void doInitialize(RequestMonitor rm) {

		if (Activator.getInstance().isDebugging()) {
			System.out.println("pyocd.GdbServerBackend.doInitialize()");
		}

		if (fDoStartGdbServer && fDoStartSemihostingConsole) {

			final Sequence.Step[] initializeSteps = new Sequence.Step[] {

					new SemihostingStep(InitializationShutdownStep.Direction.INITIALIZING),
					new SemihostingMonitorStep(InitializationShutdownStep.Direction.INITIALIZING), };

			Sequence startupSequence = new Sequence(getExecutor(), rm) {
				@Override
				public Step[] getSteps() {
					return initializeSteps;
				}
			};
			getExecutor().execute(startupSequence);

		} else {
			rm.done();
		}
	}

	@Override
	public void shutdown(final RequestMonitor rm) {

		if (Activator.getInstance().isDebugging()) {
			System.out.println("pyocd.GdbServerBackend.shutdown()");
		}

		if (fDoStartGdbServer && fDoStartSemihostingConsole) {
			final Sequence.Step[] shutdownSteps = new Sequence.Step[] {
					new SemihostingMonitorStep(InitializationShutdownStep.Direction.SHUTTING_DOWN),
					new SemihostingStep(InitializationShutdownStep.Direction.SHUTTING_DOWN), };
			Sequence startupSequence = new Sequence(getExecutor(), new ImmediateRequestMonitor(rm) {
				@Override
				protected void handleSuccess() {
					// We're done here, shutdown parent.
					GdbServerBackend.super.shutdown(rm);
				}
			}) {
				@Override
				public Step[] getSteps() {
					return shutdownSteps;
				}
			};
			getExecutor().execute(startupSequence);

		} else {
			super.shutdown(rm);
		}
	}

	@Override
	public void destroy() {

		if (Activator.getInstance().isDebugging()) {
			System.out.println("pyocd.GdbServerBackend.destroy() " + Thread.currentThread());
		}

		// Destroy the semihosting process
		if (fSemihostingProcess != null && fSemihostingBackendState == State.STARTED) {
			fSemihostingProcess.destroy();
		}

		// Destroy the parent (the GDB server; the client is also destroyed
		// there).
		super.destroy();
	}

	// ------------------------------------------------------------------------

	@Override
	protected BundleContext getBundleContext() {
		return Activator.getInstance().getBundle().getBundleContext();
	}

	@Override
	public String[] getServerCommandLineArray() {
		String[] commandLineArray = Configuration.getGdbServerCommandLineArray(fLaunchConfiguration);

		return commandLineArray;
	}

	@Override
	public String getServerCommandName() {

		String[] commandLineArray = getServerCommandLineArray();
		if (commandLineArray == null) {
			return null;
		}

		String fullCommand = commandLineArray[0];
		return StringUtils.extractNameFromPath(fullCommand);
	}

	@Override
	public int getServerLaunchTimeoutSeconds() {
		return fGdbServerLaunchTimeout;
	}

	@Override
	public String getServerName() {
		return "pyOCD GDB Server";
	}

	public String getSemihostingName() {
		return "pyOCD GDB";
	}

	public String getStartingSemihostingJobName() {
		return "Starting " + getSemihostingName() + " Semihosting Process";
	}

	public String getTerminatingSemihostingJobName() {
		return "Terminating " + getSemihostingName() + " Semihosting Process";
	}

	@Override
	public boolean canMatchStdOut() {
		return false;
	}

	@Override
	public boolean canMatchStdErr() {
		return true;
	}

	@Override
	public boolean matchStdOutExpectedPattern(String line) {
		String message = Configuration.getGdbServerStartedMessage();
		return (line.indexOf(message) >= 0);
	}

	@Override
	public boolean matchStdErrExpectedPattern(String line) {
		String message = Configuration.getGdbServerStartedMessage();
		return (line.indexOf(message) >= 0);
	}

	/**
	 * Since the stderr messages are not final, this function makes the best use
	 * of the available information (the exit code and the captured string) to
	 * compose the text displayed in case of error.
	 *
	 * @param exitCode
	 *            an integer with the process exit code.
	 * @param message
	 *            a string with the captured stderr text.
	 * @return a string with the text to be displayed.
	 */
	@Override
	public String prepareMessageBoxText(int exitCode) {

		String body = "";

		String name = getServerCommandName();
		if (name == null) {
			name = "GDB Server";
		}
		String tail = "\n\nFor more details, see the " + name + " console.";

		if (body.isEmpty()) {
			return getServerName() + " failed with code (" + exitCode + ")." + tail;
		} else {
			return getServerName() + " failed: \n" + body + tail;
		}
	}

	// ------------------------------------------------------------------------

	public Process getSemihostingProcess() {
		return fSemihostingProcess;
	}

	// ========================================================================

	// Start/Stop the pyOCD semihosting console
	protected class SemihostingStep extends InitializationShutdownStep {

		SemihostingStep(Direction direction) {
			super(direction);
		}

		@Override
		public void initialize(final RequestMonitor rm) {

			if (Activator.getInstance().isDebugging()) {
				System.out.println("pyocd.SemihostingStep.initialise()");
			}

			if (fServerBackendState != State.STARTED) {
				if (Activator.getInstance().isDebugging()) {
					System.out.println("pyocd.SemihostingStep.initialise() skipped");
				}
				// rm.cancel();
				rm.done();
				return;
			}

			class SemihostingLaunchMonitor {
				boolean fLaunched = false;
				boolean fTimedOut = false;
			}

			final SemihostingLaunchMonitor fSemihostingLaunchMonitor = new SemihostingLaunchMonitor();

			final RequestMonitor fTmpLaunchRequestMonitor = new RequestMonitor(getExecutor(), rm) {

				@Override
				protected void handleCompleted() {
					if (Activator.getInstance().isDebugging()) {
						System.out.println("pyocd.SemihostingStep.initialise() handleCompleted()");
					}
					if (!fSemihostingLaunchMonitor.fTimedOut) {
						fSemihostingLaunchMonitor.fLaunched = true;
						if (!isSuccess()) {
							rm.setStatus(getStatus());
						}
						rm.done();
					}
				}
			};

			final Job startSemihostingJob = new Job(getStartingSemihostingJobName()) {
				{
					setSystem(true);
				}

				@Override
				protected IStatus run(IProgressMonitor monitor) {

					if (fTmpLaunchRequestMonitor.isCanceled()) {

						if (Activator.getInstance().isDebugging()) {
							System.out.println("pyocd.SemihostingStep.initialise() startSemihostingJob run cancel");
						}
						fTmpLaunchRequestMonitor.setStatus(new Status(IStatus.CANCEL, Activator.PLUGIN_ID, -1,
								getStartingSemihostingJobName() + " cancelled.", null)); //$NON-NLS-1$
						fTmpLaunchRequestMonitor.done();
						return Status.OK_STATUS;
					}

					try {
						String host = "localhost";

						int port = fLaunchConfiguration.getAttribute(
								ConfigurationAttributes.GDB_SERVER_TELNET_PORT_NUMBER,
								DefaultPreferences.GDB_SERVER_TELNET_PORT_NUMBER_DEFAULT);

						fSemihostingProcess = launchSemihostingProcess(host, port);

						// Need to do this on the executor for thread-safety
						getExecutor().submit(new DsfRunnable() {
							@Override
							public void run() {
								if (Activator.getInstance().isDebugging()) {
									System.out.println(
											"pyocd.SemihostingStep.initialise() startSemihostingJob run State.STARTED");
								}
								fSemihostingBackendState = State.STARTED;
							}
						});
					} catch (CoreException e) {
						fTmpLaunchRequestMonitor
								.setStatus(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, e.getMessage(), e));
						fTmpLaunchRequestMonitor.done();
						return Status.OK_STATUS;
					}

					// TODO: check if the process started properly
					// (parse input and check greeting).

					fTmpLaunchRequestMonitor.done();
					if (Activator.getInstance().isDebugging()) {
						System.out.println("pyocd.SemihostingStep.initialise() startSemihostingJob run completed");
					}
					return Status.OK_STATUS;
				}
			};
			startSemihostingJob.schedule();

			if (Activator.getInstance().isDebugging()) {
				System.out.println("pyocd.SemihostingStep.initialise() after job schedule");
			}

			getExecutor().schedule(new Runnable() {

				@Override
				public void run() {

					// Only process the event if we have not finished yet (hit
					// the breakpoint).
					if (!fSemihostingLaunchMonitor.fLaunched) {
						fSemihostingLaunchMonitor.fTimedOut = true;
						Thread jobThread = startSemihostingJob.getThread();
						if (jobThread != null) {
							if (Activator.getInstance().isDebugging()) {
								System.out.println("pyocd.SemihostingStep.initialise() interrupt thread " + jobThread);
							}

							jobThread.interrupt();
						}
						rm.setStatus(
								new Status(IStatus.ERROR, Activator.PLUGIN_ID, DebugException.TARGET_REQUEST_FAILED,
										getStartingSemihostingJobName() + " timed out.", null)); //$NON-NLS-1$
						rm.done();
					}
				}
			}, getServerLaunchTimeoutSeconds(), TimeUnit.SECONDS);

			if (Activator.getInstance().isDebugging()) {
				System.out.println("pyocd.SemihostingStep.initialise() return");
			}
		}

		@Override
		protected void shutdown(final RequestMonitor requestMonitor) {

			if (Activator.getInstance().isDebugging()) {
				System.out.println("pyocd.SemihostingStep.shutdown()");
			}

			if (fSemihostingBackendState != State.STARTED) {
				// Not started yet or already killed, don't bother starting
				// a job to kill it
				requestMonitor.done();
				return;
			}

			new Job(getTerminatingSemihostingJobName()) {
				{
					setSystem(true);
				}

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						// Need to do this on the executor for thread-safety
						// And we should wait for it to complete since we then
						// check if the killing of GDB worked.
						if (Activator.getInstance().isDebugging()) {
							System.out.println("pyocd.SemihostingStep.shutdown() run()");
						}
						getExecutor().submit(new DsfRunnable() {
							@Override
							public void run() {
								if (Activator.getInstance().isDebugging()) {
									System.out.println("pyocd.SemihostingStep.shutdown() run() run()");
								}
								destroy();

								if (fSemihostingMonitorJob.fMonitorExited) {
									// Now that we have destroyed the process,
									// and that the monitoring thread was
									// killed,
									// we need to set our state and send the
									// event
									if (Activator.getInstance().isDebugging()) {
										System.out.println(
												"pyocd.SemihostingStep.shutdown() run() run() State.TERMINATED");
									}
									fSemihostingBackendState = State.TERMINATED;

									// If necessary, send an event like
									// BackendStateChangedEvent(getSession().getId(),
									// getId(), State.TERMINATED),
									// getProperties()
								}
							}
						}).get();
					} catch (InterruptedException e1) {
					} catch (ExecutionException e1) {
					}

					if (Activator.getInstance().isDebugging()) {
						System.out.println("pyocd.SemihostingStep.shutdown() run() before getting exitValue");
					}
					int attempts = 0;
					while (attempts < 10) {
						try {
							// Don't know if we really need the exit value...
							// but what the heck.
							// throws exception if process not exited
							fSemihostingExitValue = fSemihostingProcess.exitValue();

							if (Activator.getInstance().isDebugging()) {
								System.out.println("pyocd.SemihostingStep.shutdown() run() return");
							}
							requestMonitor.done();
							return Status.OK_STATUS;
						} catch (IllegalThreadStateException ie) {
						}
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
						}
						attempts++;
					}
					if (Activator.getInstance().isDebugging()) {
						System.out.println("pyocd.SemihostingStep.shutdown() run() REQUEST_FAILED");
					}
					requestMonitor.setStatus(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
							IDsfStatusConstants.REQUEST_FAILED, "GDB semihosting terminate failed", null)); //$NON-NLS-1$
					requestMonitor.done();
					return Status.OK_STATUS;
				}
			}.schedule();
			if (Activator.getInstance().isDebugging()) {
				System.out.println("pyocd.SemihostingStep.shutdown() return");
			}
		}

		protected Process launchSemihostingProcess(String host, int port) throws CoreException {

			SemihostingProcess proc = new SemihostingProcess(host, port);

			// proc.submit(getExecutor());
			proc.submit();

			if (Activator.getInstance().isDebugging()) {
				System.out.println("pyocd.SemihostingStep.launchSemihostingProcess() return " + proc);
			}
			return proc;
		}

	}

	// ========================================================================

	/**
	 * Monitors the semihosting process, waiting for it to terminate, and then
	 * notifies the associated runtime process.
	 */
	private class SemihostingMonitorJob extends Job {

		boolean fMonitorExited = false;
		DsfRunnable fMonitorStarted;
		Process fProcess;

		SemihostingMonitorJob(Process process, DsfRunnable monitorStarted) {
			super("Semihosting process monitor job."); //$NON-NLS-1$
			fProcess = process;
			fMonitorStarted = monitorStarted;
			setSystem(true);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			synchronized (fProcess) {
				if (Activator.getInstance().isDebugging()) {
					System.out.println(
							"pyocd.SemihostingMonitorJob.run() submit " + fMonitorStarted + " thread " + getThread());
				}
				getExecutor().submit(fMonitorStarted);
				try {
					fProcess.waitFor();
					fSemihostingExitValue = fProcess.exitValue();

					// Need to do this on the executor for thread-safety
					getExecutor().submit(new DsfRunnable() {
						@Override
						public void run() {
							if (Activator.getInstance().isDebugging()) {
								System.out.println("pyocd.SemihostingMonitorJob.run() run() thread " + getThread());
							}

							// Destroy the entire backend
							destroy();
							if (Activator.getInstance().isDebugging()) {
								System.out.println("pyocd.SemihostingMonitorJob.run() run() State.TERMINATED");
							}
							fSemihostingBackendState = State.TERMINATED;

							// If necessary, send an event like
							// BackendStateChangedEvent(getSession().getId(),
							// getId(), State.TERMINATED),
							// getProperties()
						}
					});
				} catch (InterruptedException ie) {
					// clear interrupted state
					Thread.interrupted();
				}

				if (Activator.getInstance().isDebugging()) {
					System.out.println("pyocd.SemihostingMonitorJob.run() fMonitorExited = true thread " + getThread());
				}
				fMonitorExited = true;
			}
			return Status.OK_STATUS;
		}

		void kill() {
			synchronized (fProcess) {
				if (!fMonitorExited) {
					Thread thread = getThread();
					if (thread != null) {
						if (Activator.getInstance().isDebugging()) {
							System.out.println("pyocd.SemihostingMonitorJob.kill() interrupt " + thread.toString());
						}
						thread.interrupt();
					} else {
						Activator.log("pyocd.SemihostingMonitorJob.kill() null thread");
					}
				}
			}
		}
	}

	// ========================================================================

	/**
	 * Start/stop the SemihostingMonitorJob.
	 */
	protected class SemihostingMonitorStep extends InitializationShutdownStep {

		SemihostingMonitorStep(Direction direction) {
			super(direction);
		}

		@Override
		public void initialize(final RequestMonitor rm) {

			if (Activator.getInstance().isDebugging()) {
				System.out.println("pyocd.SemihostingMonitorStep.initialize()");
			}

			if (fServerBackendState != State.STARTED) {
				if (Activator.getInstance().isDebugging()) {
					System.out.println("pyocd.SemihostingMonitorStep.initialise() skipped");
				}
				// rm.cancel();
				rm.done();
				return;
			}

			fSemihostingMonitorJob = new SemihostingMonitorJob(fSemihostingProcess, new DsfRunnable() {
				@Override
				public void run() {
					rm.done();
				}
			});
			fSemihostingMonitorJob.schedule();
		}

		@Override
		protected void shutdown(RequestMonitor requestMonitor) {

			if (Activator.getInstance().isDebugging()) {
				System.out.println("pyocd.SemihostingMonitorStep.shutdown()");
			}
			if (fSemihostingMonitorJob != null) {
				fSemihostingMonitorJob.kill();
			}
			requestMonitor.done();
			if (Activator.getInstance().isDebugging()) {
				System.out.println("pyocd.SemihostingMonitorStep.shutdown() return");
			}
		}
	}

	// ------------------------------------------------------------------------
}
