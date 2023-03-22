package com.yomahub.liteflow.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * logo打印器
 *
 * @author Bryan.Zhang
 */
public class LOGOPrinter {

	private static final Logger LOG = LoggerFactory.getLogger(LOGOPrinter.class);

	/**
	 * LiteFlow 当前版本号
	 */
	private static final String VERSION_NO = getVersion();

	public static void print() {
		StringBuilder str = new StringBuilder("\n");
		str.append(
				"================================================================================================\n");
		str.append("		 _     ___ _____ _____      _____ _     _____        __\n");
		str.append("		| |   |_ _|_   _| ____|    |  ___| |   / _ \\ \\      / /\n");
		str.append("		| |    | |  | | |  _| _____| |_  | |  | | | \\ \\ /\\ / / \n");
		str.append("		| |___ | |  | | | |__|_____|  _| | |__| |_| |\\ V  V /  \n");
		str.append("		|_____|___| |_| |_____|    |_|   |_____\\___/  \\_/\\_/   \n\n");
		str.append("		Version: " + VERSION_NO + "\n");
		str.append("		新一代轻量且强大的编排式规则引擎。\n");
		str.append("		基于开源社区文化，社区驱动型开源框架。\n");
		str.append("		Small but powerful rules engine.\n");
		str.append("		官网地址：https://liteflow.yomahub.com/\n");
		str.append("		wechat：bryan_31\n");
		str.append(
				"================================================================================================\n");
		LOG.info(str.toString());
	}

	private static String getVersion() {
		return Optional.ofNullable(LOGOPrinter.class.getPackage()).map(Package::getImplementationVersion).orElse("DEV");
	}

}
