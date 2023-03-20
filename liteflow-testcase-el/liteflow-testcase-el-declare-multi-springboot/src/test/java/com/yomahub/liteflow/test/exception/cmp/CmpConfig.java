package com.yomahub.liteflow.test.exception.cmp;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.test.exception.CustomStatefulException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@LiteflowComponent
public class CmpConfig {

	private static final Logger LOG = LoggerFactory.getLogger(CmpConfig.class);

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
	public void processA(NodeComponent bindCmp) {
		String str = bindCmp.getRequestData();
		if (StrUtil.isNotBlank(str) && str.equals("exception")) {
			throw new RuntimeException("chain execute execption");
		}
		LOG.info("Acomp executed!");
	}

	//////////
	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
	public void processB(NodeComponent bindCmp) throws InterruptedException {
		String str = bindCmp.getRequestData();
		if (StrUtil.isNotBlank(str) && str.equals("when")) {
			try {
				LOG.info("Bcomp sleep begin");
				Thread.sleep(3000);
				LOG.info("Bcomp sleep end");
			}
			catch (InterruptedException e) {
				throw e;
			}
		}
		LOG.info("Bcomp executed!");
	}

	//////////
	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "c")
	public void processC(NodeComponent bindCmp) {
		LOG.info("Ccomp executed!");
	}

	//////////
	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "d")
	public void processD(NodeComponent bindCmp) {
		if (1 == 1) {
			int a = 1 / 0;
		}
		LOG.info("Dcomp executed!");
	}

	////////
	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_SWITCH, nodeId = "e", nodeType = NodeTypeEnum.SWITCH)
	public String processSwitchE(NodeComponent bindCmp) throws Exception {
		return "a";
	}

	//////////
	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "f")
	public void processF(NodeComponent bindCmp) {
		String str = bindCmp.getRequestData();
		if (StrUtil.isNotBlank(str) && str.equals("custom-stateful-exception")) {
			throw new CustomStatefulException("300", "chain execute custom stateful execption");
		}
		LOG.info("Fcomp executed!");
	}

	////////
	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "g")
	public void processG(NodeComponent bindCmp) {
		LOG.info("Gcomp executed!");
	}

}
