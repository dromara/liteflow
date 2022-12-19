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
import org.noear.solon.annotation.Component;

@Component("b")
public class BCmp extends NodeComponent {
	
	private static final Logger LOG = LoggerFactory.getLogger(BCmp.class);

	@Override
	public void process() throws InterruptedException {
		String str = this.getRequestData();
		if(StrUtil.isNotBlank(str) && str.equals("when")) {
			try {
				LOG.info("Bcomp sleep begin");
				Thread.sleep(3000);
				LOG.info("Bcomp sleep end");
			} catch (InterruptedException e) {
				throw e;
			}
		}
		LOG.info("Bcomp executed!");
	}
}
