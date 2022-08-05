/*******************************************************************************
 * Copyright (c) 2015 Liviu Ionescu.
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
 *     Liviu Ionescu - UI part extraction.
 *******************************************************************************/

package org.eclipse.embedcdt.internal.managedbuild.cross.ui.preferences;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.embedcdt.internal.managedbuild.cross.ui.Activator;
import org.eclipse.embedcdt.internal.managedbuild.cross.ui.Messages;
import org.eclipse.embedcdt.managedbuild.cross.core.preferences.DefaultPreferences;
import org.eclipse.embedcdt.managedbuild.cross.core.preferences.PersistentPreferences;
import org.eclipse.embedcdt.ui.XpackDirectoryNotStrictFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */

public class BuildToolsGlobalPathsPreferencesPage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	// ------------------------------------------------------------------------

	public static final String ID = "org.eclipse.embedcdt.internal.managedbuild.cross.ui.preferencePage.globalToolsPaths";

	// ------------------------------------------------------------------------

	private DefaultPreferences fDefaultPreferences;

	// ------------------------------------------------------------------------

	public BuildToolsGlobalPathsPreferencesPage() {
		super(GRID);

		fDefaultPreferences = Activator.getInstance().getDefaultPreferences();

		setPreferenceStore(new ScopedPreferenceStore(ConfigurationScope.INSTANCE, Activator.CORE_PLUGIN_ID));

		setDescription(Messages.GlobalBuildToolsPathsPreferencesPage_description);
	}

	// ------------------------------------------------------------------------

	// Contributed by IWorkbenchPreferencePage
	@Override
	public void init(IWorkbench workbench) {
		if (Activator.getInstance().isDebugging()) {
			System.out.println("GlobalToolsPathsPreferencePage.init()");
		}
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common GUI
	 * blocks needed to manipulate various types of preferences. Each field editor
	 * knows how to save and restore itself.
	 */
	@Override
	protected void createFieldEditors() {

		boolean isStrict;
		isStrict = fDefaultPreferences.getBoolean(PersistentPreferences.GLOBAL_BUILDTOOLS_PATH_STRICT, true);

		String[] xpackNames = fDefaultPreferences.getBuildToolsXpackNames();

		FieldEditor buildToolsPathField = new XpackDirectoryNotStrictFieldEditor(xpackNames,
				PersistentPreferences.BUILD_TOOLS_PATH_KEY, Messages.BuildToolsPaths_label, getFieldEditorParent(),
				isStrict);

		addField(buildToolsPathField);
	}

	// ------------------------------------------------------------------------
}
