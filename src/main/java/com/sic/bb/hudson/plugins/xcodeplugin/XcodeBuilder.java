package com.sic.bb.hudson.plugins.xcodeplugin;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
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
import com.sic.bb.hudson.plugins.xcodeplugin.callables.CheckXcodeInstallationCallable;
import com.sic.bb.hudson.plugins.xcodeplugin.callables.IpaPackagerCallable;
import com.sic.bb.hudson.plugins.xcodeplugin.callables.XcodeProjectSearchCallable;
import com.sic.bb.hudson.plugins.xcodeplugin.util.XcodeProjectType;
import com.sic.bb.hudson.plugins.xcodeplugin.util.XcodebuildParser;

public class XcodeBuilder extends Builder {
	private static final String TRUE = "true";
	private static final int RETURN_OK = 0;
	private static final int RETURN_ERROR = 1;
	private static final int MIN_XCODE_PROJECT_SEARCH_DEPTH = 1;
	private static final int MAX_XCODE_PROJECT_SEARCH_DEPTH = 99;
	private static final int DEFAULT_XCODE_PROJECT_SEARCH_DEPTH = 10;
	private static final String DEFAULT_FILENAME_TEMPLATE = "<TARGET>_<CONFIG>_b<BUILD>_<DATETIME>";
	
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
    
    public String getXcodeProjectType() {
    	if(!this.data.containsKey("XcodeProjectType"))
    		return null;
    	
    	return this.data.get("XcodeProjectType");
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
    		if(key.contains(target + '|'))
    			return true;
    	
    	return false;
    }
    
    public String[] getProjectDirs(FilePath workspace) {
    	int searchDepth = MIN_XCODE_PROJECT_SEARCH_DEPTH - 1;
    	
    	try {
    		searchDepth = Integer.parseInt(getXcodeProjSearchDepth());
    	} catch(NumberFormatException e) {}
    	
    	if(searchDepth < MIN_XCODE_PROJECT_SEARCH_DEPTH || searchDepth > MAX_XCODE_PROJECT_SEARCH_DEPTH
    			|| getDescriptor().getXcodeProjSearchDepthGlobal())
    		return getDescriptor().getProjectDirs(workspace);

    	return getDescriptor().getProjectDirs(workspace,searchDepth);
    }
    
    public String[] availableSdks(FilePath workspace) {
    	if(getProjectDir() != null)
    		return getDescriptor().availableSdks(workspace.child(getProjectDir()));
    	
    	return getDescriptor().availableSdks(workspace);
    }

    public String[] getBuildConfigurations(FilePath workspace) {
    	if(getProjectDir() != null)
    		return getDescriptor().getBuildConfigurations(workspace.child(getProjectDir()));

    	return getDescriptor().getBuildConfigurations(workspace);
    }
    
    public String[] getBuildTargets(FilePath workspace) {
    	if(getProjectDir() != null)
    		return getDescriptor().getBuildTargets(workspace.child(getProjectDir()));

    	return getDescriptor().getBuildTargets(workspace);
    }
    
    @Override
    public XcodeBuilderDescriptor getDescriptor() {
        return (XcodeBuilderDescriptor) super.getDescriptor();
    }
    
