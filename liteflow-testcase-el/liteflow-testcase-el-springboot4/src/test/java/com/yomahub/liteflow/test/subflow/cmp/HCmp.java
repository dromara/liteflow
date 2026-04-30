/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.subflow.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component("h")
public class HCmp extends NodeComponent {

	@Override
	public void process() {
		DefaultContext context = this.getFirstContextBean();
		String data = this.getCmpData(String.class);
		if (!context.hasData("testMap")){
			Map<String, Integer> testMap = new HashMap<>();
			testMap.put(data, 1);
			context.setData("testMap", testMap);
		}else{
			Map<String, Integer> testMap = context.getData("testMap");
			if (testMap.containsKey(data)){
				Integer count = testMap.get(data);
				testMap.put(data, count + 1);
			}else{
				testMap.put(data, 1);
			}
		}
		System.out.println("HCmp executed!");
	}
}
