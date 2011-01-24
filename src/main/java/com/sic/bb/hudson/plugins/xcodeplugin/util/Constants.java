package com.sic.bb.hudson.plugins.xcodeplugin.util;

public final class Constants {
	public static final String TRUE = "true";

	public static final int RETURN_OK = 0;
	
	public static final int MIN_XCODE_PROJECT_SEARCH_DEPTH = 1;
	public static final int MAX_XCODE_PROJECT_SEARCH_DEPTH = 99;
	public static final int DEFAULT_XCODE_PROJECT_SEARCH_DEPTH = 10;
	
	public static final String DEFAULT_FILENAME_TEMPLATE = "<TARGET>_<CONFIG>_b<BUILD>_<DATETIME>";
	
	public static final String BUILD_FOLDER_NAME = "build";
	
	public static final String FIELD_DELIMITER = "|-|";
	
	public static final String BUILD_ARG = "build";
	public static final String UNIT_TEST_TARGET_ARG = "unit_test";
	public static final String CLEAN_BEFORE_BUILD_ARG = "clean_before_build";
	public static final String CREATE_IPA_ARG = "create_ipa";
	public static final String ARCHIVE_APP_ARG = "archive_app";
}
