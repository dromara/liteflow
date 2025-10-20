/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.exception.cmp;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.NodeComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("a")
public class ACmp extends NodeComponent {

	private static final Logger LOG = LoggerFactory.getLogger(ACmp.class);

	@Override
	public void process() {
		String str = this.getRequestData();
		if (StrUtil.isNotBlank(str) && str.equals("exception")) {
			throw new RuntimeException("chain execute exception");
		}
		LOG.info("Acomp executed!");
	}

}
