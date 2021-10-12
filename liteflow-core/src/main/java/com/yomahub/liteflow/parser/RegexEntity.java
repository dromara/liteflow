/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则实体，主要用于条件节点
 * @author Bryan.Zhang
 */
public class RegexEntity {

	private static final Pattern p = Pattern.compile("[^\\)\\(]+");

	private RegexNodeEntity item;

	private RegexNodeEntity[] realItemArray;

	public static RegexEntity parse(String nodeStr){
		List<String> list = new ArrayList<String>();
		Matcher m = p.matcher(nodeStr);
		while(m.find()){
			list.add(m.group());
		}

		RegexEntity regexEntity = new RegexEntity();
		regexEntity.setItem(RegexNodeEntity.parse(list.get(0)));
		try{
			String[] array = list.get(1).split("\\|");
			List<RegexNodeEntity> regexNodeEntityList = new ArrayList<>();
			for (String itemStr : array){
				regexNodeEntityList.add(RegexNodeEntity.parse(itemStr));
			}
			regexEntity.setRealItemArray(regexNodeEntityList.toArray(new RegexNodeEntity[]{}));
		}catch (Exception ignored){}
		return regexEntity;
	}

	public RegexNodeEntity getItem() {
		return item;
	}

	public void setItem(RegexNodeEntity item) {
		this.item = item;
	}

	public RegexNodeEntity[] getRealItemArray() {
		return realItemArray;
	}

	public void setRealItemArray(RegexNodeEntity[] realItemArray) {
		this.realItemArray = realItemArray;
	}
}
