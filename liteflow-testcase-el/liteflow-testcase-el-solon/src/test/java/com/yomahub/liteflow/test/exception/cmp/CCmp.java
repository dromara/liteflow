/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.exception.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.noear.solon.annotation.Component;

@Component("c")
public class CCmp extends NodeComponent {
	
	private static final Logger LOG = LoggerFactory.getLogger(CCmp.class);
	
	@Override
	public void process() {
		LOG.info("Ccomp executed!");
	}
}
