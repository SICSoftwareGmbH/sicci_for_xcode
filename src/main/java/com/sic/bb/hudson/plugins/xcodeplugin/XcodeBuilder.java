package com.sic.bb.hudson.plugins.xcodeplugin;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.StreamTaskListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class XcodeBuilder extends Builder {
	private final static int BUILD_OK = 0;
	private final static int BUILD_ERROR = 1;
	private final static int MIN_XCODE_PROJ_SEARCH_DEPTH = 1;
	private final static int MAX_XCODE_PROJ_SEARCH_DEPTH = 99;
	private final static int DEFAULT_XCODE_PROJ_SEARCH_DEPTH = 10;
	private final static String DEFAULT_XCODEBUILD_PATH = "/usr/bin/xcodebuild";
	private final static String DEFAULT_IPA_FILENAME_TEMPLATE = "<TARGET>_<CONFIG>_b<BUILD>_<DATETIME>";
	
    private final Map<String,String> data;

    @DataBoundConstructor
    public XcodeBuilder(Map<String,String> data) {
    	this.data = data;
    }
    
    public String getProjectDir() {
    	if(!this.data.containsKey("ProjectDir"))
    		return null;
    	
    	return this.data.get("ProjectDir");
    }
    
    public String getIpaFilenameTemplate() {
    	if(!this.data.containsKey("IpaFilenameTemplate"))
    		return null;
    	
    	return this.data.get("IpaFilenameTemplate");
    }
    
    public boolean getXcodeClean() {
    	if(!this.data.containsKey("XcodeClean"))
    		return false;
    	
    	return this.data.get("XcodeClean").equals("true");
    }
    
    public String getXcodeProjSearchDepth() {
    	if(!this.data.containsKey("XcodeProjSearchDepth"))
    		return null;
    	
    	return this.data.get("XcodeProjSearchDepth");
    }
    
    public boolean getBooleanPreference(String key) {
    	if(!this.data.containsKey(key))
    		return false;
    	
    	return this.data.get(key).equals("true");
    }
    
    public boolean subMenuUsed(String target) {
    	for(String key: (String[]) this.data.keySet().toArray(new String[this.data.size()])) {
    		if(key.contains(target + '|'))
    			return true;
    	}
    	
    	return false;
    }
    
    public String[] getProjectDirs(String workspace) {
    	int searchDepth = MIN_XCODE_PROJ_SEARCH_DEPTH - 1;
    	
    	try {
    		searchDepth = Integer.parseInt(getXcodeProjSearchDepth());
    	} catch(NumberFormatException e) {
    		// TODO
    	}
    	
    	if(searchDepth < MIN_XCODE_PROJ_SEARCH_DEPTH || searchDepth > MAX_XCODE_PROJ_SEARCH_DEPTH)
    		return getDescriptor().getProjectDirs(workspace);
    	else
    		return getDescriptor().getProjectDirs(workspace,searchDepth);
    }
    
    public String[] availableSdks(String workspace) {
    	if(getProjectDir() != null)
    		return getDescriptor().availableSdks(workspace + '/' + getProjectDir());
    	else
    		return getDescriptor().availableSdks(workspace);
    }

    public String[] getBuildConfigurations(String workspace) {
    	if(getProjectDir() != null)
    		return getDescriptor().getBuildConfigurations(workspace + '/' + getProjectDir());
    	else
    		return getDescriptor().getBuildConfigurations(workspace);
    }
    
    public String[] getBuildTargets(String workspace) {
    	if(getProjectDir() != null)
    		return getDescriptor().getBuildTargets(workspace + '/' + getProjectDir());
    	else
    		return getDescriptor().getBuildTargets(workspace);
    }
    
    @Override
    public XcodeBuilderDescriptor getDescriptor() {
        return (XcodeBuilderDescriptor) super.getDescriptor();
    }
    
    /*
    @Override
    public boolean prebuild(AbstractBuild<?,?> build, BuildListener listener) {
    	listener.getLogger().println("FAILED PREBUILD");
    	return false;
    }
    */
    
    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) {
        listener.getLogger().println(Messages.XcodeBuilder_perform_started());
		
        String xcodebuild = getDescriptor().getXcodebuild();
        FilePath workspace = build.getWorkspace();
        
        if(getProjectDir() != null)
			workspace = workspace.child(getProjectDir());
        
        try {
        	EnvVars envs = build.getEnvironment(listener);
        	
			if(!new FilePath(workspace.getChannel(), xcodebuild).exists()) {
				listener.fatalError(Messages.XcodeBuilder_perform_xcodebuildNotFound() + ": " + xcodebuild);
				return false;
			}
			
			if(!workspace.exists()) {
				listener.fatalError(Messages.XcodeBuilder_perform_projectDirNotFound() + ": " + workspace);
				return false;
			}
			
			List<Integer> returnCodes = new ArrayList<Integer>();
			
			// cleanup
			if(getXcodeClean())
				for(String toClean: getToPerformStep("clean"))
					returnCodes.add(launcher.launch().envs(envs).stdout(listener).pwd(workspace).
							cmds(createCmds(xcodebuild,toClean,"clean")).join());
			else {
				for(String toClean: getToPerformStep("clean")) {
					FilePath buildDir = workspace.child("build");
					String[] array = toClean.split("\\|");
					
					List<FilePath> buildDirs = buildDir.list(new BuildDirFilter());
					
					if(buildDirs != null) {
						for(FilePath dir: buildDirs) {
							dir = dir.child(array[1] + "-iphoneos").child(array[0] + ".build");
							
							if(dir.isDirectory())
								dir.deleteRecursive();
						}
					}
					
					buildDir = buildDir.child(array[1] + "-iphoneos");
					
					if(buildDir.child(array[0] + ".app").isDirectory())
						buildDir.child(array[0] + ".app").deleteRecursive();
					
					if(buildDir.child(array[0] + ".app.dSYM").isDirectory())
						buildDir.child(array[0] + ".app.dSYM").deleteRecursive();
				}
			}
			
			
			ArrayList<String> blackList =  new ArrayList<String>();
			
			// build
			for(String toBuild: getToPerformStep("build")) {
				int rcode = launcher.launch().envs(envs).stdout(listener).pwd(workspace).
						cmds(createCmds(xcodebuild,toBuild,"build")).join();
				
				if(rcode != BUILD_OK)
					blackList.add(toBuild);
				
				returnCodes.add(rcode);
			}
			
			
			// create ipa
			for(String toCreateIPA: getToPerformStep("ipa")) {
				if(blackList.contains(toCreateIPA)) {
					returnCodes.add(BUILD_ERROR);
					continue;
				}
					
				FilePath buildDir = workspace.child("build");
				String[] array = toCreateIPA.split("\\|");
				
				if(buildDir.child(array[1]).isDirectory())
					buildDir = buildDir.child(array[1]);
				else if(buildDir.child(array[1] + "-iphoneos").isDirectory())
					buildDir = buildDir.child(array[1] + "-iphoneos");
				else {
					returnCodes.add(BUILD_ERROR);
					continue;
				}
				
	            for(FilePath app: buildDir.list(new AppDirFilter())) {
	            	if(!app.getBaseName().equals(array[0]))
	            		continue;
	            	            	
	            	FilePath ipa = buildDir.child(createIPAFilename(build, app.getBaseName(), array[1]));
	                
	                if(ipa.exists())
	                	ipa.delete();

	                FilePath payload = buildDir.child("Payload");
	                
	                if(payload.exists())
	                	payload.deleteRecursive();
	                
	                payload.mkdirs();
	                app.renameTo(payload.child(app.getName()));
	                
	                ZipArchiveOutputStream zipStream = new ZipArchiveOutputStream(ipa.write());
	                zipDirectory(payload,"",zipStream);
	                zipStream.close();
	                
	                payload.child(app.getName()).renameTo(buildDir.child(app.getName()));
	                
	                payload.deleteRecursive();
	            }
			}
			
			if(returnCodes.contains(BUILD_ERROR))
				return false;
			else
				return true;
			
		} catch (IOException e) {
			// TODO
			listener.getLogger().println("IOException:" + e.getMessage());
		} catch (InterruptedException e) {
			// TODO
			listener.getLogger().println("InterruptedException: " + e.getMessage());
		}
        
        return false;
    }
    
    private Set<String> getToPerformStep(String cmd) {
    	String[] keys = (String[]) this.data.keySet().toArray(new String[this.data.size()]);
    	Set<String> toPerformStep = new HashSet<String>();
    	
    	for(String key: keys) {
			if(!key.contains("|"))
				continue;
			
			String[] fields = key.split("\\|");
			
			if(!cmd.equals("build") && (!fields[fields.length - 1].equals(cmd) || !this.data.get(key).equals("true")))
				continue;
			
			toPerformStep.add(fields[0] + '|' + fields[1]);
		}
    	
    	return toPerformStep;
    }
    
    private static List<String> createCmds(String xcodebuild, String arg, String cmd) {
    	List<String> cmds = new ArrayList<String>();
    	String[] args = arg.split("\\|");
		
		cmds.add(xcodebuild);
		cmds.add("-target");
		cmds.add(args[0]);
		cmds.add("-configuration");
		cmds.add(args[1]);
		cmds.add(cmd);
		
		return cmds;
    }
    
    private static void zipDirectory(FilePath directory, String path, ZipArchiveOutputStream zipStream) {
    	if(path != null && !path.isEmpty())
    		path += '/';
    		
		try {
	    	ZipArchiveEntry zipEntry = new ZipArchiveEntry(new File(directory.toURI()),path + directory.getName());
	    	zipEntry.setUnixMode(directory.mode());
	    	zipStream.putArchiveEntry(zipEntry);
	    	
	    	if(!directory.isDirectory()) {
	    		directory.copyTo(zipStream);
	    		zipStream.closeArchiveEntry();
			} else {
				zipStream.closeArchiveEntry();
				
		    	List<FilePath> entries = directory.list();
		    		
		    	if(entries != null)
		    		for(FilePath entry: entries)
		    			zipDirectory(entry,path + directory.getName(),zipStream);
			}
		} catch(InterruptedException e) {
			// TODO
		} catch(IOException e) {
			// TODO
		}
    }
    
    private String createIPAFilename(AbstractBuild<?,?> build, String targetName, String configName) {
    	String ipaFilename = getIpaFilenameTemplate();
    	
    	if(ipaFilename.isEmpty())
    		ipaFilename = DEFAULT_IPA_FILENAME_TEMPLATE;
    	
    	Date buildTimeStamp = build.getTimestamp().getTime();
    	
    	ipaFilename = ipaFilename.replaceAll("<SECOND>",new SimpleDateFormat("ss").format(buildTimeStamp));
    	ipaFilename = ipaFilename.replaceAll("<MINUTE>",new SimpleDateFormat("mm").format(buildTimeStamp));
    	ipaFilename = ipaFilename.replaceAll("<HOUR>",new SimpleDateFormat("HH").format(buildTimeStamp));
    	ipaFilename = ipaFilename.replaceAll("<DAY>",new SimpleDateFormat("dd").format(buildTimeStamp));
    	ipaFilename = ipaFilename.replaceAll("<MONTH>",new SimpleDateFormat("MM").format(buildTimeStamp));
    	ipaFilename = ipaFilename.replaceAll("<YEAR>",new SimpleDateFormat("yyyy").format(buildTimeStamp));
    	ipaFilename = ipaFilename.replaceAll("<TIME>",new SimpleDateFormat("HH_mm_ss").format(buildTimeStamp));
    	ipaFilename = ipaFilename.replaceAll("<DATE>",new SimpleDateFormat("yyyy_MM_dd").format(buildTimeStamp));
    	ipaFilename = ipaFilename.replaceAll("<DATETIME>",new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss").format(buildTimeStamp));
    	ipaFilename = ipaFilename.replaceAll("<BUILD>",String.valueOf(build.getNumber()));
    	ipaFilename = ipaFilename.replaceAll("<TARGET>",targetName);
    	ipaFilename = ipaFilename.replaceAll("<CONFIG>",configName);
    	ipaFilename += ".ipa";
    	
    	return ipaFilename;
    }
    
    @Extension
    public static final class XcodeBuilderDescriptor extends BuildStepDescriptor<Builder> {
    	private static final Pattern availableSdksPattern = Pattern.compile("^.*(?:-sdk\\s*)(\\S+)\\s*$");
        private static final Pattern parseXcodeBuildListPattern1 = Pattern.compile("^\\s*((?:[^(\\s]+\\s*)+).*$");
        private static final Pattern parseXcodeBuildListPattern2 = Pattern.compile("^\\s*((?:\\S+\\s*\\S+)+)\\s*$");
        
    	private String currentProjectDir, workspaceTemp, xcodebuildOutputTemp;
    	
    	private String xcodebuild;
    	private String ipaFilenameTemplate;
        private boolean ipaFilenameTemplateGlobal;
        private boolean xcodeClean, xcodeCleanGlobal;
        private int xcodeProjSearchDepth;
        private boolean xcodeProjSearchDepthGlobal;
        private boolean cleanBeforeBuild, cleanBeforeBuildGlobal;
        private boolean createIpa, createIpaGlobal;
        
        public XcodeBuilderDescriptor() {
        	super(XcodeBuilder.class);
        	load();
        }
        
        public String getXcodebuild() {
        	if(this.xcodebuild == null || this.xcodebuild.isEmpty())
        		return XcodeBuilder.DEFAULT_XCODEBUILD_PATH;
        	
            return this.xcodebuild;
        }
        
        public void setXcodebuild(String xcodebuild) {
        	if(xcodebuild == null || xcodebuild.isEmpty())
        		this.xcodebuild = XcodeBuilder.DEFAULT_XCODEBUILD_PATH;
        	
        	this.xcodebuild = xcodebuild;
        }
        
        public void setXcodeClean(boolean xcodeClean) {
        	this.xcodeClean = xcodeClean;
        }
        
        public boolean getXcodeClean() {
            return this.xcodeClean;
        }

        public void setXcodeCleanGlobal(boolean xcodeCleanGlobal) {
        	this.xcodeCleanGlobal = xcodeCleanGlobal;
        }
        
        public boolean getXcodeCleanGlobal() {
        	return this.xcodeCleanGlobal;
        }
        
        public void setXcodeProjSearchDepth(String searchDepth) {        	
        	try {
        		this.xcodeProjSearchDepth = Integer.parseInt(searchDepth);
        		
        		if(this.xcodeProjSearchDepth < XcodeBuilder.MIN_XCODE_PROJ_SEARCH_DEPTH ||
        				this.xcodeProjSearchDepth > XcodeBuilder.MAX_XCODE_PROJ_SEARCH_DEPTH)
        			this.xcodeProjSearchDepth = XcodeBuilder.DEFAULT_XCODE_PROJ_SEARCH_DEPTH;
        	} catch(NumberFormatException e) {
        		this.xcodeProjSearchDepth = XcodeBuilder.DEFAULT_XCODE_PROJ_SEARCH_DEPTH;
        	}
        }
        
        public String getXcodeProjSearchDepth() {
    		if(this.xcodeProjSearchDepth < XcodeBuilder.MIN_XCODE_PROJ_SEARCH_DEPTH ||
    				this.xcodeProjSearchDepth > XcodeBuilder.MAX_XCODE_PROJ_SEARCH_DEPTH)
    			return String.valueOf(XcodeBuilder.DEFAULT_XCODE_PROJ_SEARCH_DEPTH);
    		
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
        
        public void setIpaFilenameTemplate(String ipaFilenameTemplate) {
        	if(ipaFilenameTemplate == null || ipaFilenameTemplate.isEmpty())
        		this.ipaFilenameTemplate = XcodeBuilder.DEFAULT_IPA_FILENAME_TEMPLATE;
        	
        	this.ipaFilenameTemplate = ipaFilenameTemplate;
        }
        
        public String getIpaFilenameTemplate() {
        	if(this.ipaFilenameTemplate == null || this.ipaFilenameTemplate.isEmpty())
        		return XcodeBuilder.DEFAULT_IPA_FILENAME_TEMPLATE;
        	
        	return this.ipaFilenameTemplate;
        }
        
        public void setIpaFilenameTemplateGlobal(boolean ipaFilenameTemplateGlobal) {
        	this.ipaFilenameTemplateGlobal = ipaFilenameTemplateGlobal;
        }
        
        public boolean getIpaFilenameTemplateGlobal() {
        	return this.ipaFilenameTemplateGlobal;
        } 
        
        public String getProjectDir() {
        	return this.currentProjectDir;
        }
        
        public boolean subMenuUsed(String target) {
        	return false;
        }
        
        public boolean getBooleanPreference(String key) {
        	return false;
        }
        
        public String getDisplayName() {
            return Messages.XcodeBuilderDescriptor_getDisplayName();
        }
        
        @Override
        public XcodeBuilder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
        	return new XcodeBuilder(collectFormData(formData));
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
        			if(!formData.getString(key).isEmpty()) {
        				formDataMap.put(key,formData.getString(key));
        				//System.out.println(key + " : " + formData.getString(key));
        			}
        		}
        	}
        	
        	return formDataMap;
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req,formData);
        }
        
        public void doAjaxTargets(StaplerRequest req, StaplerResponse rsp, @QueryParameter String projectDir) throws IOException, ServletException {
        	this.currentProjectDir = projectDir;
        	req.getView(this,'/' + XcodeBuilder.class.getName().replaceAll("\\.","\\/")  + "/targets.jelly").forward(req, rsp);
        }
        
        public FormValidation doCheckXcodebuild(@QueryParameter String value) throws IOException, ServletException {
        	if(value.isEmpty())
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckXcodebuild_emptyValue());
        	
        	try {
        		FilePath xcodebuild = new FilePath(new File(value));
        		
        		if(!xcodebuild.exists() || xcodebuild.isDirectory())
        			return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckXcodebuild_fileNotExists());
        	} catch(InterruptedException e) {
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckXcodebuild_fileNotExists());
        	}
        	
            if(!value.contains("xcodebuild"))
            	return FormValidation.warningWithMarkup(Messages.XcodeBuilderDescriptor_doCheckXcodebuild_valueNotContainingXcodebuild());
            
            return FormValidation.ok();
        }
        
        public FormValidation doCheckXcodeProjSearchDepth(@QueryParameter String value) throws IOException, ServletException {
        	if(value.isEmpty())
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckXcodeProjSearchDepth_emptyValue() + " (min " + XcodeBuilder.MIN_XCODE_PROJ_SEARCH_DEPTH +
        				", max " + XcodeBuilder.MAX_XCODE_PROJ_SEARCH_DEPTH + ')');
        	
        	int xcodeProjSearchDepth;
        	
        	try {
        		xcodeProjSearchDepth = Integer.parseInt(value);
        	} catch(NumberFormatException e) {
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckXcodeProjSearchDepth_valueNotANumber());
        	}
 
        	if(xcodeProjSearchDepth < XcodeBuilder.MIN_XCODE_PROJ_SEARCH_DEPTH)
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckXcodeProjSearchDepth_valueTooSmall() + " (min " + XcodeBuilder.MIN_XCODE_PROJ_SEARCH_DEPTH + ')');
        	else if(xcodeProjSearchDepth > XcodeBuilder.MAX_XCODE_PROJ_SEARCH_DEPTH)
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckXcodeProjSearchDepth_valueTooBig() + " (max " + XcodeBuilder.MAX_XCODE_PROJ_SEARCH_DEPTH + ')');
        	
            return FormValidation.ok();
        }
        
        public FormValidation doCheckIpaFilenameTemplate(@QueryParameter String value) throws IOException, ServletException {
        	if(value.isEmpty())
        		return FormValidation.error(Messages.XcodeBuilderDescriptor_doCheckIpaFilenameTemplate_setDefaultIpaFilename());
        	
            return FormValidation.ok();
        }

        @SuppressWarnings("rawtypes")
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
        	if(aClass == FreeStyleProject.class)
        		return true;
        	
            return false;
        }
        

        public String[] getProjectDirs(String workspace) {
        	if(this.xcodeProjSearchDepth < XcodeBuilder.MIN_XCODE_PROJ_SEARCH_DEPTH 
        			|| this.xcodeProjSearchDepth > XcodeBuilder.MAX_XCODE_PROJ_SEARCH_DEPTH)
        		return getProjectDirs(workspace, XcodeBuilder.DEFAULT_XCODE_PROJ_SEARCH_DEPTH);
        		
        	return getProjectDirs(workspace, this.xcodeProjSearchDepth);
        }
        
        
        public String[] getProjectDirs(String workspace, int searchDepth) {
        	ArrayList<String> projectDirs = searchXcodeProjFiles(workspace, searchDepth);
        	String[] projectDirsArray = new String[projectDirs.size()];
        	
        	for(int i = 0; i < projectDirs.size(); i++) {
        		String path = projectDirs.get(i);
        		
        		projectDirsArray[i] = path.substring(workspace.length() + 1,path.length());
        	}
        	
        	return projectDirsArray;
        }
        
        private ArrayList<String> searchXcodeProjFiles(String workspace, int searchDepth) {
        	ArrayList<String> projectDirs = new ArrayList<String>();
        	
        	if(searchDepth <= 0)
        		return projectDirs;
        	
        	FilePath dir = new FilePath(new File(workspace));
        	
        	try {
    			if(dir.list(new XcodeProjDirFilter()).size() != 0)
    				projectDirs.add(workspace + '/');
        		
				for(FilePath path : dir.listDirectories()) {
					if(!projectDirs.contains(workspace + '/' + path.getName()))
						projectDirs.addAll(searchXcodeProjFiles(workspace + '/' + path.getName(), searchDepth - 1));
				}
			} catch (IOException e) {
				// TODO
				//projectDirs.add("IOException: " + e.getMessage());
			} catch (InterruptedException e) {
				// TODO
				//projectDirs.add("InterruptedException: " + e.getMessage());
			}
			
			return projectDirs;
        }
        
        public String[] getBuildConfigurations(String workspace) {
        	return parseXcodebuildList(workspace, "Build Configurations:");
        }
        
        public String[] getBuildTargets(String workspace) {
        	return parseXcodebuildList(workspace, "Targets:");
        }
        
        public String[] availableSdks(String workspace) {
			ArrayList<String> sdks = new ArrayList<String>();
			
			for(String sdk: callXcodebuild(workspace,"-showsdks").toString().split("\n")) {
				if(!sdk.contains("-sdk"))
					continue;
				
				sdks.add(availableSdksPattern.matcher(sdk).replaceAll("$1"));
			}
        
			return (String[]) sdks.toArray(new String[sdks.size()]);
        }
        
        private String[] parseXcodebuildList(String workspace, String arg) {
			ArrayList<String> items = new ArrayList<String>();
			boolean found = false;
			
			for(String item: callXcodebuild(workspace,"-list").toString().split("\n")) {
				if(item.contains(arg)) {
					found = true;
					continue;
				}
					
				if(!found) continue;
				if(item.isEmpty()) break;
				
				item = parseXcodeBuildListPattern1.matcher(item).replaceAll("$1");
				items.add(parseXcodeBuildListPattern2.matcher(item).replaceAll("$1"));
			}
        
			return (String[]) items.toArray(new String[items.size()]);
        }
        
        private String callXcodebuild(String workspace, String arg) {
        	if(this.workspaceTemp != null && this.workspaceTemp.equals(workspace + arg))
        		return this.xcodebuildOutputTemp;
        	else
        		this.workspaceTemp = workspace + arg;
        	
	    	FilePath file = new FilePath(new File(this.xcodebuild));
	    	ByteArrayOutputStream stdout = new ByteArrayOutputStream();
	    	
	    	try {
	    		Launcher launcher = file.createLauncher(new StreamTaskListener(new ByteArrayOutputStream()));
	    		launcher.launch().stdout(stdout).pwd(workspace).cmds(this.xcodebuild, arg).join();
			} catch (IOException e) {
				// TODO
				return "IOException: " + e.getMessage();
			} catch (InterruptedException e) {
				// TODO
				return "InterruptedException: " + e.getMessage();
			}
			
			this.xcodebuildOutputTemp = stdout.toString();
			
			return this.xcodebuildOutputTemp;
        }
        
        @SuppressWarnings("serial")
    	private final class XcodeProjDirFilter implements FileFilter,Serializable {
            public boolean accept(File pathname) {
                return pathname.isDirectory() && pathname.getName().endsWith(".xcodeproj");
            }
        }
    }
    
    @SuppressWarnings("serial")
	private final class AppDirFilter implements FileFilter,Serializable {
        public boolean accept(File pathname) {
            return pathname.isDirectory() && pathname.getName().endsWith(".app");
        }
    }
    
    @SuppressWarnings("serial")
	private final class BuildDirFilter implements FileFilter,Serializable {
        public boolean accept(File pathname) {
            return pathname.isDirectory() && pathname.getName().endsWith(".build");
        }
    }
    
    /*
    @SuppressWarnings("serial")
	private final class DsymDirFilter implements FileFilter,Serializable {
        public boolean accept(File pathname) {
            return pathname.isDirectory() && pathname.getName().endsWith(".app.dSYM");
        }
    }
    
    @SuppressWarnings("serial")
	private final class IpaFileFilter implements FileFilter,Serializable {
        public boolean accept(File pathname) {
            return pathname.isDirectory() && pathname.getName().endsWith(".ipa");
        }
    }
    */
}