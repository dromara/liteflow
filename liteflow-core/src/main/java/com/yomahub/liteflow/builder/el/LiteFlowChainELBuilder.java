package com.yomahub.liteflow.builder.el;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.QLOptions;
import com.alibaba.qlexpress4.QLResult;
import com.alibaba.qlexpress4.exception.QLException;

import java.util.HashMap;
import java.util.Map;

import com.yomahub.liteflow.common.ChainConstant;
import com.yomahub.liteflow.common.entity.ValidationResp;
import com.yomahub.liteflow.enums.ParseModeEnum;
import com.yomahub.liteflow.exception.*;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.element.condition.AndOrCondition;
import com.yomahub.liteflow.flow.element.condition.NotCondition;
import com.yomahub.liteflow.flow.instanceId.NodeInstanceIdManageSpi;
import com.yomahub.liteflow.flow.instanceId.NodeInstanceIdManageSpiHolder;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.util.ElRegexUtil;
import com.yomahub.liteflow.util.QlExpressUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * Chain基于代码形式的组装器 EL表达式规则专属组装器
 *
 * @author Bryan.Zhang
 * @author Jay li
 * @author jason
 * @author luo yi
 * @since 2.8.0
 */
public class LiteFlowChainELBuilder {

	private static final LFLog LOG = LFLoggerManager.getLogger(LiteFlowChainELBuilder.class);

	private static ObjectMapper objectMapper = new ObjectMapper();

	private Chain chain;

	/**
	 * 这是route EL的文本
	 */
	private Executable route;

	/**
	 * 这是主体的Condition //声明这个变量，而不是用chain.getConditionList的目的，是为了辅助平滑加载
	 * 虽然FlowBus里面的map都是CopyOnWrite类型的，但是在buildCondition的时候，为了平滑加载，所以不能事先把chain.getConditionList给设为空List
	 * 所以在这里做一个缓存，等conditionList全部build完毕后，再去一次性替换chain里面的conditionList
	 */
	private final List<Condition> conditionList;
    /**
     * EL解析引擎
     */
    private final static Express4Runner EXPRESS_RUNNER = QlExpressUtils.getELExpressRunner();

	public static LiteFlowChainELBuilder createChain() {
		return new LiteFlowChainELBuilder();
	}

	public static LiteFlowChainELBuilder fromChain(Chain chain){
		return new LiteFlowChainELBuilder(chain);
	}

	public LiteFlowChainELBuilder() {
		this.chain = new Chain();
		this.conditionList = new ArrayList<>();
	}

	public LiteFlowChainELBuilder(Chain chain) {
		this.chain = chain;
		this.conditionList = new ArrayList<>();
	}

	// 在parser中chain的build是2段式的，因为涉及到依赖问题，以前是递归parser
	// 2.6.8之后取消了递归的模式，两段式组装，先把带有chainName的chain对象放进去，第二段再组装chain里面的condition
	// 所以这里setChainName的时候需要判断下

	/**
	 * @return LiteFlowChainELBuilder
	 * @deprecated 请使用 {@link #setChainId(String)}
	 */
	@Deprecated
	public LiteFlowChainELBuilder setChainName(String chainName) {
		if (FlowBus.containChain(chainName)) {
			this.chain = FlowBus.getChain(chainName);
		} else {
			this.chain.setChainName(chainName);
		}
		return this;
	}

	public LiteFlowChainELBuilder setChainId(String chainId) {
		if (FlowBus.containChain(chainId)) {
			this.chain = FlowBus.getChain(chainId);
			this.chain.setCompiled(false);
		} else {
			this.chain.setChainId(chainId);
		}
		return this;
	}

	public LiteFlowChainELBuilder setRoute(String routeEl){
		if (StrUtil.isBlank(routeEl)) {
			return this;
		}
		this.chain.setRouteEl(routeEl);
		return this;
	}

	public LiteFlowChainELBuilder setEL(String elStr) {
		if (StrUtil.isBlank(elStr)) {
			String errMsg = StrUtil.format("no el in this chain[{}]", chain.getChainId());
			throw new FlowSystemException(errMsg);
		}

		this.chain.setEl(elStr);

		String elMd5 = MD5.create().digestHex(ElRegexUtil.normalize(elStr));
		this.chain.setElMd5(elMd5);

		return this;
	}

	// 往condition里设置instanceId
    private void setNodesInstanceId(Condition condition) {
		NodeInstanceIdManageSpi nodeInstanceIdManageSpi = NodeInstanceIdManageSpiHolder.getInstance().getNodeInstanceIdManageSpi();

		nodeInstanceIdManageSpi.setNodesInstanceId(condition, chain);
    }


	public LiteFlowChainELBuilder setNamespace(String nameSpace){
		if (StrUtil.isBlank(nameSpace)) {
			nameSpace = ChainConstant.DEFAULT_NAMESPACE;
		}
		this.chain.setNamespace(nameSpace);
		return this;
	}

