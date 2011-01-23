package com.sic.bb.hudson.plugins.xcodeplugin;

import static com.sic.bb.hudson.plugins.xcodeplugin.util.Constants.ARCHIVE_APP_ARG;
import static com.sic.bb.hudson.plugins.xcodeplugin.util.Constants.BUILD_FOLDER_NAME;
import static com.sic.bb.hudson.plugins.xcodeplugin.util.Constants.CLEAN_BEFORE_BUILD_ARG;
import static com.sic.bb.hudson.plugins.xcodeplugin.util.Constants.CREATE_IPA_ARG;
import static com.sic.bb.hudson.plugins.xcodeplugin.util.Constants.DEFAULT_FILENAME_TEMPLATE;
import static com.sic.bb.hudson.plugins.xcodeplugin.util.Constants.DEFAULT_XCODE_PROJECT_SEARCH_DEPTH;
import static com.sic.bb.hudson.plugins.xcodeplugin.util.Constants.FIELD_DELIMITER;
import static com.sic.bb.hudson.plugins.xcodeplugin.util.Constants.FIELD_DELIMITER_REGEX;
import static com.sic.bb.hudson.plugins.xcodeplugin.util.Constants.MAX_XCODE_PROJECT_SEARCH_DEPTH;
import static com.sic.bb.hudson.plugins.xcodeplugin.util.Constants.MIN_XCODE_PROJECT_SEARCH_DEPTH;
import static com.sic.bb.hudson.plugins.xcodeplugin.util.Constants.TRUE;
import static com.sic.bb.hudson.plugins.xcodeplugin.util.Constants.UNIT_TEST_TARGET_ARG;

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

import com.sic.bb.hudson.plugins.xcodeplugin.callables.AppArchiverCallable;
import com.sic.bb.hudson.plugins.xcodeplugin.callables.IpaPackagerCallable;
import com.sic.bb.hudson.plugins.xcodeplugin.callables.XcodeProjectSearchCallable;
import com.sic.bb.hudson.plugins.xcodeplugin.cli.SecurityCommandCaller;
import com.sic.bb.hudson.plugins.xcodeplugin.cli.XcodebuildCommandCaller;
import com.sic.bb.hudson.plugins.xcodeplugin.io.XcodebuildCommandOutputParser;
import com.sic.bb.hudson.plugins.xcodeplugin.util.XcodePlatform;

public class XcodeBuilder extends Builder {
    private final Map<String,String> data;
    
    private transient FilePath currentProjectDirectory;
    private transient String currentUsername;
    private transient String currentPassword;

    public XcodeBuilder(Map<String,String> data) {
    	this.data = data;
    }
    
    public String getProjectDir() {
    	if(!this.data.containsKey("ProjectDir"))
    		return null;
    	
		// TODO ProjectDir could contain slashes (right dir separator?)
    	return this.data.get("ProjectDir");
    }
    
    public String getXcodePlatform() {
    	if(!this.data.containsKey("XcodePlatform"))
    		return null;
    	
    	return this.data.get("XcodePlatform");
    }
    
    public String getFilenameTemplate() {
    	if(!this.data.containsKey("FilenameTemplate"))
    		return null;
    	
    	return this.data.get("FilenameTemplate");
    }
    
    public String getXcodeProjSearchDepth() {
    	if(!this.data.containsKey("XcodeProjSearchDepth"))
    		return null;
    	
    	return this.data.get("XcodeProjSearchDepth");
    }
    
    public boolean getBooleanPreference(String key) {
    	if(!this.data.containsKey(key))
    		return false;
    	
    	return this.data.get(key).equals(TRUE);
    }
    
    public boolean subMenuUsed(String target) {
    	for(String key: (String[]) this.data.keySet().toArray(new String[this.data.size()]))
    		if(key.contains(target))
    			return true;
    	
    	return false;
    }
    
    public String[] getProjectDirs(FilePath workspace) {
    	int searchDepth = MIN_XCODE_PROJECT_SEARCH_DEPTH - 1;
    	
    	try {
    		searchDepth = Integer.parseInt(getXcodeProjSearchDepth());
    	} catch(NumberFormatException e) {
    	}
    	
    	if(searchDepth < MIN_XCODE_PROJECT_SEARCH_DEPTH || searchDepth > MAX_XCODE_PROJECT_SEARCH_DEPTH
    			|| getDescriptor().getXcodeProjSearchDepthGlobal())
    		return getDescriptor().getProjectDirs(workspace);

    	return getDescriptor().getProjectDirs(workspace,searchDepth);
    }
    
