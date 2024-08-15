package com.yomahub.liteflow.util;

import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * logo打印器
 *
 * @author Bryan.Zhang
 */
public class LOGOPrinter {

	private static final LFLog LOG = LFLoggerManager.getLogger(LOGOPrinter.class);

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
		str.append("		Make your code amazing.\n");
		str.append("		website：https://liteflow.cc\n");
		str.append(
				"================================================================================================\n");
		LOG.info(str.toString());
	}

	private static String getVersion() {
		return Optional.ofNullable(LOGOPrinter.class.getPackage()).map(Package::getImplementationVersion).orElse("DEV");
	}

}
