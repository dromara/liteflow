package com.thebeastshop.liteflow.test.regex;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTest {
	
	public static void main(String[] args) {
		String str = "192.168.1.1:2181,192.168.1.2:2182,192.168.1.3:2183";
		List<String> list = new ArrayList<String>();
	    Pattern p = Pattern.compile("[\\w\\d][\\w\\d\\.]+\\:(\\d)+(\\,[\\w\\d][\\w\\d\\.]+\\:(\\d)+)*");
	    Matcher m = p.matcher(str);
	    while(m.find()){
	        list.add(m.group());
	    }
	    System.out.println(list.size());
	    System.out.println(list);

	}

}