	public LiteFlowChainELBuilder setThreadPoolExecutorClass(String threadPoolExecutorClass) {
		this.chain.setThreadPoolExecutorClass(threadPoolExecutorClass);
		return this;
	}

    /**
     * EL表达式校验，此方法已经过时，请使用 {@link LiteFlowChainELBuilder#validateWithEx(String)}
     *
     * @param elStr EL表达式
     * @return true 校验成功 false 校验失败
     */
    public static boolean validate(String elStr) {
        return validateWithEx(elStr).isSuccess();
    }

    /**
     * 校验
     *
	 * @param elStr EL表达式
     * @return ValidationResp
     */
    public static ValidationResp validateWithEx(String elStr) {
        try {
            LiteFlowChainELBuilder.createChain().compile(elStr, true);
            return ValidationResp.success();
        } catch (Exception e) {
			String msg = buildDataNotFoundExceptionMsg(elStr);
            return ValidationResp.fail(new ELParseException(msg));
        }
    }

	public void build() {
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
		// 如果设置了不检查Node是否存在，那么这里是不解析的
		if (liteflowConfig.getParseMode().equals(ParseModeEnum.PARSE_ONE_ON_FIRST_EXEC)){
			this.chain.setCompiled(false);
			FlowBus.addChain(this.chain);
		}else{
			compileChain();
		}
	}

	private void compileChain(){
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
		// 编译规则
		String elStr = this.chain.getEl();
		if (StrUtil.isBlank(elStr)) {
			String errMsg = StrUtil.format("no el in this chain[{}]", chain.getChainId());
			throw new FlowSystemException(errMsg);
		}
		try {
			Condition condition = compile(elStr, true);

			if (Objects.isNull(condition)){
				throw new ELParseException(StrUtil.format("parse el fail,el:[{}]", elStr));
			}

			if (liteflowConfig.getEnableNodeInstanceId()) {
				setNodesInstanceId(condition);
			}

			// 把主要的condition加入
			this.conditionList.add(condition);
		} catch (QLException e) {
			// EL 底层会包装异常，这里是曲线处理
			if (ObjectUtil.isNotNull(e.getCause())) {
				// 构建错误信息
				String msg = buildDataNotFoundExceptionMsg(elStr);
				throw new ELParseException(msg);
			}else{
				throw new ELParseException(StrUtil.isNotBlank(e.getMessage()) ? e.getMessage() : "Unknown EL parse error");
			}
		} catch (Exception e) {
			String errMsg = StrUtil.format("parse el fail in this chain[{}];\r\n", chain.getChainId());
			String exMsg = e.getMessage();
			throw new ELParseException(errMsg + (StrUtil.isNotBlank(exMsg) ? exMsg : e.getClass().getSimpleName()));
		}


		// 编译决策路由
		if (StrUtil.isNotBlank(this.chain.getRouteEl())){
			String routeEl = this.chain.getRouteEl();
			try {
				Executable routeExecutable = compile(routeEl, false);

				// 判断routeEL是不是符合规范
				if (!(routeExecutable instanceof AndOrCondition || routeExecutable instanceof NotCondition || routeExecutable instanceof Node)){
					throw new RouteELInvalidException("the route EL can only be a boolean node, or an AND or OR expression.");
				}

				// 把主要的condition加入
				this.route = routeExecutable;
			} catch (QLException e) {
				// EL 底层会包装异常，这里是曲线处理
				if (ObjectUtil.isNotNull(e.getCause()) && Objects.equals(e.getCause().getMessage(), DataNotFoundException.MSG)) {
					// 构建错误信息
					String msg = buildDataNotFoundExceptionMsg(routeEl);
					throw new ELParseException(msg);
				}else if (ObjectUtil.isNotNull(e.getCause())){
					String causeMsg = e.getCause().getMessage();
					throw new ELParseException(StrUtil.isNotBlank(causeMsg) ? causeMsg : e.getMessage());
				}else{
					throw new ELParseException(StrUtil.isNotBlank(e.getMessage()) ? e.getMessage() : "Unknown EL parse error");
				}
			}catch (RouteELInvalidException e){
				throw e;
			}catch (Exception e) {
				String errMsg = StrUtil.format("parse el fail in this chain[{}];\r\n", chain.getChainId());
				String exMsg = e.getMessage();
				throw new ELParseException(errMsg + (StrUtil.isNotBlank(exMsg) ? exMsg : e.getClass().getSimpleName()));
			}
		}
		this.chain.setConditionList(this.conditionList);
		this.chain.setRouteItem(this.route);
		if (CollectionUtil.isNotEmpty(this.chain.getConditionList())){
			this.chain.setCompiled(true);
		}
		FlowBus.addChain(this.chain);
	}


