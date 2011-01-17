package com.sic.bb.hudson.plugins.xcodeplugin;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.Secret;

public class XcodeUserNodeProperty extends NodeProperty<Node> {
	private Secret username;
	private Secret password;
	
	@DataBoundConstructor
	public XcodeUserNodeProperty(Secret username, Secret password) {		
		this.username = username;
		this.password = password;
	}
	
	public String getUsername() {
		return Secret.toString(this.username);
	}
	
	public String getPassword() {
		return Secret.toString(this.password);
	}
	
	public static XcodeUserNodeProperty getCurrentNodesProperties() {
		XcodeUserNodeProperty property = Computer.currentComputer().getNode().getNodeProperties().get(XcodeUserNodeProperty.class);
		
		if(property == null)
			property = Hudson.getInstance().getGlobalNodeProperties().get(XcodeUserNodeProperty.class);
		
		return property;
	}
	
	@Extension	
	public static final class XcodeUserNodePropertyDescriptor extends NodePropertyDescriptor {
		public XcodeUserNodePropertyDescriptor() {
			super(XcodeUserNodeProperty.class);
		}
		
		@Override
		public String getDisplayName() {
			return Messages.XcodeUserNodePropertyDescriptor_getDisplayName();
		}
	}
}
