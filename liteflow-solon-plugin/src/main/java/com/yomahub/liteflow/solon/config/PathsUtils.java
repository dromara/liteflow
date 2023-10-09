package com.yomahub.liteflow.solon.config;

import cn.hutool.core.io.FileUtil;
import com.yomahub.liteflow.util.PathMatchUtil;
import org.noear.solon.core.util.ScanUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * solon 1.11.7 之后才有 Utils.resolvePaths；所以临时用下先
 *
 * @author noear
 * @since 1.11
 */
public class PathsUtils {

	public static Collection<String> resolvePaths(String pathExpr) {
		List<String> paths = new ArrayList<>();
		if(!FileUtil.isAbsolutePath(pathExpr)) {
			if (pathExpr.contains("/*") == false) { // 说明没有*符
				paths.add(pathExpr);
				return paths;
			}

			// 确定目录
			int dirIdx = pathExpr.indexOf("/*");
			String dir = pathExpr.substring(0, dirIdx);

			// 确定后缀
			int sufIdx = pathExpr.lastIndexOf(".");
			String suf = null;
			if (sufIdx > 0) {
				suf = pathExpr.substring(sufIdx);
				if (suf.contains("*")) {
					sufIdx = -1;
					suf = null;
				}
			}

			int sufIdx2 = sufIdx;
			String suf2 = suf;

			// 匹配表达式
			String expr = pathExpr.replaceAll("/\\*\\.", "/[^\\.]*\\.");
			expr = expr.replaceAll("/\\*\\*/", "(/[^/]*)*/");

			Pattern pattern = Pattern.compile(expr);

			List<String> finalPaths = paths;
			ScanUtil.scan(dir, n -> {
				// 进行后缀过滤，相对比较快
				if (sufIdx2 > 0) {
					return n.endsWith(suf2);
				}
				else {
					return true;
				}
			}).forEach(uri -> {
				// 再进行表达式过滤
				if (pattern.matcher(uri).find()) {
					finalPaths.add(uri);
				}
			});
		} else {
			String[] pathExprs = pathExpr.split(",");
			paths = PathMatchUtil.searchAbsolutePath(Arrays.asList(pathExprs));
		}
		return paths;
	}

}