	/**
	 * 解析 EL 表达式，查找未定义的 id 并构建错误信息
	 * @param elStr el 表达式
	 */
	private static String buildDataNotFoundExceptionMsg(String elStr) {
		String msg = String.format("[node/chain is not exist or node/chain not register]\n EL: %s",
				StrUtil.trim(elStr));
		try {
			// 使用 QLExpress4 的 getOutVarNames 方法获取脚本中使用的所有外部变量
			Set<String> outVarNames = EXPRESS_RUNNER.getOutVarNames(elStr);
			if (CollUtil.isEmpty(outVarNames)) {
				return msg;
			}

			List<String> chainIds = CollUtil.map(FlowBus.getChainMap().values(), Chain::getChainId, true);
			List<String> nodeIds = CollUtil.map(FlowBus.getNodeMap().values(), Node::getId, true);
			for (String attrName : outVarNames) {
				if (!chainIds.contains(attrName) && !nodeIds.contains(attrName)) {
					msg = String.format(
							"[%s] is not exist or [%s] is not registered, you need to define a node or chain with id [%s] and register it \n EL: ",
							attrName, attrName, attrName);

					// 去除 EL 表达式中的空格和换行符
					String sourceEl = StrUtil.removeAll(elStr, CharUtil.SPACE, CharUtil.LF, CharUtil.CR);
					// 这里需要注意的是，nodeId 和 chainId 可能是关键字的一部分，如果直接 indexOf(attrName) 会出现误判
					// 所以需要判断 attrName 前后是否有 ","
					int commaRightIndex = sourceEl.indexOf(attrName + StrUtil.COMMA);
					if (commaRightIndex != -1) {
						// 需要加上 "EL: " 的长度 4，再加上 "^" 的长度 1，indexOf 从 0 开始，所以还需要加 1
						return msg + sourceEl + "\n" + StrUtil.fill("^", CharUtil.SPACE, commaRightIndex + 6, true);
					}
					int commaLeftIndex = sourceEl.indexOf(StrUtil.COMMA + attrName);
					if (commaLeftIndex != -1) {
						// 需要加上 "EL: " 的长度 4，再加上 "^" 的长度 1，再加上 "," 的长度 1，indexOf 从 0
						// 开始，所以还需要加 1
						return msg + sourceEl + "\n" + StrUtil.fill("^", CharUtil.SPACE, commaLeftIndex + 7, true);
					}
					// 还有一种特殊情况，就是 EL 表达式中的节点使用 node("a")
					int nodeIndex = sourceEl.indexOf(String.format("node(\"%s\")", attrName));
					if (nodeIndex != -1) {
						// 需要加上 "EL: " 的长度 4，再加上 "node("" 长度 6，再加上 "^" 的长度 1，indexOf 从 0
						// 开始，所以还需要加 1
						return msg + sourceEl + "\n" + StrUtil.fill("^", CharUtil.SPACE, commaLeftIndex + 12, true);
					}
				}
			}
		} catch (Exception ex) {
			// ignore
		}
		return msg;
	}

	public static void buildUnCompileChain(Chain chain){
		if (StrUtil.isBlank(chain.getEl())){
			throw new FlowSystemException(StrUtil.format("no el content in this unCompile chain[{}]", chain.getChainId()));
		}
		fromChain(chain).compileChain();
	}

	@SuppressWarnings("unchecked")
	private <T extends Executable> T compile(String elStr, boolean putChain2Context) throws Exception{
		Map<String, Object> context = new HashMap<>();

		if (putChain2Context){
			// 这里一定要先放chain，再放node，因为node优先于chain，所以当重名时，node会覆盖掉chain
			// 往上下文里放入所有的chain，是的el表达式可以直接引用到chain
			FlowBus.getChainMap().values().forEach(chain -> context.put(chain.getChainId(), chain));
		}

		// 往上下文里放入所有的node，使得el表达式可以直接引用到nodeId
		FlowBus.getNodeMap().keySet().forEach(nodeId -> context.put(nodeId, FlowBus.getNode(nodeId)));

		// 放入当前主chain的ID
		if (this.chain != null){
			context.put(ChainConstant.CURR_CHAIN_ID, this.chain.getChainId());
		}

		// 那么会有一种级联的情况：这个EL中含有其他的chain，如果这时候不先解析其他chain，就到导致诸如循环场景无法设置index或者obj的情况
		// 所以这里要判断表达式里有没有其他的chain，如果有，进行先行解析
		Set<String> itemSet = EXPRESS_RUNNER.getOutVarNames(elStr);
		itemSet.forEach(item -> {
			if (FlowBus.containChain(item) && !chain.getChainId().equals(item)) {
				Chain itemChain = FlowBus.getChain(item);
				if (!itemChain.isCompiled()){
					buildUnCompileChain(FlowBus.getChain(item));
				}
			}
		});

		// 解析el成为一个Condition
		// 为什么这里只是一个Condition，而不是一个List<Condition>呢
		// 这里无论多复杂的，外面必定有一个最外层的Condition，所以这里只有一个，内部可以嵌套很多层，这点和以前的不太一样
        QLResult expressResult = EXPRESS_RUNNER.execute(elStr, context, QLOptions.builder().cache(true).build());
        return (T) expressResult.getResult();
	}

	public Chain getChain() {
		return chain;
	}
}
