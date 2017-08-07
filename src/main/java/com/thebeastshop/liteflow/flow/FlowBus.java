/**
 * <p>Title: liteFlow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2017-7-28
 * @version 1.0
 */
package com.thebeastshop.liteflow.flow;

import java.util.HashMap;
import java.util.Map;

import com.thebeastshop.liteflow.entity.config.Chain;

public class FlowBus {
	
	private static Map<String, Chain> chainMap;
	
	public static Chain getChain(String id) throws Exception{
		if(chainMap == null || chainMap.isEmpty()){
			throw new Exception("please config the rule first");
		}
		return chainMap.get(id);
	}
	
	public static void addChain(String name,Chain chain){
		if(chainMap == null){
			chainMap = new HashMap<String, Chain>();
		}
		chainMap.put(name, chain);
	}
}
