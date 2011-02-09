/*
 * SICCI for Xcode - Jenkins Plugin for Xcode projects
 * 
 * Copyright (C) 2011 Benedikt Biallowons, SIC! Software GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.sic.bb.jenkins.plugins.sicci_for_xcode;


import static com.sic.bb.jenkins.plugins.sicci_for_xcode.util.Constants.ARCHIVE_APP_ARG;
import static com.sic.bb.jenkins.plugins.sicci_for_xcode.util.Constants.BUILD_ARG;
import static com.sic.bb.jenkins.plugins.sicci_for_xcode.util.Constants.BUILD_CONFIGURATIONS_ARG;
import static com.sic.bb.jenkins.plugins.sicci_for_xcode.util.Constants.BUILD_FOLDER_NAME;
import static com.sic.bb.jenkins.plugins.sicci_for_xcode.util.Constants.BUILD_TARGETS_ARG;
import static com.sic.bb.jenkins.plugins.sicci_for_xcode.util.Constants.CLEAN_BEFORE_BUILD_ARG;
import static com.sic.bb.jenkins.plugins.sicci_for_xcode.util.Constants.CREATE_IPA_ARG;
import static com.sic.bb.jenkins.plugins.sicci_for_xcode.util.Constants.DEFAULT_FILENAME_TEMPLATE;
import static com.sic.bb.jenkins.plugins.sicci_for_xcode.util.Constants.DEFAULT_XCODE_PROJECT_SEARCH_DEPTH;
import static com.sic.bb.jenkins.plugins.sicci_for_xcode.util.Constants.FIELD_DELIMITER;
import static com.sic.bb.jenkins.plugins.sicci_for_xcode.util.Constants.FILENAME_TEMPLATE_ARG;
import static com.sic.bb.jenkins.plugins.sicci_for_xcode.util.Constants.MAX_XCODE_PROJECT_SEARCH_DEPTH;
import static com.sic.bb.jenkins.plugins.sicci_for_xcode.util.Constants.MIN_XCODE_PROJECT_SEARCH_DEPTH;
import static com.sic.bb.jenkins.plugins.sicci_for_xcode.util.Constants.PROJECT_DIR_ARG;
import static com.sic.bb.jenkins.plugins.sicci_for_xcode.util.Constants.PROJECT_DIRS_ARG;
import static com.sic.bb.jenkins.plugins.sicci_for_xcode.util.Constants.TRUE;
import static com.sic.bb.jenkins.plugins.sicci_for_xcode.util.Constants.UNIT_TEST_TARGET_ARG;
import static com.sic.bb.jenkins.plugins.sicci_for_xcode.util.Constants.XCODE_PLATFORM_ARG;
import static com.sic.bb.jenkins.plugins.sicci_for_xcode.util.Constants.XCODE_PROJECT_SEARCH_DEPTH_ARG;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.sic.bb.jenkins.plugins.sicci_for_xcode.callables.AppArchiverCallable;
import com.sic.bb.jenkins.plugins.sicci_for_xcode.callables.IpaPackagerCallable;
import com.sic.bb.jenkins.plugins.sicci_for_xcode.callables.XcodeProjectSearchCallable;
import com.sic.bb.jenkins.plugins.sicci_for_xcode.cli.SecurityCommandCaller;
import com.sic.bb.jenkins.plugins.sicci_for_xcode.cli.XcodebuildCommandCaller;
import com.sic.bb.jenkins.plugins.sicci_for_xcode.io.XcodebuildOutputParser;
import com.sic.bb.jenkins.plugins.sicci_for_xcode.util.PluginUtils;
import com.sic.bb.jenkins.plugins.sicci_for_xcode.util.XcodePlatform;

public class XcodeBuilder extends Builder {
	private Map<String,String> data;
    private Map<String,List<String>> dataList;
    
    private transient FilePath currentProjectDirectory;
    private transient String currentUsername;
    private transient String currentPassword;
    private transient boolean currentBuildIsUnitTest; 

    public XcodeBuilder(Map<String,String> data, Map<String,List<String>> dataList) {
    	this.data = data;
    	this.dataList = dataList;
    }
    
    public String getProjectDir() {
    	if(this.data == null || !this.data.containsKey(PROJECT_DIR_ARG))
    		return null;
    	
		return this.data.get(PROJECT_DIR_ARG);
    }
    
    public String getXcodePlatform() {
    	if(this.data == null || !this.data.containsKey(XCODE_PLATFORM_ARG))
    		return null;
    	
    	return this.data.get(XCODE_PLATFORM_ARG);
    }
    
    public String getFilenameTemplate() {
    	if(this.data == null || !this.data.containsKey(FILENAME_TEMPLATE_ARG))
    		return null;
    	
    	String filenameTemplate = this.data.get(FILENAME_TEMPLATE_ARG);
    	
    	if(StringUtils.isBlank(filenameTemplate))
    		return null;
    	
    	return filenameTemplate;
    }
    
    public String getXcodeProjectSearchDepth() {
    	if(this.data == null || !this.data.containsKey(XCODE_PROJECT_SEARCH_DEPTH_ARG))
    		return null;
    	
    	String xcodeProjectSearchDepth = this.data.get(XCODE_PROJECT_SEARCH_DEPTH_ARG);
    	
    	if(StringUtils.isBlank(xcodeProjectSearchDepth))
    		return null;
    	
    	return xcodeProjectSearchDepth;
    }
    
    public boolean getBooleanPreference(String key) {
    	if(this.data == null || !this.data.containsKey(key))
    		return false;
    	
    	return this.data.get(key).equals(TRUE);
    }
    
    public boolean subMenuUsed(String target) {
    	if(this.data == null)
    		return false;
    	
    	for(String key: this.data.keySet())
    		if(key.contains(target))
    			return true;
    	
    	return false;
    }
    
    private void setData(String key, String[] values) {
    	if(values.length == 0)
    		return;
    	
    	if(values.length == 1) {
    		if(this.data == null)
    			this.data = new HashMap<String,String>();
    				
    		this.data.put(key,values[0]);
    		return;
    	}
    		
		List<String> dataList = new ArrayList<String>();	
		
		for(String value: values)
			dataList.add(value);
		
		if(this.dataList == null)
			this.dataList = new HashMap<String,List<String>>();
		
		this.dataList.put(key,dataList);
    }
    
    public String[] getProjectDirs(FilePath workspace) {
    	String[] projectDirs;
    	
    	if(this.data != null && this.data.containsKey(PROJECT_DIRS_ARG)) {
    		projectDirs = new String[] { this.data.get(PROJECT_DIRS_ARG) };
    	} else if(this.dataList != null && this.dataList.containsKey(PROJECT_DIRS_ARG)) {
    		List<String> projectDirsList = this.dataList.get(PROJECT_DIRS_ARG);	
    		projectDirs = (String[]) projectDirsList.toArray(new String[projectDirsList.size()]);
    	} else {
    		projectDirs = getProjectDirsNoCache(workspace);
    		setData(PROJECT_DIRS_ARG,projectDirs);
    	}
    	
    	return projectDirs;
    }
    
    private String[] getProjectDirsNoCache(FilePath workspace) {
    	int searchDepth = MIN_XCODE_PROJECT_SEARCH_DEPTH - 1;
    	
    	try {
    		searchDepth = Integer.parseInt(getXcodeProjectSearchDepth());
    	} catch(NumberFormatException e) {
    	}
    	
    	if(searchDepth < MIN_XCODE_PROJECT_SEARCH_DEPTH || searchDepth > MAX_XCODE_PROJECT_SEARCH_DEPTH
    			|| getDescriptor().getXcodeProjectSearchDepthGlobal())
    		return getDescriptor().getProjectDirs(workspace);

    	return getDescriptor().getProjectDirs(workspace,searchDepth);
    }
    
    public String[] getBuildTargets(FilePath workspace) {
    	String[] buildTargets;
    	
    	if(this.data != null && this.data.containsKey(BUILD_TARGETS_ARG)) {
    		buildTargets = new String[] { this.data.get(BUILD_TARGETS_ARG) };
    	} else if(this.dataList != null && this.dataList.containsKey(BUILD_TARGETS_ARG)) {
    		List<String> buildTargetsList = this.dataList.get(BUILD_TARGETS_ARG);	
    		buildTargets = (String[]) buildTargetsList.toArray(new String[buildTargetsList.size()]);
    	} else {
	    	if(getProjectDir() != null)
	    		buildTargets = XcodebuildOutputParser.getBuildTargets(workspace.child(getProjectDir()));
	    	else
	    		buildTargets = XcodebuildOutputParser.getBuildTargets(workspace);
	    	
	    	setData(BUILD_TARGETS_ARG,buildTargets);
    	}
    	
    	return buildTargets;
    }
    
    public String[] getBuildConfigurations(FilePath workspace) {
    	String[] buildConfigurations;
    	
    	if(this.data != null && this.data.containsKey(BUILD_CONFIGURATIONS_ARG)) {
    		buildConfigurations = new String[] { this.data.get(BUILD_CONFIGURATIONS_ARG) };
    	} else if(this.dataList != null && this.dataList.containsKey(BUILD_CONFIGURATIONS_ARG)) {
    		List<String> buildConfigurationsList = this.dataList.get(BUILD_CONFIGURATIONS_ARG);	
    		buildConfigurations = (String[]) buildConfigurationsList.toArray(new String[buildConfigurationsList.size()]);
    	} else {
	    	if(getProjectDir() != null)
	    		buildConfigurations = XcodebuildOutputParser.getBuildConfigurations(workspace.child(getProjectDir()));
	    	else
	    		buildConfigurations = XcodebuildOutputParser.getBuildConfigurations(workspace);
	    	
	    	setData(BUILD_CONFIGURATIONS_ARG,buildConfigurations);
    	}
    	
    	return buildConfigurations;
    }
    
    @Override
    public XcodeBuilderDescriptor getDescriptor() {
        return (XcodeBuilderDescriptor) super.getDescriptor();
    }
    
    @Override
    public boolean prebuild(AbstractBuild<?,?> build, BuildListener listener) {
		Computer curComputer = Computer.currentComputer();
		FilePath workspace = build.getWorkspace();
        
        listener.getLogger().println("\n" + Messages.XcodeBuilder_prebuild_started() + "\n");
		
        // TODO: check if it's OS X
		if(!curComputer.getNode().createLauncher(listener).isUnix()) {
			listener.fatalError(Messages.XcodeBuilder_prebuild_unixOnly());
			return false;
		}

		if(!XcodebuildCommandCaller.getInstance().check(workspace.getChannel(), listener))
			return false;
		
		if(!SecurityCommandCaller.getInstance().check(workspace.getChannel(), listener))
			return false;
		
		try {
			String[] projectDirs = getProjectDirsNoCache(workspace);
			setData(PROJECT_DIRS_ARG,projectDirs);
			
	        if(getProjectDir() != null)
				workspace = workspace.child(getProjectDir());
	        else if(build.getNumber() == 1 && !projectDirs[0].isEmpty())
	        	workspace = workspace.child(projectDirs[0]);
			
			if(!workspace.exists()) {
				listener.fatalError(Messages.XcodeBuilder_prebuild_projectDirNotFound() + ": " + workspace);
				return false;
			} else
				this.currentProjectDirectory = workspace;
			
			setData(BUILD_TARGETS_ARG,XcodebuildOutputParser.getBuildTargetsNoCache(workspace));
			setData(BUILD_CONFIGURATIONS_ARG,XcodebuildOutputParser.getBuildConfigurationsNoCache(workspace));
		} catch(Exception e) {
			listener.fatalError(Messages.XcodeBuilder_prebuild_projectDirNotFound() + ": " + workspace + "\n");
			return false;
		}
		
		XcodeUserNodeProperty property = XcodeUserNodeProperty.getCurrentNodesProperties();
		
		if(property != null) {
			this.currentUsername = property.getUsername();
			this.currentPassword = property.getPassword();
			
			if(this.currentUsername == null) {
				listener.fatalError(Messages.XcodeBuilder_prebuild_keychainUsernameNotSet() + "\n");
				
				if(XcodePlatform.fromString(getXcodePlatform()) == XcodePlatform.IOS)
					return false;
			}
			
			if(this.currentPassword == null) {
				listener.fatalError(Messages.XcodeBuilder_prebuild_keychainPasswordNotSet() + "\n");
				
				if(XcodePlatform.fromString(getXcodePlatform()) == XcodePlatform.IOS)
					return false;
			}
		} else {
			listener.fatalError(Messages.XcodeBuilder_prebuild_keychainCredentialsNotSet() + "\n");
			
			if(XcodePlatform.fromString(getXcodePlatform()) == XcodePlatform.IOS)
				return false;
		}
		
        listener.getLogger().println(Messages.XcodeBuilder_prebuild_finished() + "\n");
		
    	return true;
    }
    
    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) {
    	PrintStream logger = listener.getLogger();
        XcodeBuilderDescriptor descr = getDescriptor();
        FilePath workspace = this.currentProjectDirectory;
        
        List<String> blackList =  new ArrayList<String>();
		List<Boolean> returnCodes = new ArrayList<Boolean>();
		
        boolean rcode = true;
        
        logger.println("\n" + Messages.XcodeBuilder_perform_started() + "\n");
        
        try {
        	EnvVars envVars = build.getEnvironment(listener);
        	
        	logger.println(Messages.XcodeBuilder_perform_cleanStarted() + "\n");
			
			if(!(descr.getCleanBeforeBuildGlobal() && !descr.getCleanBeforeBuild()))
				for(String toClean: getToPerformStep(CLEAN_BEFORE_BUILD_ARG,(descr.getCleanBeforeBuildGlobal() && descr.getCleanBeforeBuild())))
					returnCodes.add(XcodebuildCommandCaller.getInstance().clean(launcher, envVars, listener, workspace, createArgs(toClean)));
			
			logger.println(Messages.XcodeBuilder_perform_cleanFinished() + "\n\n");
			
			if(this.currentUsername != null && this.currentPassword != null) {
				if(!SecurityCommandCaller.getInstance().unlockKeychain(envVars, listener, workspace, this.currentUsername, this.currentPassword)
						&& XcodePlatform.fromString(getXcodePlatform()) == XcodePlatform.IOS)
					return false;
				
				logger.print("\n");
			}
			
			logger.println(Messages.XcodeBuilder_perform_buildStarted() + "\n");
			
			for(String toBuild: getToPerformStep(BUILD_ARG,true)) {
				rcode = XcodebuildCommandCaller.getInstance().build(launcher, envVars, listener, workspace, this.currentBuildIsUnitTest, createArgs(toBuild));
				
				if(!rcode)
					blackList.add(toBuild);
				
				returnCodes.add(rcode);
			}
			
			logger.println(Messages.XcodeBuilder_perform_buildFinished() + "\n\n");
			
			FilePath buildDir = workspace.child(BUILD_FOLDER_NAME);
			
			logger.println(Messages.XcodeBuilder_perform_archiveAppsStarted() + "\n");
			
			if(!(descr.getArchiveAppGlobal() && !descr.getArchiveApp())) {	
				for(String toArchiveApp: getToPerformStep(ARCHIVE_APP_ARG,(descr.getArchiveAppGlobal() && descr.getArchiveApp()))) {
					if(blackList.contains(toArchiveApp))
						continue;
						
					String[] array = toArchiveApp.split(PluginUtils.stringToPattern(FIELD_DELIMITER));
									
					if(getBooleanPreference(array[0] + FIELD_DELIMITER + UNIT_TEST_TARGET_ARG))
						continue;
					
					logger.print(Messages.XcodeBuilder_perform_archivingApp() + ": " + array[0] + " " + array[1]);
					
					FilePath tempBuildDir = getBuildDir(buildDir, array[1]);
					
					if(tempBuildDir != null && tempBuildDir.act(new AppArchiverCallable(array[0], createFilename(build, array[0], array[1])))) {
						logger.println(" " + Messages.XcodeBuilder_perform_archivingAppDone());
						returnCodes.add(true);
					} else {
						logger.println(" " + Messages.XcodeBuilder_perform_archivingAppFailed());
						returnCodes.add(false);
					}
				}
			}
			
			logger.println("\n" + Messages.XcodeBuilder_perform_archiveAppsFinished() + "\n\n");
			logger.println(Messages.XcodeBuilder_perform_createIpasStarted() + "\n");
			
			if(!(descr.getCreateIpaGlobal() && !descr.getCreateIpa())) {	
				for(String toCreateIpa: getToPerformStep(CREATE_IPA_ARG,(descr.getCreateIpaGlobal() && descr.getCreateIpa()))) {
					if(blackList.contains(toCreateIpa))
						continue;
						
					String[] array = toCreateIpa.split(PluginUtils.stringToPattern(FIELD_DELIMITER));
										
					if(getBooleanPreference(array[0] + FIELD_DELIMITER + UNIT_TEST_TARGET_ARG))
						continue;
					
					logger.print(Messages.XcodeBuilder_perform_creatingIpa() + ": " + array[0] + " " + array[1]);
						
					FilePath tempBuildDir = getBuildDir(buildDir, array[1]);
					
					if(tempBuildDir != null && tempBuildDir.act(new IpaPackagerCallable(array[0], createFilename(build, array[0], array[1])))) {
						logger.println(" " + Messages.XcodeBuilder_perform_creatingIpaDone());
						returnCodes.add(true);
					} else {
						logger.println(" " + Messages.XcodeBuilder_perform_creatingIpaFailed());
						returnCodes.add(false);
					}
				}
			}
			
			logger.println("\n" + Messages.XcodeBuilder_perform_createIpasFinished() + "\n\n");
			
			logger.println(Messages.XcodeBuilder_perform_finished() + "\n\n");
			
			if(returnCodes.contains(false))
				return false;
			
			return true;
		} catch (Exception e) {
			logger.println(e.getStackTrace());
		}
        
        return false;
    }
    
    private Set<String> getToPerformStep(String cmd, boolean force) {
    	Set<String> toPerformStep = new HashSet<String>();
    	
    	for(String key: this.data.keySet()) {
			if(StringUtils.countMatches(key, FIELD_DELIMITER) < 2)
				continue;
			
			String[] fields = key.split(PluginUtils.stringToPattern(FIELD_DELIMITER));
			
			if(!cmd.equals(BUILD_ARG) && (!fields[fields.length - 1].equals(cmd) || (!force && !this.data.get(key).equals(TRUE))))
				continue;
			
			toPerformStep.add(fields[0] + FIELD_DELIMITER + fields[1]);
		}
    	
    	return toPerformStep;
    }
    
    private FilePath getBuildDir(FilePath buildDir, String configurationName) {
		String configBuildDirName = null;
		
		try {
			for(String configBuildDirNameTemp: XcodePlatform.fromString(getXcodePlatform()).getProjectBuildDirNames(configurationName)) {
				if(buildDir.child(configBuildDirNameTemp).isDirectory()) {
					configBuildDirName = configBuildDirNameTemp;
					break;
				}
			}
		} catch (Exception e) {
			return null;
		}
			
		if(configBuildDirName == null)
			return null;	
		else
			return buildDir.child(configBuildDirName);
    }
    
    private List<String> createArgs(String arg) {
    	List<String> cmds = new ArrayList<String>();
    	String[] args = arg.split(PluginUtils.stringToPattern(FIELD_DELIMITER));
		
		cmds.add("-target");
		cmds.add(args[0]);
		cmds.add("-configuration");
		cmds.add(args[1]);
		
		if(getBooleanPreference(args[0] + FIELD_DELIMITER + UNIT_TEST_TARGET_ARG)) {
			// iOS unit test have to be run with iOS Simulator platform
			if(getXcodePlatform().equals(XcodePlatform.IOS.getXcodePlatformName())) {
				cmds.add("-sdk");
				cmds.add(XcodePlatform.IOS_SIMULATOR.getXcodePlatformSdkName());
			}
			
			this.currentBuildIsUnitTest = true;
    	} else
    		this.currentBuildIsUnitTest = false;
		
		return cmds;
    }
    
    private String createFilename(AbstractBuild<?,?> build, String targetName, String configurationName) {
    	String filename;
    	
    	if(getDescriptor().filenameTemplateGlobal)
    		filename = getDescriptor().getFilenameTemplate();
    	else
    		filename = getFilenameTemplate();
    	
    	if(filename == null)
    		filename = DEFAULT_FILENAME_TEMPLATE;
    	
    	Date buildTimeStamp = build.getTimestamp().getTime();
    	
    	filename = filename.replaceAll("<SECOND>",new SimpleDateFormat("ss").format(buildTimeStamp));
    	filename = filename.replaceAll("<MINUTE>",new SimpleDateFormat("mm").format(buildTimeStamp));
    	filename = filename.replaceAll("<HOUR>",new SimpleDateFormat("HH").format(buildTimeStamp));
    	filename = filename.replaceAll("<DAY>",new SimpleDateFormat("dd").format(buildTimeStamp));
    	filename = filename.replaceAll("<MONTH>",new SimpleDateFormat("MM").format(buildTimeStamp));
    	filename = filename.replaceAll("<YEAR>",new SimpleDateFormat("yyyy").format(buildTimeStamp));
    	filename = filename.replaceAll("<TIME>",new SimpleDateFormat("HH_mm_ss").format(buildTimeStamp));
    	filename = filename.replaceAll("<DATE>",new SimpleDateFormat("yyyy_MM_dd").format(buildTimeStamp));
    	filename = filename.replaceAll("<DATETIME>",new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss").format(buildTimeStamp));
    	filename = filename.replaceAll("<BUILD>",String.valueOf(build.getNumber()));
    	filename = filename.replaceAll("<TARGET>",targetName);
    	filename = filename.replaceAll("<CONFIG>",configurationName);
    	
    	return filename;
    }
    
    @Extension
    public static final class XcodeBuilderDescriptor extends BuildStepDescriptor<Builder> {
    	private String filenameTemplate;
        private boolean filenameTemplateGlobal;
        private int xcodeProjectSearchDepth;
        private boolean xcodeProjectSearchDepthGlobal;
        private boolean cleanBeforeBuild, cleanBeforeBuildGlobal;
        private boolean archiveApp, archiveAppGlobal;
        private boolean createIpa, createIpaGlobal;
        
    	private transient Map<String,FilePath> projectWorkspaceMap;
    	private transient FilePath currentProjectDir;
        
        public XcodeBuilderDescriptor() {
        	super(XcodeBuilder.class);
        	load();
        	
        	this.projectWorkspaceMap = new HashMap<String,FilePath>();
        }
        
        public String getDisplayName() {
            return Messages.XcodeBuilderDescriptor_getDisplayName();
        }
        
        @Override
        public XcodeBuilder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
        	Map<String,String> stringMap = new HashMap<String,String>();
        	Map<String,List<String>> stringListMap = new HashMap<String,List<String>>();
        	
        	collectFormData(formData,stringMap,stringListMap);
        	
        	return new XcodeBuilder(stringMap,stringListMap);
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            
            return super.configure(req,formData);
        }
        
        @SuppressWarnings("rawtypes")
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        	return jobType == FreeStyleProject.class;
        }
        
        @SuppressWarnings("unchecked")
		private void collectFormData(JSONObject formData, Map<String,String> stringMap, Map<String,List<String>> stringListMap) {
			Iterator<String> it = formData.keys();
			
        	while(it.hasNext()) {
        		String key = it.next();
        		
        		if(formData.get(key).getClass() == JSONArray.class) {
        			for(int i = 0; i < formData.getJSONArray(key).size(); i++) {
        				if(formData.getJSONArray(key).get(i).getClass() == JSONObject.class)
        					collectFormData(formData.getJSONArray(key).getJSONObject(i), stringMap, stringListMap);
        				else if(formData.getJSONArray(key).get(i).getClass() == String.class)
        					collectFormData(new JSONObject().element(key, formData.getJSONArray(key).getString(i)), stringMap, stringListMap);
        			}	
        		} else if(formData.get(key).getClass() == JSONObject.class) {
        			collectFormData(formData.getJSONObject(key), stringMap, stringListMap);
        		} else {
    				if(stringListMap.containsKey(key)) {
    					stringListMap.get(key).add(StringUtils.strip(formData.getString(key)));
    				} else if(stringMap.containsKey(key)) {
    					List<String> stringList = new ArrayList<String>();
    					stringList.add(stringMap.remove(key));
    					stringList.add(StringUtils.strip(formData.getString(key)));
    					stringListMap.put(key, stringList);
    				} else
    					stringMap.put(key, StringUtils.strip(formData.getString(key)));
        		}
        	}
        }
        
        public void setXcodeProjectSearchDepth(String searchDepth) {        	
        	try {
        		this.xcodeProjectSearchDepth = Integer.parseInt(StringUtils.strip(searchDepth));
        		
        		if(this.xcodeProjectSearchDepth < MIN_XCODE_PROJECT_SEARCH_DEPTH ||
        				this.xcodeProjectSearchDepth > MAX_XCODE_PROJECT_SEARCH_DEPTH)
        			this.xcodeProjectSearchDepth = DEFAULT_XCODE_PROJECT_SEARCH_DEPTH;
        	} catch(NumberFormatException e) {
        		this.xcodeProjectSearchDepth = DEFAULT_XCODE_PROJECT_SEARCH_DEPTH;
        	}
        }
        
        public String getXcodeProjectSearchDepth() {
    		if(this.xcodeProjectSearchDepth < MIN_XCODE_PROJECT_SEARCH_DEPTH ||
    				this.xcodeProjectSearchDepth > MAX_XCODE_PROJECT_SEARCH_DEPTH)
    			return String.valueOf(DEFAULT_XCODE_PROJECT_SEARCH_DEPTH);
    		
        	return String.valueOf(this.xcodeProjectSearchDepth);
        }
        
        public void setXcodeProjectSearchDepthGlobal(boolean searchDepthGlobal) {
        	this.xcodeProjectSearchDepthGlobal = searchDepthGlobal;
        }
        
        public boolean getXcodeProjectSearchDepthGlobal() {
        	return this.xcodeProjectSearchDepthGlobal;
        }
        
        public void setCleanBeforeBuild(boolean cleanBeforeBuild) {
        	this.cleanBeforeBuild = cleanBeforeBuild;
        }
        
        public boolean getCleanBeforeBuild() {
            return this.cleanBeforeBuild;
        }
        
        public void setCleanBeforeBuildGlobal(boolean cleanBeforeBuildGlobal) {
        	this.cleanBeforeBuildGlobal = cleanBeforeBuildGlobal;
        }
        
        public boolean getCleanBeforeBuildGlobal() {
            return this.cleanBeforeBuildGlobal;
        }
        
        public void setArchiveApp(boolean archiveApp) {
        	this.archiveApp = archiveApp;
        }
        
        public boolean getArchiveApp() {
            return this.archiveApp;
        }
        
        public void setArchiveAppGlobal(boolean archiveAppGlobal) {
        	this.archiveAppGlobal = archiveAppGlobal;
        }
        
        public boolean getArchiveAppGlobal() {
            return this.archiveAppGlobal;
        }
        
        public void setCreateIpa(boolean createIpa) {
        	this.createIpa = createIpa;
        }
        
        public boolean getCreateIpa() {
            return this.createIpa;
        }
        
        public void setCreateIpaGlobal(boolean createIpaGlobal) {
        	this.createIpaGlobal = createIpaGlobal;
        }
        
        public boolean getCreateIpaGlobal() {
            return this.createIpaGlobal;
        }
        
        public void setFilenameTemplate(String filenameTemplate) {
        	if(StringUtils.isBlank(filenameTemplate))
        		this.filenameTemplate = DEFAULT_FILENAME_TEMPLATE;
        	else
        		this.filenameTemplate = StringUtils.strip(filenameTemplate);
        }
        
        public String getFilenameTemplate() {
        	if(StringUtils.isBlank(this.filenameTemplate))
        		return DEFAULT_FILENAME_TEMPLATE;
        	
        	return this.filenameTemplate;
        }
        
        public void setFilenameTemplateGlobal(boolean filenameTemplateGlobal) {
        	this.filenameTemplateGlobal = filenameTemplateGlobal;
        }
        
        public boolean getFilenameTemplateGlobal() {
        	return this.filenameTemplateGlobal;
        }
        
        public FilePath getProjectDir() {
        	return this.currentProjectDir;
        }
        
        public String[] getXcodePlatformNames() {
        	return XcodePlatform.getXcodePlatformNames();
        }
        
        public String getXcodePlatform() {
        	return null;
        }
        
        public boolean subMenuUsed(String target) {
        	return false;
        }
        
        public boolean getBooleanPreference(String key) {
        	return false;
        }
        
        public void doAjaxTargets(StaplerRequest req, StaplerResponse rsp, @QueryParameter String jobName, @QueryParameter String projectDir) throws IOException, ServletException {
        	if(this.projectWorkspaceMap.containsKey(jobName))
        		this.currentProjectDir = this.projectWorkspaceMap.get(jobName).child(projectDir);
        	
        	req.getView(this,'/' + XcodeBuilder.class.getName().replaceAll("\\.","\\/")  + "/targets.jelly").forward(req, rsp);
        	
        	this.currentProjectDir = null;
        }
        
        public FormValidation doCheckXcodeProjectSearchDepth(@QueryParameter String value) throws IOException, ServletException {
        	if(StringUtils.isBlank(value))
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckXcodeProjectSearchDepth_emptyValue() + " (min " + MIN_XCODE_PROJECT_SEARCH_DEPTH +
        				", max " + MAX_XCODE_PROJECT_SEARCH_DEPTH + ')');
        	
        	int xcodeProjectSearchDepth;
        	
        	try {
        		xcodeProjectSearchDepth = Integer.parseInt(StringUtils.strip(value));
        	} catch(NumberFormatException e) {
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckXcodeProjectSearchDepth_valueNotANumber());
        	}
 
        	if(xcodeProjectSearchDepth < MIN_XCODE_PROJECT_SEARCH_DEPTH)
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckXcodeProjectSearchDepth_valueTooSmall() + " (min " + MIN_XCODE_PROJECT_SEARCH_DEPTH + ')');
        	else if(xcodeProjectSearchDepth > MAX_XCODE_PROJECT_SEARCH_DEPTH)
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckXcodeProjectSearchDepth_valueTooBig() + " (max " + MAX_XCODE_PROJECT_SEARCH_DEPTH + ')');
        	
            return FormValidation.ok();
        }
        
        public FormValidation doCheckFilenameTemplate(@QueryParameter String value) throws IOException, ServletException {
        	if(StringUtils.isBlank(value))
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckFilenameTemplate_setDefaultFilename());
        	
            return FormValidation.ok();
        }
        
        public String[] getProjectDirs(FilePath workspace) {
        	if(this.xcodeProjectSearchDepth < MIN_XCODE_PROJECT_SEARCH_DEPTH 
        			|| this.xcodeProjectSearchDepth > MAX_XCODE_PROJECT_SEARCH_DEPTH)
        		return getProjectDirs(workspace, DEFAULT_XCODE_PROJECT_SEARCH_DEPTH);
        		
        	return getProjectDirs(workspace, this.xcodeProjectSearchDepth);
        }
        
        @SuppressWarnings("deprecation")
		public String[] getProjectDirs(FilePath workspace, int searchDepth) {
        	List<String> projectDirs = new ArrayList<String>();
        	
        	if(workspace.isRemote())
        		this.projectWorkspaceMap.put(workspace.getName(),workspace);
        	else
        		this.projectWorkspaceMap.put(workspace.getParent().getName(),workspace);
        	
			try {
				projectDirs.addAll(workspace.act(new XcodeProjectSearchCallable(searchDepth)));
			} catch (Exception e) {
			}
			
        	String[] projectDirsArray = new String[projectDirs.size()];
        	
        	for(int i = 0; i < projectDirs.size(); i++) {
        		String path = projectDirs.get(i);
        		
        		// TODO don't use this deprecated method
        		projectDirsArray[i] = path.substring(workspace.toString().length() + 1,path.length());
        	}
        	
        	return projectDirsArray;
        }
        
        public static String[] getBuildTargets(FilePath workspace) {
        	return XcodebuildOutputParser.getBuildTargets(workspace);
        }
        
        public static String[] getBuildConfigurations(FilePath workspace) {
        	return XcodebuildOutputParser.getBuildConfigurations(workspace);
        }
        
        public static String replaceSpaces(String withSpaces) {
        	return withSpaces.replaceAll("\\s", "_");
        }
        
        /* <j:getStatic> doesn't work -> classloader problem */
        public static String getProjectDirArg() {
        	return PROJECT_DIR_ARG;
        }
        
        /* <j:getStatic> doesn't work -> classloader problem */
        public static String getXcodePlatformArg() {
        	return XCODE_PLATFORM_ARG;
        }
        
        /* <j:getStatic> doesn't work -> classloader problem */
        public static String getFilenameTemplateArg() {
        	return FILENAME_TEMPLATE_ARG;
        }
        
        /* <j:getStatic> doesn't work -> classloader problem */
        public static String getXcodeProjectSearchDepthArg() {
        	return XCODE_PROJECT_SEARCH_DEPTH_ARG;
        }
        
        /* <j:getStatic> doesn't work -> classloader problem */
        public static String getProjectDirsArg() {
        	return PROJECT_DIRS_ARG;
        }
        
        /* <j:getStatic> doesn't work -> classloader problem */
        public static String getBuildTargetsArg() {
        	return BUILD_TARGETS_ARG;
        }
        
        /* <j:getStatic> doesn't work -> classloader problem */
        public static String getBuildConfigurationsArg() {
        	return BUILD_CONFIGURATIONS_ARG;
        }
        
        /* <j:getStatic> doesn't work -> classloader problem */
        public static String getFieldDelimiter() {
        	return FIELD_DELIMITER;
        }
        
        /* <j:getStatic> doesn't work -> classloader problem */
        public static String getUnitTestTargetArg() {
        	return UNIT_TEST_TARGET_ARG;
        }
        
        /* <j:getStatic> doesn't work -> classloader problem */
        public static String getCleanBeforeBuildArg() {
        	return CLEAN_BEFORE_BUILD_ARG;
        }
        
        /* <j:getStatic> doesn't work -> classloader problem */ 
        public static String getCreateIpaArg() {
        	return CREATE_IPA_ARG;
        }
        
        /* <j:getStatic> doesn't work -> classloader problem */
        public static String getArchiveAppArg() {
        	return ARCHIVE_APP_ARG;
        }
    }
}