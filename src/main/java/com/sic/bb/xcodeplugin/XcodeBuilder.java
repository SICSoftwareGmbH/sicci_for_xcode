package com.sic.bb.xcodeplugin;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.StreamTaskListener;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerResponse;


import javax.servlet.ServletException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class XcodeBuilder extends Builder {
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
    
    public String getIPAFilename() {
    	if(!this.data.containsKey("IPAfilename"))
    		return null;
    	
    	return this.data.get("IPAfilename");
    }
    
    public String getXcodeClean() {
    	if(!this.data.containsKey("XcodeClean"))
    		return null;
    	
    	return this.data.get("XcodeClean");
    }
    
    public boolean getBooleanPreference(String key) {
    	if(!this.data.containsKey(key))
    		return false;
    	
    	return this.data.get(key).equals("true");
    }
    
    public boolean subMenuUsed(String target) {
    	for(String key: (String[]) this.data.keySet().toArray(new String[this.data.size()])) {
    		if(key.contains(target + "|"))
    			return true;
    	}
    	
    	return false;
    }
    
    public String[] availableSdks(String workspace) {
    	if(getProjectDir() != null)
    		return getDescriptor().availableSdks(workspace + "/" + getProjectDir());
    	else
    		return getDescriptor().availableSdks(workspace);
    }

    public String[] getBuildConfigurations(String workspace) {
    	if(getProjectDir() != null)
    		return getDescriptor().getBuildConfigurations(workspace + "/" + getProjectDir());
    	else
    		return getDescriptor().getBuildConfigurations(workspace);
    }
    
    public String[] getBuildTargets(String workspace) {
    	if(getProjectDir() != null)
    		return getDescriptor().getBuildTargets(workspace + "/" + getProjectDir());
    	else
    		return getDescriptor().getBuildTargets(workspace);
    }
    
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }
    
    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) {
        listener.getLogger().println("Xcodebuilder: started");
		
        String xcodebuild = getDescriptor().getXcodebuild();
        FilePath workspace = build.getWorkspace();
        
        if(getProjectDir() != null)
			workspace = workspace.child(getProjectDir());
        
        try {
        	EnvVars envs = build.getEnvironment(listener);
        	
			if(!new FilePath(workspace.getChannel(), xcodebuild).exists()) {
				listener.fatalError("cannot find xcodebuild: " + xcodebuild);
				return false;
			}
			
			if(!workspace.exists()) {
				listener.fatalError("cannot find project dir: " + workspace);
				return false;
			}
			
			List<Integer> returnCodes = new ArrayList<Integer>();
			
			// cleanup
			if(getXcodeClean().equals("true"))
				for(String toClean: getToPerformStep("clean"))
					returnCodes.add(launcher.launch().envs(envs).stdout(listener).pwd(workspace).
							cmds(createCmds(xcodebuild,toClean,"clean")).join());
			else {
				for(String toClean: getToPerformStep("clean")) {
					FilePath buildDir = workspace.child("build");
					String[] array = toClean.split("\\|");
					
					if(buildDir.child(array[1]).isDirectory())
						buildDir = buildDir.child(array[1]);
					else if(buildDir.child(array[1] + "-iphoneos").isDirectory())
						buildDir = buildDir.child(array[1] + "-iphoneos");
					else
						continue;
					
					for(FilePath dir: buildDir.listDirectories())
						dir.deleteRecursive();
				}
			}
			
			
			// build
			for(String toBuild: getToPerformStep("build"))
				returnCodes.add(launcher.launch().envs(envs).stdout(listener).pwd(workspace).
						cmds(createCmds(xcodebuild,toBuild,"build")).join());
			
			
			// create ipa
			for(String toCreateIPA: getToPerformStep("ipa")) {
				FilePath buildDir = workspace.child("build");
				String[] array = toCreateIPA.split("\\|");
				
				if(buildDir.child(array[1]).isDirectory())
					buildDir = buildDir.child(array[1]);
				else if(buildDir.child(array[1] + "-iphoneos").isDirectory())
					buildDir = buildDir.child(array[1] + "-iphoneos");
				else {
					returnCodes.add(1);
					continue;
				}
				
				List<FilePath> apps = buildDir.list(new APPFileFilter());
				//List<FilePath> ipas = buildDir.list(new IPAFileFilter());
				//List<FilePath> dsyms = buildDir.list(new DSYMFileFilter());
				
	            for(FilePath app: apps) {
	            	if(!app.getBaseName().equals(array[0]))
	            		continue;
	            	
	            	// TODO: Datum etc.
	            	String date = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss").format(build.getTimestamp().getTime());
	            	FilePath ipa = buildDir.child(app.getBaseName() + "-" + date + "-b" + build.getNumber() + ".ipa");
	                
	                if(ipa.exists())
	                	ipa.delete();

	                FilePath payload = buildDir.child("Payload");
	                
	                if(payload.exists()) {
	                	payload.deleteRecursive();
	                	payload.mkdirs();
	                }

	                app.copyRecursiveTo(payload.child(app.getName()));
	                payload.zip(ipa.write());

	                payload.deleteRecursive();
	            }
			}
			
			// check for failed build
			if(returnCodes.contains(1))
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
			
			toPerformStep.add(new String(fields[0] + "|" + fields[1]));
		}
    	
    	return toPerformStep;
    }
    
    private List<String> createCmds(String xcodebuild, String arg, String cmd) {
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
    
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
    	private static final Pattern availableSdksPattern = Pattern.compile("^.*(?:-sdk\\s*)(\\S+)\\s*$");
        private static final Pattern parseXcodeBuildListPattern1 = Pattern.compile("^\\s*((?:[^(\\s]+\\s*)+).*$");
        private static final Pattern parseXcodeBuildListPattern2 = Pattern.compile("^\\s*((?:\\S+\\s*\\S+)+)\\s*$");
        
        private static final String DisplayName = "Xcode build";
        
    	private final int xcodeProjSearchDepth = 10;
        private String xcodebuild, currentProjectDir, workspaceTemp, xcodebuildOutputTemp;
        private boolean xcodeclean, clean, ipa, versioning;
        
        public DescriptorImpl() {
        	super();
        	load();
        }
        
        public String getXcodebuild() {
            return this.xcodebuild;
        }
        
        public void setXcodebuild(String xcodebuild) {
        	this.xcodebuild = xcodebuild;
        }
        
        public void setXcodeClean(boolean clean) {
        	this.xcodeclean = clean;
        }
        
        public boolean getXcodeClean() {
            return this.xcodeclean;
        }
        
        public void setCleanBeforeBuild(boolean clean) {
        	this.clean = clean;
        }
        
        public boolean getCleanBeforeBuild() {
            return this.clean;
        }
        
        public void setCreateIPA(boolean ipa) {
        	this.ipa = ipa;
        }
        
        public boolean getCreateIPA() {
            return this.ipa;
        }
        
        public void setUseHudsonVersioning(boolean versioning) {
        	this.versioning = versioning;
        }
        
        public boolean getUseHudsonVersioning() {
            return this.versioning;
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
            return DisplayName;
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
        
        public void doAjax(StaplerRequest req, StaplerResponse rsp, @QueryParameter String projectDir) throws IOException, ServletException {
        	this.currentProjectDir = projectDir;
        	req.getView(this,"/com/sic/bb/xcodeplugin/XcodeBuilder/targets.jelly").forward(req, rsp);
        }
        
        public FormValidation doCheckXcodebuild(@QueryParameter String value) throws IOException, ServletException {
        	if(value.isEmpty())
        		return FormValidation.error("insert absolute path to xcodebuild");
        	
            if(!value.contains("xcodebuild"))
            	return FormValidation.warningWithMarkup("xcodebuild should be the cli command");
            
            return FormValidation.ok();
        }

        @SuppressWarnings("rawtypes")
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
        	if(aClass == FreeStyleProject.class)
        		return true;
        	
            return false;
        }
        
        public String[] getProjectDirs(String workspace) {
        	ArrayList<String> projectDirs = searchXcodeProjFiles(workspace, this.xcodeProjSearchDepth);
        	String[] projectDirsArray = new String[projectDirs.size()];
        	
        	for(int i = 0; i < projectDirs.size(); i++) {
        		String path = projectDirs.get(i);
        		
        		projectDirsArray[i] = path.substring(workspace.length() + 1,path.length());
        	}
        	
        	return projectDirsArray;
        }
        
        private ArrayList<String> searchXcodeProjFiles(String workspace, int depth) {
        	ArrayList<String> projectDirs = new ArrayList<String>();
        	
        	if(depth == 0)
        		return projectDirs;
        	
        	FilePath dir = new FilePath(new File(workspace));
        	
        	try {
        		if(dir.list(new XcodeProjFileFilter()).size() != 0)
        			projectDirs.add(workspace + "/");
        		
				for(FilePath path : dir.listDirectories()) {
					if(!projectDirs.contains(workspace + "/" + path.getName()))
						projectDirs.addAll(searchXcodeProjFiles(workspace + "/" + path.getName(), depth - 1));
				}
			} catch (IOException e) {
				// TODO
				projectDirs.add("IOException: " + e.getMessage());
			} catch (InterruptedException e) {
				// TODO
				projectDirs.add("InterruptedException: " + e.getMessage());
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
			return stdout.toString();
        }
        
        @SuppressWarnings("serial")
    	private final class XcodeProjFileFilter implements FileFilter,Serializable {
            public boolean accept(File pathname) {
                return pathname.isDirectory() && pathname.getName().endsWith(".xcodeproj");
            }
        }
    }
    
    @SuppressWarnings("serial")
	private final class APPFileFilter implements FileFilter,Serializable {
        public boolean accept(File pathname) {
            return pathname.isDirectory() && pathname.getName().endsWith(".app");
        }
    }
    
    /*
    @SuppressWarnings("serial")
	private final class IPAFileFilter implements FileFilter,Serializable {
        public boolean accept(File pathname) {
            return pathname.isDirectory() && pathname.getName().endsWith(".ipa");
        }
    }
    
    @SuppressWarnings("serial")
	private final class DSYMFileFilter implements FileFilter,Serializable {
        public boolean accept(File pathname) {
            return pathname.isDirectory() && pathname.getName().endsWith(".app.dSYM");
        }
    }
    */
}