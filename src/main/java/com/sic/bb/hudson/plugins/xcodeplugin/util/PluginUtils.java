package com.sic.bb.hudson.plugins.xcodeplugin.util;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class PluginUtils {
	//private static final String[] REGEXSPECIALCHARS = new String[] {"(",")","{","}","[","]","^","$","*",".","?","|"};

	public static String stringToRegex(String string) {
        /*for(String regexChar: REGEXSPECIALCHARS)
        	string = string.replace(regexChar, "\\" + regexChar);
        
        return string;*/
		
		return Pattern.quote(string);
	}
	
	public static Pattern createPattern(List<String> stringList) {
	    StringBuilder regex = new StringBuilder();
	    
	    if(stringList != null) {
	      regex.append('(');
	      
	      for(String item: stringList) {
	        if(StringUtils.isBlank(item))
	          continue;
	        
	        regex.append(stringToRegex(item));
	        regex.append('|');
	      }
	      
	      regex.deleteCharAt(regex.length() - 1);
	      regex.append(')');
	    }
	    
	    return Pattern.compile(regex.toString());
	}
}
