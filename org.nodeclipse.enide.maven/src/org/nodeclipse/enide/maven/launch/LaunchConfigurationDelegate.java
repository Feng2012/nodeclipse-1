package org.nodeclipse.enide.maven.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
//import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.jface.preference.IPreferenceStore;
import org.nodeclipse.enide.maven.Activator;
import org.nodeclipse.enide.maven.preferences.Dialogs;
//import org.nodeclipse.debug.util.Constants;
//import org.nodeclipse.debug.util.VariablesUtil;
//import org.nodeclipse.ui.Activator;
//import org.nodeclipse.ui.preferences.Dialogs;
//import org.nodeclipse.ui.preferences.PreferenceConstants;
//import org.nodeclipse.ui.util.NodeclipseConsole;
import org.nodeclipse.enide.maven.preferences.MavenConstants;
import org.nodeclipse.enide.maven.util.VariablesUtil;

/**
 * pom.xml Run As Maven build<br>
 * see LaunchConfigurationDelegate in .debug and .phantomjs, .jjs module for comparison.
 * 
 * @since 0.10
 * @author Paul Verest
 */
public class LaunchConfigurationDelegate implements ILaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {

		IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
		
		// Using configuration to build command line	
		List<String> cmdLine = new ArrayList<String>();
		
		// Maven installation path should be stored in preference.
		String mavenPath= preferenceStore.getString(MavenConstants.MAVEN_PATH);
		// Check if the maven location is correctly configured
		File mavenFile = new File(mavenPath);
		if(!mavenFile.exists()){
			// If the location is not valid than show a dialog which prompts the user to goto the preferences page
			Dialogs.showPreferencesDialog(MavenConstants.PREFERENCES_PAGE,
					"Maven installation is not correctly configured.\n\n"
					+ "Please goto Window -> Preferences -> "+MavenConstants.PREFERENCE_PAGE_NAME
					+" and configure the correct location");
			return;
		}			
		cmdLine.add(mavenPath);

		String file = configuration.getAttribute("KEY_FILE_PATH",	"");
		String filePath = ResourcesPlugin.getWorkspace().getRoot().findMember(file).getLocation().toOSString();
		// path is relative, so cannot find it, unless get absolute path
		cmdLine.add("-f");  //  -f,--file <arg>                        Force the use of an alternate POM
		cmdLine.add(filePath);
		cmdLine.add("package");
		
		String workingDirectory = configuration.getAttribute(MavenConstants.ATTR_WORKING_DIRECTORY, "");
		File workingPath = null;
		if(workingDirectory.length() == 0) {
			workingPath = (new File(filePath)).getParentFile();
		} else {
			workingDirectory = VariablesUtil.resolveValue(workingDirectory);
			if(workingDirectory == null) {
				workingPath = (new File(filePath)).getParentFile();
			} else {
				workingPath = new File(workingDirectory);
			}
		}
		
		Map<String, String> envm = new HashMap<String, String>();
		envm = configuration.getAttribute(MavenConstants.ATTR_ENVIRONMENT_VARIABLES, envm);
		String[] envp = new String[envm.size() + 2];
		int idx = 0;
		for(String key : envm.keySet()) {
			String value = envm.get(key);
			envp[idx++] = key + "=" + value;
		}
		envp[idx++] = "JAVA_HOME=" + System.getProperty("java.home"); //System.getenv("JAVA_HOME");
//ERROR: M2_HOME not found in your environment.
//Please set the M2_HOME variable in your environment to match the
//location of the Maven installation
		envp[idx++] = "M2_HOME=" + System.getenv("MAVEN_HOME");
		
		
//		for(String s : cmdLine) NodeclipseConsole.write(s+" ");
//		NodeclipseConsole.write("\n");
		
		String[] cmds = {};
		cmds = cmdLine.toArray(cmds);
		// Launch a process to debug.eg,
		Process p = DebugPlugin.exec(cmds, workingPath, envp);
		RuntimeProcess process = (RuntimeProcess)DebugPlugin.newProcess(launch, p, MavenConstants.PROCESS_MESSAGE);
		
	}

}