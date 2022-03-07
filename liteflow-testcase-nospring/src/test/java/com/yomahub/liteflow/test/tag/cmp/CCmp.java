/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.tag.cmp;

import com.yomahub.liteflow.core.NodeCondComponent;

public class CCmp extends NodeCondComponent {

	@Override
	public String processCond() throws Exception {
		if(this.getTag().equals("2")){
			return "e";
		}else{
			return "d";
		}
	}
}
