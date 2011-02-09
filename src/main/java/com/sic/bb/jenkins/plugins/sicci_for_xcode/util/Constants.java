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

package com.sic.bb.jenkins.plugins.sicci_for_xcode.util;

public final class Constants {
	public static final String TRUE = "true";

	public static final int RETURN_OK = 0;
	
	public static final int MIN_XCODE_PROJECT_SEARCH_DEPTH = 1;
	public static final int MAX_XCODE_PROJECT_SEARCH_DEPTH = 99;
	public static final int DEFAULT_XCODE_PROJECT_SEARCH_DEPTH = 10;
	
	public static final String DEFAULT_FILENAME_TEMPLATE = "<TARGET>_<CONFIG>_b<BUILD>_<DATETIME>";
	
	public static final String BUILD_FOLDER_NAME = "build";
	
	public static final String TEST_FOLDER_NAME = "test-reports";
	
	public static final String FIELD_DELIMITER = "|-|";
	
	public static final String PROJECT_DIR_ARG = "ProjectDir";
	public static final String XCODE_PLATFORM_ARG = "XcodePlatform";
	public static final String FILENAME_TEMPLATE_ARG = "FilenameTemplate";
	public static final String XCODE_PROJECT_SEARCH_DEPTH_ARG = "XcodeProjectSearchDepth";
	public static final String PROJECT_DIRS_ARG = "ProjectDirs";
	public static final String BUILD_TARGETS_ARG = "BuildTargets";
	public static final String BUILD_CONFIGURATIONS_ARG = "BuildConfigurations";
	
	public static final String BUILD_ARG = "build";
	public static final String UNIT_TEST_TARGET_ARG = "unit_test";
	public static final String CLEAN_BEFORE_BUILD_ARG = "clean_before_build";
	public static final String CREATE_IPA_ARG = "create_ipa";
	public static final String ARCHIVE_APP_ARG = "archive_app";
}
