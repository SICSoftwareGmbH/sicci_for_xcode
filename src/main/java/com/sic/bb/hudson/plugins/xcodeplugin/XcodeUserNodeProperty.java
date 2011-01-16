package com.sic.bb.hudson.plugins.xcodeplugin;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Slave;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;

public class XcodeUserNodeProperty extends NodeProperty<Slave> {
	private String username;
	private String password;
	
	@DataBoundConstructor
	public XcodeUserNodeProperty(String username, String password) {
		this.username = username;
		this.password = password;
	}
	
	public String getUsername() {
		return this.username;
	}
	
	public String getPassword() {
		return this.password;
	}
	
	@Extension	
	public static final class XcodeUserNodePropertyDescriptor extends NodePropertyDescriptor {
		@Override
		public String getDisplayName() {
			return Messages.XcodeUserNodeProperty_getDisplayName();
		}
	}
}
