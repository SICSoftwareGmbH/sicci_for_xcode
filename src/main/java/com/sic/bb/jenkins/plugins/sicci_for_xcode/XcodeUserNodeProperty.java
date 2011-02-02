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

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.Secret;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class XcodeUserNodeProperty extends NodeProperty<Node> {
	private Secret username;
	private Secret password;
	
	@DataBoundConstructor
	public XcodeUserNodeProperty(Secret username, Secret password) {		
		this.username = username;
		this.password = password;
	}
	
	public String getUsername() {
		if(StringUtils.isBlank(Secret.toString(this.username)))
			return null;
		
		return Secret.toString(this.username);
	}
	
	public String getPassword() {
		if(StringUtils.isBlank(Secret.toString(this.password)))
			return null;
		
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
