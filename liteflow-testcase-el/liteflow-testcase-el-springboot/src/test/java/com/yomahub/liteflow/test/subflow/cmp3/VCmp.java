package com.yomahub.liteflow.test.subflow.cmp3;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.flow.LiteflowResponse;
import org.springframework.stereotype.Component;

@Component("v")
public class VCmp extends NodeComponent {

	@Override
	public void process() {
		String data = this.getTag();
		LiteflowResponse resp = this.invoke2Resp(data, null);

		if (!resp.isSuccess()){
			throw new RuntimeException("implicit sub flow exception");
		}
	}

}
