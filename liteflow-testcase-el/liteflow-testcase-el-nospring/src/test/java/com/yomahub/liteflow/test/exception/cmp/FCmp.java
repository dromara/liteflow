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
import com.yomahub.liteflow.test.exception.CustomStatefulException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FCmp extends NodeComponent {

	private static final Logger LOG = LoggerFactory.getLogger(FCmp.class);

	@Override
	public void process() {
		String str = this.getRequestData();
		if (StrUtil.isNotBlank(str) && str.equals("custom-stateful-exception")) {
			throw new CustomStatefulException("300", "chain execute custom stateful exception");
		}
		LOG.info("Fcomp executed!");
	}

}
