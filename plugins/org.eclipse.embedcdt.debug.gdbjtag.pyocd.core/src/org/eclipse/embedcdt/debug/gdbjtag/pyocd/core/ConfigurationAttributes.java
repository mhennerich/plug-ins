/*******************************************************************************
 * Copyright (c) 2013 Liviu Ionescu.
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

package org.eclipse.embedcdt.debug.gdbjtag.pyocd.core;

public interface ConfigurationAttributes extends org.eclipse.embedcdt.debug.gdbjtag.core.ConfigurationAttributes {

	// ------------------------------------------------------------------------

	// public static final String PREFIX = Activator.PLUGIN_ID;
	public static final String PREFIX = "ilg.gnumcueclipse.debug.gdbjtag.pyocd";

	// ------------------------------------------------------------------------

	// TabDebugger

	// Must be in sync with plugin.xml definition
	public static final String JTAG_DEVICE = "GNU MCU PyOCD";

	public static final String DO_START_GDB_SERVER = PREFIX + ".doStartGdbServer"; //$NON-NLS-1$

	public static final String GDB_SERVER_EXECUTABLE = PREFIX + ".gdbServerExecutable"; //$NON-NLS-1$

	public static final String GDB_SERVER_CONNECTION_ADDRESS = PREFIX + ".gdbServerConnectionAddress"; //$NON-NLS-1$

	public static final String GDB_SERVER_GDB_PORT_NUMBER = PREFIX + ".gdbServerGdbPortNumber"; //$NON-NLS-1$

	public static final String GDB_SERVER_TELNET_PORT_NUMBER = PREFIX + ".gdbServerTelnetPortNumber"; //$NON-NLS-1$

	/**
	 * @since 3.0
	 */
	public static final String GDB_SERVER_PROBE_ID = PREFIX + ".gdbServerBoardId"; //$NON-NLS-1$

	public static final String GDB_SERVER_BOARD_NAME = PREFIX + ".gdbServerBoardName"; //$NON-NLS-1$

	public static final String GDB_SERVER_BUS_SPEED = PREFIX + ".gdbServerBusSpeed"; //$NON-NLS-1$

	public static final String GDB_SERVER_OVERRIDE_TARGET = PREFIX + ".gdbServerOverrideTarget"; //$NON-NLS-1$

	public static final String GDB_SERVER_TARGET_NAME = PREFIX + ".gdbServerTargetName"; //$NON-NLS-1$

	/**
	 * @since 3.0
	 */
	public static final String GDB_SERVER_CONNECT_MODE = PREFIX + ".gdbServerConnectMode"; //$NON-NLS-1$

	/**
	 * @since 3.0
	 */
	public static final String GDB_SERVER_RESET_TYPE = PREFIX + ".gdbServerResetType"; //$NON-NLS-1$

	public static final String GDB_SERVER_HALT_AT_HARD_FAULT = PREFIX + ".gdbServerHaltAtHardFault"; //$NON-NLS-1$

	public static final String GDB_SERVER_STEP_INTO_INTERRUPTS = PREFIX + ".gdbServerStepIntoInterrutps"; //$NON-NLS-1$

	/**
	 * @since 3.0
	 */
	public static final String GDB_SERVER_FLASH_MODE = PREFIX + ".gdbServerFlashMode"; //$NON-NLS-1$

	/**
	 * @since 3.0
	 */
	public static final String GDB_SERVER_SMART_FLASH = PREFIX + ".gdbServerSmartFlash"; //$NON-NLS-1$

	public static final String GDB_SERVER_ENABLE_SEMIHOSTING = PREFIX + ".gdbServerEnableSemihosting"; //$NON-NLS-1$

	public static final String GDB_SERVER_USE_GDB_SYSCALLS = PREFIX + ".gdbServerUseGdbSyscalls"; //$NON-NLS-1$

	public static final String GDB_SERVER_LOG = PREFIX + ".gdbServerLog"; //$NON-NLS-1$

	public static final String GDB_SERVER_OTHER = PREFIX + ".gdbServerOther"; //$NON-NLS-1$

	public static final String DO_GDB_SERVER_ALLOCATE_CONSOLE = PREFIX + ".doGdbServerAllocateConsole"; //$NON-NLS-1$

	public static final String DO_GDB_SERVER_ALLOCATE_SEMIHOSTING_CONSOLE = PREFIX
			+ ".doGdbServerAllocateSemihostingConsole"; //$NON-NLS-1$

	public static final String GDB_CLIENT_OTHER_OPTIONS = PREFIX + ".gdbClientOtherOptions"; //$NON-NLS-1$

	public static final String GDB_CLIENT_OTHER_COMMANDS = PREFIX + ".gdbClientOtherCommands"; //$NON-NLS-1$

	// ------------------------------------------------------------------------

	// TabStartup
	public static final String OTHER_INIT_COMMANDS = PREFIX + ".otherInitCommands"; //$NON-NLS-1$

	public static final String DO_DEBUG_IN_RAM = PREFIX + ".doDebugInRam"; //$NON-NLS-1$

	public static final String DO_SECOND_RESET = PREFIX + ".doSecondReset"; //$NON-NLS-1$

	public static final String SECOND_RESET_TYPE = PREFIX + ".secondResetType"; //$NON-NLS-1$

	public static final String OTHER_RUN_COMMANDS = PREFIX + ".otherRunCommands"; //$NON-NLS-1$

	public static final String DO_CONTINUE = PREFIX + ".doContinue"; //$NON-NLS-1$

	// ------------------------------------------------------------------------
}
