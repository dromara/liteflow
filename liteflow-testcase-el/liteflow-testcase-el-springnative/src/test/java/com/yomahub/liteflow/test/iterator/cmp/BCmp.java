
package com.yomahub.liteflow.test.iterator.cmp;

import com.yomahub.liteflow.core.NodeBooleanComponent;
import org.springframework.stereotype.Component;

@Component("b")
public class BCmp extends NodeBooleanComponent {

	@Override
	public boolean processBoolean() throws Exception {
		return this.getLoopIndex() == 1;
	}

}