    public String[] getAvailableSdks(FilePath workspace) {
    	if(getProjectDir() != null)
    		return XcodebuildCommandOutputParser.getAvailableSdks(workspace.child(getProjectDir()));
    	
    	return XcodebuildCommandOutputParser.getAvailableSdks(workspace);
    }

    public String[] getBuildConfigurations(FilePath workspace) {
    	if(getProjectDir() != null)
    		return XcodebuildCommandOutputParser.getBuildConfigurations(workspace.child(getProjectDir()));

    	return XcodebuildCommandOutputParser.getBuildConfigurations(workspace);
    }
    
    public String[] getBuildTargets(FilePath workspace) {
    	if(getProjectDir() != null)
    		return XcodebuildCommandOutputParser.getBuildTargets(workspace.child(getProjectDir()));

    	return XcodebuildCommandOutputParser.getBuildTargets(workspace);
    }
    
    @Override
    public XcodeBuilderDescriptor getDescriptor() {
        return (XcodeBuilderDescriptor) super.getDescriptor();
    }
    
    @Override
    public boolean prebuild(AbstractBuild<?,?> build, BuildListener listener) {
		Computer curComputer = Computer.currentComputer();
		FilePath workspace = build.getWorkspace();
		
		// to empty cache of XcodebuildParser
        XcodebuildCommandCaller.getInstance().setWorkspaceTemp(workspace.getParent().getName());
		
		if(!curComputer.getNode().createLauncher(listener).isUnix()) {
			listener.fatalError(Messages.XcodeBuilder_prebuild_unixOnly());
			return false;
		}
		
        if(getProjectDir() != null)
			workspace = workspace.child(getProjectDir());
		
		try {
			if(!XcodebuildCommandCaller.getInstance().check(workspace.getChannel(), listener))
				return false;
			
			if(!workspace.exists()) {
				listener.fatalError(Messages.XcodeBuilder_prebuild_projectDirNotFound() + ": " + workspace);
				return false;
			} else
				this.currentProjectDirectory = workspace;
		} catch(Exception e) {
			// TODO
		}
		
		XcodeUserNodeProperty property = XcodeUserNodeProperty.getCurrentNodesProperties();
		
		if(property == null) {
			listener.fatalError("xcode preferences not set");
			return false;
		}
		
		this.currentUsername = property.getUsername();
		this.currentPassword = property.getPassword();
		
		if(this.currentUsername == null) {
			// TODO
			listener.fatalError("xcode username not set");
			return false;
		}
		
		if(this.currentPassword == null) {
			// TODO
			listener.fatalError("xcode password not set");
			return false;
		}
		
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
        
        logger.println(Messages.XcodeBuilder_perform_started());
        
        try {
        	EnvVars envVars = build.getEnvironment(listener);
			
			// <cleanup>
			
			if(!(descr.getCleanBeforeBuildGlobal() && !descr.getCleanBeforeBuild()))
				for(String toClean: getToPerformStep(CLEAN_BEFORE_BUILD_ARG,(descr.getCleanBeforeBuildGlobal() && descr.getCleanBeforeBuild())))
					returnCodes.add(XcodebuildCommandCaller.getInstance().clean(envVars, listener, workspace, createArgs(toClean)));
			
			// </cleanup>
			
			// <build>

			if(!SecurityCommandCaller.getInstance().unlockKeychain(envVars, listener, workspace, this.currentUsername, this.currentPassword))
				return false;

			
			for(String toBuild: getToPerformStep("build",true)) {
				rcode = XcodebuildCommandCaller.getInstance().build(envVars, listener, workspace, createArgs(toBuild));
				
				if(!rcode)
					blackList.add(toBuild);
				
				returnCodes.add(rcode);
			}
			
			// </build>
			
			
			FilePath buildDir = workspace.child(BUILD_FOLDER_NAME);
			
			// <archive app>
			
			if(!(descr.getArchiveAppGlobal() && !descr.getArchiveApp())) {	
				for(String toArchiveApp: getToPerformStep(ARCHIVE_APP_ARG,(descr.getArchiveAppGlobal() && descr.getArchiveApp()))) {
					if(blackList.contains(toArchiveApp)) {
						returnCodes.add(false);
						continue;
					}
						
					String[] array = toArchiveApp.split(FIELD_DELIMITER_REGEX);
					
					FilePath tempBuildDir = getBuildDir(buildDir, array[1]);
					
					if(tempBuildDir != null && tempBuildDir.act(new AppArchiverCallable(array[0], createFilename(build, array[0], array[1]))))
						returnCodes.add(true);
					else
						returnCodes.add(false);
				}
			}
			
			// </archive app>
			
			// <create ipa>
			
			if(!(descr.getCreateIpaGlobal() && !descr.getCreateIpa())) {	
				for(String toCreateIpa: getToPerformStep(CREATE_IPA_ARG,(descr.getCreateIpaGlobal() && descr.getCreateIpa()))) {
					if(blackList.contains(toCreateIpa)) {
						returnCodes.add(false);
						continue;
					}
						
					String[] array = toCreateIpa.split(FIELD_DELIMITER_REGEX);
						
					FilePath tempBuildDir = getBuildDir(buildDir, array[1]);
					
					if(tempBuildDir != null && tempBuildDir.act(new IpaPackagerCallable(array[0], createFilename(build, array[0], array[1]))))
						returnCodes.add(true);
					else
						returnCodes.add(false);
				}
			}
			
			// </create ipa>
			
			return !returnCodes.contains(false);
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
			
			String[] fields = key.split(FIELD_DELIMITER_REGEX);
			
			if(!cmd.equals("build") && (!fields[fields.length - 1].equals(cmd) || (!force && !this.data.get(key).equals(TRUE))))
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
    	String[] args = arg.split(FIELD_DELIMITER_REGEX);
		
		cmds.add("-target");
		cmds.add(args[0]);
		cmds.add("-configuration");
		cmds.add(args[1]);
		
		if(getBooleanPreference(args[0] + FIELD_DELIMITER + UNIT_TEST_TARGET_ARG)) {
			if(getXcodePlatform().equals(XcodePlatform.IOS.getXcodePlatformName())) {
				cmds.add("-sdk");
				cmds.add(XcodePlatform.IOS_SIMULATOR.getXcodePlatformSdkName());
			}
    	}
		
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
    	private transient Map<String,FilePath> projectWorkspaceMap;
    	private transient FilePath currentProjectDir;
    	
    	private String filenameTemplate;
        private boolean filenameTemplateGlobal;
        private int xcodeProjSearchDepth;
        private boolean xcodeProjSearchDepthGlobal;
        private boolean cleanBeforeBuild, cleanBeforeBuildGlobal;
        private boolean archiveApp, archiveAppGlobal;
        private boolean createIpa, createIpaGlobal;
        
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
        	return new XcodeBuilder(collectFormData(formData));
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
		private Map<String,String> collectFormData(JSONObject formData) {
        	Map<String,String> formDataMap = new HashMap<String,String>();
			Iterator<String> it = formData.keys();

        	while(it.hasNext()) {
        		String key = it.next();
        		
        		if(formData.get(key).getClass() == JSONArray.class) {
        			for(int i = 0; i < ((JSONArray) formData.get(key)).size(); i++) 
        				formDataMap.putAll(collectFormData(((JSONArray) formData.get(key)).getJSONObject(i)));
        		} else if(formData.get(key).getClass() == JSONObject.class) {
        			formDataMap.putAll(collectFormData((JSONObject) formData.get(key)));
        		} else {
        			if(!StringUtils.isBlank(formData.getString(key))) {
        				formDataMap.put(key,StringUtils.strip(formData.getString(key)));
        				//System.out.println(key + " : " + StringUtils.strip(formData.getString(key)));
        			}
        		}
        	}
        	
        	return formDataMap;
        }
        
        public void setXcodeProjSearchDepth(String searchDepth) {        	
        	try {
        		this.xcodeProjSearchDepth = Integer.parseInt(StringUtils.strip(searchDepth));
        		
        		if(this.xcodeProjSearchDepth < MIN_XCODE_PROJECT_SEARCH_DEPTH ||
        				this.xcodeProjSearchDepth > MAX_XCODE_PROJECT_SEARCH_DEPTH)
        			this.xcodeProjSearchDepth = DEFAULT_XCODE_PROJECT_SEARCH_DEPTH;
        	} catch(NumberFormatException e) {
        		this.xcodeProjSearchDepth = DEFAULT_XCODE_PROJECT_SEARCH_DEPTH;
        	}
        }
        
        public String getXcodeProjSearchDepth() {
    		if(this.xcodeProjSearchDepth < MIN_XCODE_PROJECT_SEARCH_DEPTH ||
    				this.xcodeProjSearchDepth > MAX_XCODE_PROJECT_SEARCH_DEPTH)
    			return String.valueOf(DEFAULT_XCODE_PROJECT_SEARCH_DEPTH);
    		
        	return String.valueOf(this.xcodeProjSearchDepth);
        }
        
        public void setXcodeProjSearchDepthGlobal(boolean searchDepthGlobal) {
        	this.xcodeProjSearchDepthGlobal = searchDepthGlobal;
        }
        
        public boolean getXcodeProjSearchDepthGlobal() {
        	return this.xcodeProjSearchDepthGlobal;
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
        	
        	// to fix a race condition
        	this.currentProjectDir = null;
        }
        
        public FormValidation doCheckXcodeProjSearchDepth(@QueryParameter String value) throws IOException, ServletException {
        	if(StringUtils.isBlank(value))
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckXcodeProjSearchDepth_emptyValue() + " (min " + MIN_XCODE_PROJECT_SEARCH_DEPTH +
        				", max " + MAX_XCODE_PROJECT_SEARCH_DEPTH + ')');
        	
        	int xcodeProjSearchDepth;
        	
        	try {
        		xcodeProjSearchDepth = Integer.parseInt(StringUtils.strip(value));
        	} catch(NumberFormatException e) {
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckXcodeProjSearchDepth_valueNotANumber());
        	}
 
        	if(xcodeProjSearchDepth < MIN_XCODE_PROJECT_SEARCH_DEPTH)
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckXcodeProjSearchDepth_valueTooSmall() + " (min " + MIN_XCODE_PROJECT_SEARCH_DEPTH + ')');
        	else if(xcodeProjSearchDepth > MAX_XCODE_PROJECT_SEARCH_DEPTH)
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckXcodeProjSearchDepth_valueTooBig() + " (max " + MAX_XCODE_PROJECT_SEARCH_DEPTH + ')');
        	
            return FormValidation.ok();
        }
        
        public FormValidation doCheckFilenameTemplate(@QueryParameter String value) throws IOException, ServletException {
        	if(StringUtils.isBlank(value))
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckFilenameTemplate_setDefaultFilename());
        	
            return FormValidation.ok();
        }
        
        public String[] getProjectDirs(FilePath workspace) {
        	if(this.xcodeProjSearchDepth < MIN_XCODE_PROJECT_SEARCH_DEPTH 
        			|| this.xcodeProjSearchDepth > MAX_XCODE_PROJECT_SEARCH_DEPTH)
        		return getProjectDirs(workspace, DEFAULT_XCODE_PROJECT_SEARCH_DEPTH);
        		
        	return getProjectDirs(workspace, this.xcodeProjSearchDepth);
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
				// TODO
			}
			
        	String[] projectDirsArray = new String[projectDirs.size()];
        	
        	for(int i = 0; i < projectDirs.size(); i++) {
        		String path = projectDirs.get(i);
        		
        		// TODO don't use this deprecated method
        		projectDirsArray[i] = path.substring(workspace.toString().length() + 1,path.length());
        	}
        	
        	return projectDirsArray;
        }
        
        public static String[] getBuildConfigurations(FilePath workspace) {
        	return XcodebuildCommandOutputParser.getBuildConfigurations(workspace);
        }
        
        public static String[] getBuildTargets(FilePath workspace) {
        	return XcodebuildCommandOutputParser.getBuildTargets(workspace);
        }
        
        public static String[] getAvailableSdks(FilePath workspace) {
			return XcodebuildCommandOutputParser.getAvailableSdks(workspace);
        }
        
        /* <j:getStatic> doesn't work -> classloader doesn't find needed class */
        public static String getFieldDelimiter() {
        	return FIELD_DELIMITER;
        }
        
        /* <j:getStatic> doesn't work -> classloader doesn't find needed class */
        public static String getUnitTestTargetArg() {
        	return UNIT_TEST_TARGET_ARG;
        }
        
        /* <j:getStatic> doesn't work -> classloader doesn't find needed class */
        public static String getCleanBeforeBuildArg() {
        	return CLEAN_BEFORE_BUILD_ARG;
        }
        
        /* <j:getStatic> doesn't work -> classloader doesn't find needed class */ 
        public static String getCreateIpaArg() {
        	return CREATE_IPA_ARG;
        }
        
        /* <j:getStatic> doesn't work -> classloader doesn't find needed class */
        public static String getArchiveAppArg() {
        	return ARCHIVE_APP_ARG;
        }
    }
}