    @Override
    public boolean prebuild(AbstractBuild<?,?> build, BuildListener listener) {
		Computer curComputer = Computer.currentComputer();
		FilePath workspace = build.getWorkspace();
		
		// to empty cache of xcodebuildparser
        getDescriptor().getXcodebuildParser().setWorkspaceTemp(workspace.getParent().getName());
		
		if(!curComputer.getNode().createLauncher(listener).isUnix()) {
			// TODO see also XcodebuildParser, there's a launcher too
			listener.fatalError(Messages.XcodeBuilder_prebuild_unixOnly());
			return false;
		}
		
        if(getProjectDir() != null)
			workspace = workspace.child(getProjectDir());
		
		try {
			if(!new FilePath(workspace.getChannel(), CheckXcodeInstallationCallable.XCODEBUILD_COMMAND).exists()) {
				listener.fatalError(Messages.XcodeBuilder_prebuild_xcodebuildNotFound()+ ": " + CheckXcodeInstallationCallable.XCODEBUILD_COMMAND);
				return false;
			}
			
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
		List<Integer> returnCodes = new ArrayList<Integer>();
		
        int rcode = RETURN_OK;
        
        logger.println(Messages.XcodeBuilder_perform_started());
        
        try {
        	EnvVars envs = build.getEnvironment(listener);
			
			// <cleanup>
			
			if(!(descr.getCleanBeforeBuildGlobal() && !descr.getCleanBeforeBuild())) {
				for(String toClean: getToPerformStep("clean_before_build",(descr.getCleanBeforeBuildGlobal() && descr.getCleanBeforeBuild())))
					returnCodes.add(launcher.launch().envs(envs).stdout(listener).pwd(workspace).
							cmds(createCmds(toClean,"clean")).join());
			}
			
			// </cleanup>
			
			// <build>

			// TODO: plaintext password will be logged
			rcode = launcher.launch().envs(envs).pwd(workspace).
							cmds(CheckXcodeInstallationCallable.SECURITY_COMMAND,"unlock-keychain","-p",this.currentPassword,
									CheckXcodeInstallationCallable.getKeychain(this.currentUsername)).join();
		
			if(rcode != RETURN_OK) {
				listener.fatalError(Messages.XcodeBuilder_perform_keychainNotUnlockable());
				return false;
			}
			
			for(String toBuild: getToPerformStep("build",true)) {
				rcode = launcher.launch().envs(envs).stdout(listener).pwd(workspace).
						cmds(createCmds(toBuild,"build")).join();
				
				if(rcode != RETURN_OK)
					blackList.add(toBuild);
				
				returnCodes.add(rcode);
			}		

			// TODO: plaintext password will be logged
			rcode = launcher.launch().envs(envs).pwd(workspace).
							cmds(CheckXcodeInstallationCallable.SECURITY_COMMAND,"lock-keychain",
									CheckXcodeInstallationCallable.getKeychain(this.currentUsername)).join();
			
			if(rcode != RETURN_OK) {
				listener.fatalError(Messages.XcodeBuilder_perform_keychainNotLockable());
				return false;
			}
			
			// </build>
			
			
			FilePath buildDir = workspace.child("build");
			
			// <archive app>
			
			if(!(descr.getArchiveAppGlobal() && !descr.getArchiveApp())) {	
				for(String toArchiveApp: getToPerformStep("archive_app",(descr.getArchiveAppGlobal() && descr.getArchiveApp()))) {
					if(blackList.contains(toArchiveApp)) {
						returnCodes.add(RETURN_ERROR);
						continue;
					}
						
					String[] array = toArchiveApp.split("\\|");
					
					String configBuildDirName = XcodeProjectType.getProjectBuildDirName(getXcodeProjectType(), array[1]);
					
					if(configBuildDirName == null || !buildDir.child(configBuildDirName).isDirectory()) {
						returnCodes.add(RETURN_ERROR);
						continue;
					}
					
					FilePath tempBuildDir = buildDir.child(configBuildDirName);

					if(tempBuildDir.act(new AppArchiverCallable(array[0], createFilename(build, array[0], array[1]))))
						returnCodes.add(RETURN_OK);
					else
						returnCodes.add(RETURN_ERROR);
				}
			}
			
			// </archive app>
			
			// <create ipa>
			
			if(!(descr.getCreateIpaGlobal() && !descr.getCreateIpa())) {	
				for(String toCreateIpa: getToPerformStep("create_ipa",(descr.getCreateIpaGlobal() && descr.getCreateIpa()))) {
					if(blackList.contains(toCreateIpa)) {
						returnCodes.add(RETURN_ERROR);
						continue;
					}
						
					String[] array = toCreateIpa.split("\\|");
					
					String configBuildDirName = XcodeProjectType.getProjectBuildDirName(getXcodeProjectType(), array[1]);
					
					if(configBuildDirName == null || !buildDir.child(configBuildDirName).isDirectory()) {
						returnCodes.add(RETURN_ERROR);
						continue;
					}
					
					FilePath tempBuildDir = buildDir.child(configBuildDirName);
					
					if(tempBuildDir.act(new IpaPackagerCallable(array[0], createFilename(build, array[0], array[1]))))
						returnCodes.add(RETURN_OK);
					else
						returnCodes.add(RETURN_ERROR);
				}
			}
			
			// </create ipa>
			
			if(returnCodes.contains(RETURN_ERROR))
				return false;
			
			return true;
		} catch (Exception e) {
			logger.println(e.getStackTrace());
		}
        
        return false;
    }
    
    private Set<String> getToPerformStep(String cmd, boolean force) {
    	String[] keys = (String[]) this.data.keySet().toArray(new String[this.data.size()]);
    	Set<String> toPerformStep = new HashSet<String>();
    	
    	for(String key: keys) {
			if(!key.contains("|"))
				continue;
			
			String[] fields = key.split("\\|");
			
			if(!cmd.equals("build") && (!fields[fields.length - 1].equals(cmd) || (!force && !this.data.get(key).equals(TRUE))))
				continue;
			
			toPerformStep.add(fields[0] + '|' + fields[1]);
		}
    	
    	return toPerformStep;
    }
    
    private static List<String> createCmds(String arg, String cmd) {
    	List<String> cmds = new ArrayList<String>();
    	String[] args = arg.split("\\|");
		
		cmds.add(CheckXcodeInstallationCallable.XCODEBUILD_COMMAND);
		cmds.add("-target");
		cmds.add(args[0]);
		cmds.add("-configuration");
		cmds.add(args[1]);
		cmds.add(cmd);
		
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
    	private transient XcodebuildParser xcodebuildParser; 	
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
        	
        	if(this.xcodebuildParser == null)
            	this.xcodebuildParser = new XcodebuildParser();
        	
        	if(this.projectWorkspaceMap == null)
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
        
        public XcodebuildParser getXcodebuildParser() {
        	return this.xcodebuildParser;
        }
        
        public void setXcodeProjSearchDepth(String searchDepth) {        	
        	try {
        		this.xcodeProjSearchDepth = Integer.parseInt(StringUtils.strip(searchDepth));
        		
        		if(this.xcodeProjSearchDepth < XcodeBuilder.MIN_XCODE_PROJECT_SEARCH_DEPTH ||
        				this.xcodeProjSearchDepth > XcodeBuilder.MAX_XCODE_PROJECT_SEARCH_DEPTH)
        			this.xcodeProjSearchDepth = XcodeBuilder.DEFAULT_XCODE_PROJECT_SEARCH_DEPTH;
        	} catch(NumberFormatException e) {
        		this.xcodeProjSearchDepth = XcodeBuilder.DEFAULT_XCODE_PROJECT_SEARCH_DEPTH;
        	}
        }
        
        public String getXcodeProjSearchDepth() {
    		if(this.xcodeProjSearchDepth < XcodeBuilder.MIN_XCODE_PROJECT_SEARCH_DEPTH ||
    				this.xcodeProjSearchDepth > XcodeBuilder.MAX_XCODE_PROJECT_SEARCH_DEPTH)
    			return String.valueOf(XcodeBuilder.DEFAULT_XCODE_PROJECT_SEARCH_DEPTH);
    		
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
        		this.filenameTemplate = XcodeBuilder.DEFAULT_FILENAME_TEMPLATE;
        	else
        		this.filenameTemplate = StringUtils.strip(filenameTemplate);
        }
        
        public String getFilenameTemplate() {
        	if(StringUtils.isBlank(this.filenameTemplate))
        		return XcodeBuilder.DEFAULT_FILENAME_TEMPLATE;
        	
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
        
        public String[] getXcodeProjectTypes() {
        	return XcodeProjectType.ProjectTypes;
        }
        
        public String getXcodeProjectType() {
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
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckXcodeProjSearchDepth_emptyValue() + " (min " + XcodeBuilder.MIN_XCODE_PROJECT_SEARCH_DEPTH +
        				", max " + XcodeBuilder.MAX_XCODE_PROJECT_SEARCH_DEPTH + ')');
        	
        	int xcodeProjSearchDepth;
        	
        	try {
        		xcodeProjSearchDepth = Integer.parseInt(StringUtils.strip(value));
        	} catch(NumberFormatException e) {
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckXcodeProjSearchDepth_valueNotANumber());
        	}
 
        	if(xcodeProjSearchDepth < XcodeBuilder.MIN_XCODE_PROJECT_SEARCH_DEPTH)
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckXcodeProjSearchDepth_valueTooSmall() + " (min " + XcodeBuilder.MIN_XCODE_PROJECT_SEARCH_DEPTH + ')');
        	else if(xcodeProjSearchDepth > XcodeBuilder.MAX_XCODE_PROJECT_SEARCH_DEPTH)
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckXcodeProjSearchDepth_valueTooBig() + " (max " + XcodeBuilder.MAX_XCODE_PROJECT_SEARCH_DEPTH + ')');
        	
