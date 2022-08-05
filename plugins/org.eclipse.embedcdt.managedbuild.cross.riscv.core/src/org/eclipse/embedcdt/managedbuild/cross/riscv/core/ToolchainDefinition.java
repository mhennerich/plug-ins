/*******************************************************************************
 * Copyright (c) 2013 Liviu Ionescu.
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
 *******************************************************************************/

package org.eclipse.embedcdt.managedbuild.cross.riscv.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.embedcdt.internal.managedbuild.cross.riscv.core.Activator;

public class ToolchainDefinition extends org.eclipse.embedcdt.managedbuild.cross.core.ToolchainDefinition {

	// ------------------------------------------------------------------------

	// Custom ID: 2273142912+1, since the name is duplicate.
	public static final String XPACK_RISCV_GCC_2 = "xPack GNU RISC-V Embedded GCC";
	public static final String XPACK_RISCV_GCC = "xPack GNU RISC-V Embedded GCC";
	public static final String GME_RISCV_GCC = "GNU MCU RISC-V GCC";

	public static final String RISC_V_GCC_NEWLIB = "RISC-V GCC/Newlib";
	public static final String RISC_V_GCC_LINUX = "RISC-V GCC/Linux";
	public static final String RISC_V_GCC_RTEMS = "RISC-V GCC/RTEMS";

	public static final String DEFAULT_TOOLCHAIN_NAME = XPACK_RISCV_GCC_2;

	// ------------------------------------------------------------------------

	// Static members
	protected static List<ToolchainDefinition> fgList = new ArrayList<>();
	protected static String fgArchitectures[] = { "RISC-V" };

	// ------------------------------------------------------------------------

	public ToolchainDefinition(String sName) {
		super(sName);
		fArchitecture = "risc-v";
	}

	public ToolchainDefinition(String sName, String sPrefix) {
		this(sName);
		fPrefix = sPrefix;
	}

	public ToolchainDefinition(String sName, String sPrefix, String sArchitecture) {
		this(sName, sPrefix);
		fArchitecture = sArchitecture;
	}

	public ToolchainDefinition(String sName, String sPrefix, String sArchitecture, String cmdMake, String cmdRm) {
		this(sName, sPrefix, sArchitecture);
		fCmdMake = cmdMake;
		fCmdRm = cmdRm;
	}

	// ------------------------------------------------------------------------

	@Override
	public void setArchitecture(String architecture) {
		assert (architecture.equals("risc-v"));
		super.setArchitecture(architecture);
	}

	// ------------------------------------------------------------------------

	public static List<ToolchainDefinition> getList() {
		return fgList;
	}

	public static ToolchainDefinition getToolchain(int index) {
		return fgList.get(index);
	}

	public static ToolchainDefinition getToolchain(String index) {
		return fgList.get(Integer.parseInt(index));
	}

	public static int getSize() {
		return fgList.size();
	}

	public static void addToolchain(ToolchainDefinition toolchain) {
		fgList.add(toolchain);
	}

	/**
	 * Try to identify toolchain by name. If not possible, throw
	 * IndexOutOfBoundsException().
	 *
	 * @param sName a string with the toolchain name.
	 * @return non-negative index.
	 */
	public static int findToolchainByName(String sName) {

		int i = 0;
		for (ToolchainDefinition td : fgList) {
			if (td.fName.equals(sName))
				return i;
			i++;
		}
		// not found
		throw new IndexOutOfBoundsException();
	}

	public static int findToolchainByFullName(String sName) {

		int i = 0;
		for (ToolchainDefinition td : fgList) {
			String sFullName = td.getFullName();
			if (sFullName.equals(sName))
				return i;
			i++;
		}
		// not found
		return getDefault();
	}

	public static int findToolchainById(String sId) {

		int i = 0;
		for (ToolchainDefinition td : fgList) {
			if (td.getId().equals(sId.trim()))
				return i;
			i++;
		}
		// not found
		throw new IndexOutOfBoundsException();
	}

	public static int getDefault() {
		return 0;
	}

	public static String[] getArchitectures() {
		return fgArchitectures;
	}

	public static String getArchitecture(int index) {
		return fgArchitectures[index];
	}

	/*
	 * Additional toolchains to be considered.
	 */
	public static void addExtensionsToolchains(String extensionPointId) {
		IConfigurationElement[] elements = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(extensionPointId);
		for (IConfigurationElement element : elements) {

			String id = element.getAttribute("id");
			String name = element.getAttribute("name");

			if (id != null && !id.isEmpty()) {
				try {
					findToolchainById(id);
					Activator.log("Duplicate toolchain id '" + id + "', ignored.");
					continue;
				} catch (IndexOutOfBoundsException e) {
				}
			} else {
				try {
					findToolchainByName(name);
					Activator.log("Duplicate toolchain name '" + name + "', ignored.");
					continue;
				} catch (IndexOutOfBoundsException e) {
				}
			}

			ToolchainDefinition td = new ToolchainDefinition(name);
			if (id != null && !id.isEmpty()) {
				td.setId(id);
			}
			String prefix = element.getAttribute("prefix");
			if (prefix != null && !prefix.isEmpty()) {
				td.setPrefix(prefix);
			}
			String suffix = element.getAttribute("suffix");
			if (suffix != null && !suffix.isEmpty()) {
				td.setSuffix(suffix);
			}
			String architecture = element.getAttribute("architecture");
			if (architecture != null && !architecture.isEmpty()) {
				td.setArchitecture(architecture);
			}
			String cmdMake = element.getAttribute("make_cmd");
			if (cmdMake != null && !cmdMake.isEmpty()) {
				td.setCmdMake(cmdMake);
			}
			String cmdRm = element.getAttribute("remove_cmd");
			if (cmdRm != null && !cmdRm.isEmpty()) {
				td.setCmdRm(cmdRm);
			}
			fgList.add(td);
		}
	}

	// ------------------------------------------------------------------------

	private static final String CUSTOM_TOOLCHAINS_EXTENSION_POINT_ID = "org.eclipse.embedcdt.managedbuild.cross.riscv.core.toolchains";

	// Initialise the list of known toolchains
	static {
		ToolchainDefinition tc;

		tc = new ToolchainDefinition(XPACK_RISCV_GCC_2, "riscv-none-elf-");
		tc.setId("2273142913"); // 2273142912+1
		addToolchain(tc);

		tc = new ToolchainDefinition(RISC_V_GCC_NEWLIB, "riscv64-unknown-elf-");
		tc.setId("2262347901");
		addToolchain(tc);

		tc = new ToolchainDefinition(RISC_V_GCC_LINUX, "riscv64-unknown-linux-gnu-");
		tc.setId("3950568028");
		addToolchain(tc);

		tc = new ToolchainDefinition(RISC_V_GCC_RTEMS, "riscv64-unknown-rtems-");
		tc.setId("3955442865");
		addToolchain(tc);

		tc = new ToolchainDefinition(XPACK_RISCV_GCC, "riscv-none-embed-");
		tc.setId("2273142912");
		tc.setIsDeprecated(true);
		addToolchain(tc);

		tc = new ToolchainDefinition(GME_RISCV_GCC, "riscv-none-embed-");
		tc.setId("512258282");
		tc.setIsDeprecated(true);
		addToolchain(tc);

		// Enumerate extension points and add custom toolchains.
		addExtensionsToolchains(CUSTOM_TOOLCHAINS_EXTENSION_POINT_ID);
	}

	// ------------------------------------------------------------------------
}
