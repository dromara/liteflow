package com.thebeastshop.liteflow.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LOGOPrinter {
	
	private static final Logger LOG = LoggerFactory.getLogger(LOGOPrinter.class);
	
	public static void print() {
		StringBuilder str = new StringBuilder("\n");
		str.append("================================================================================================\n");
		str.append("		 _     ___ _____ _____      _____ _     _____        __\n");
		str.append("		| |   |_ _|_   _| ____|    |  ___| |   / _ \\ \\      / /\n");
		str.append("		| |    | |  | | |  _| _____| |_  | |  | | | \\ \\ /\\ / / \n");
		str.append("		| |___ | |  | | | |__|_____|  _| | |__| |_| |\\ V  V /  \n");
		str.append("		|_____|___| |_| |_____|    |_|   |_____\\___/  \\_/\\_/   \n\n");
		str.append("		做最轻量级，最吊炸天的微流程框架\n");
		str.append("		To be the most lightweight and the most practical micro-process framework\n");
		str.append("================================================================================================\n");
		LOG.info(str.toString());
	}
}