            return FormValidation.ok();
        }
        
        public FormValidation doCheckFilenameTemplate(@QueryParameter String value) throws IOException, ServletException {
        	if(StringUtils.isBlank(value))
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckFilenameTemplate_setDefaultFilename());
        	
            return FormValidation.ok();
        }
        
        public String[] getProjectDirs(FilePath workspace) {
        	if(this.xcodeProjSearchDepth < XcodeBuilder.MIN_XCODE_PROJECT_SEARCH_DEPTH 
        			|| this.xcodeProjSearchDepth > XcodeBuilder.MAX_XCODE_PROJECT_SEARCH_DEPTH)
        		return getProjectDirs(workspace, XcodeBuilder.DEFAULT_XCODE_PROJECT_SEARCH_DEPTH);
        		
        	return getProjectDirs(workspace, this.xcodeProjSearchDepth);
        }
        
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
        
        public String[] getBuildConfigurations(FilePath workspace) {
        	return this.xcodebuildParser.getBuildConfigurations(workspace);
        }
        
        public String[] getBuildTargets(FilePath workspace) {
        	return this.xcodebuildParser.getBuildTargets(workspace);
        }
        
        public String[] availableSdks(FilePath workspace) {
			return this.xcodebuildParser.getAvailableSdks(workspace);
        }
    }
}