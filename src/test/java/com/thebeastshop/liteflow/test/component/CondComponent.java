/**
 * <p>Title: litis</p>
 * <p>Description: redis的全方位开发运维平台</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @email 47483522@qq.com
 * @Date 2017-11-28
 */
package com.thebeastshop.liteflow.test.component;

import org.springframework.stereotype.Component;

import com.thebeastshop.liteflow.core.NodeComponent;
import com.thebeastshop.liteflow.core.NodeCondComponent;

@Component("cond")
public class CondComponent extends NodeCondComponent {

	@Override
	protected Class<? extends NodeComponent> processCond() throws Exception {
		return BComponent.class;
	}
}
