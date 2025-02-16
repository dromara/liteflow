package com.yomahub.liteflow.builder.el;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.*;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.InstructionSet;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.builder.el.operator.*;
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

import java.util.*;


/**
 * Chain基于代码形式的组装器 EL表达式规则专属组装器
 *
 * @author Bryan.Zhang
 * @author Jay li
 * @author jason
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
	public final static ExpressRunner EXPRESS_RUNNER = new ExpressRunner();

	static {
		// 初始化QLExpress的Runner
		EXPRESS_RUNNER.addFunction(ChainConstant.THEN, new ThenOperator());
		EXPRESS_RUNNER.addFunction(ChainConstant.WHEN, new WhenOperator());
		EXPRESS_RUNNER.addFunction(ChainConstant.SER, new ThenOperator());
		EXPRESS_RUNNER.addFunction(ChainConstant.PAR, new WhenOperator());
		EXPRESS_RUNNER.addFunction(ChainConstant.SWITCH, new SwitchOperator());
		EXPRESS_RUNNER.addFunction(ChainConstant.PRE, new PreOperator());
		EXPRESS_RUNNER.addFunction(ChainConstant.FINALLY, new FinallyOperator());
		EXPRESS_RUNNER.addFunction(ChainConstant.IF, new IfOperator());
		EXPRESS_RUNNER.addFunction(ChainConstant.NODE.toUpperCase(), new NodeOperator());
		EXPRESS_RUNNER.addFunction(ChainConstant.NODE, new NodeOperator());
		EXPRESS_RUNNER.addFunction(ChainConstant.FOR, new ForOperator());
		EXPRESS_RUNNER.addFunction(ChainConstant.WHILE, new WhileOperator());
		EXPRESS_RUNNER.addFunction(ChainConstant.ITERATOR, new IteratorOperator());
		EXPRESS_RUNNER.addFunction(ChainConstant.CATCH, new CatchOperator());
		EXPRESS_RUNNER.addFunction(ChainConstant.AND, new AndOperator());
		EXPRESS_RUNNER.addFunction(ChainConstant.OR, new OrOperator());
		EXPRESS_RUNNER.addFunction(ChainConstant.NOT, new NotOperator());
		EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.ELSE, Object.class, new ElseOperator());
		EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.ELIF, Object.class, new ElifOperator());
		EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.TO, Object.class, new ToOperator());
		EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.TO.toLowerCase(), Object.class, new ToOperator());
		EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.DEFAULT, Object.class, new DefaultOperator());
		EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.TAG, Object.class, new TagOperator());
		EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.ANY, Object.class, new AnyOperator());
		EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.MUST, Object.class, new MustOperator());
		EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.ID, Object.class, new IdOperator());
		EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.IGNORE_ERROR, Object.class, new IgnoreErrorOperator());
		EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.THREAD_POOL, Object.class, new ThreadPoolOperator());
		EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.DO, Object.class, new DoOperator());
		EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.BREAK, Object.class, new BreakOperator());
		EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.DATA, Object.class, new DataOperator());
		EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.MAX_WAIT_SECONDS, Object.class, new MaxWaitSecondsOperator());
        EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.MAX_WAIT_MILLISECONDS, Object.class, new MaxWaitMillisecondsOperator());
		EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.PARALLEL, Object.class, new ParallelOperator());
		EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.RETRY, Object.class, new RetryOperator());
		EXPRESS_RUNNER.addFunctionAndClassMethod(ChainConstant.BIND, Object.class, new BindOperator());

	}

	public static LiteFlowChainELBuilder createChain() {
		return new LiteFlowChainELBuilder();
	}

	public LiteFlowChainELBuilder() {
		chain = new Chain();
		conditionList = new ArrayList<>();
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
		} else {
			this.chain.setChainId(chainId);
		}
		return this;
	}

	public LiteFlowChainELBuilder setRoute(String routeEl){
		if (StrUtil.isBlank(routeEl)) {
			return this;
		}
		List<String> errorList = new ArrayList<>();
		try {
			DefaultContext<String, Object> context = new DefaultContext<>();

			// 往上下文里放入所有的node，使得el表达式可以直接引用到nodeId
			FlowBus.getNodeMap().keySet().forEach(nodeId -> context.put(nodeId, FlowBus.getNode(nodeId)));

			// 解析route el成为一个executable
			Executable routeExecutable = (Executable) EXPRESS_RUNNER.execute(routeEl, context, errorList, true, true);

			// 判断routeEL是不是符合规范
			if (!(routeExecutable instanceof AndOrCondition || routeExecutable instanceof NotCondition || routeExecutable instanceof Node)){
				throw new RouteELInvalidException("the route EL can only be a boolean node, or an AND or OR expression.");
			}

			// 把主要的condition加入
			this.route = routeExecutable;
			return this;
		} catch (QLException e) {
			// EL 底层会包装异常，这里是曲线处理
			if (ObjectUtil.isNotNull(e.getCause()) && Objects.equals(e.getCause().getMessage(), DataNotFoundException.MSG)) {
				// 构建错误信息
				String msg = buildDataNotFoundExceptionMsg(routeEl);
				throw new ELParseException(msg);
			}else if (ObjectUtil.isNotNull(e.getCause())){
				throw new ELParseException(e.getCause().getMessage());
			}else{
				throw new ELParseException(e.getMessage());
			}
		}catch (RouteELInvalidException e){
			throw e;
		}catch (Exception e) {
			String errMsg = StrUtil.format("parse el fail in this chain[{}];\r\n", chain.getChainId());
			throw new ELParseException(errMsg + e.getMessage());
		}
	}

	public LiteFlowChainELBuilder setEL(String elStr) {
		if (StrUtil.isBlank(elStr)) {
			String errMsg = StrUtil.format("no el in this chain[{}]", chain.getChainId());
			throw new FlowSystemException(errMsg);
		}

		this.chain.setEl(elStr);
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
		// 如果设置了不检查Node是否存在，那么这里是不解析的
		if (liteflowConfig.getParseMode().equals(ParseModeEnum.PARSE_ONE_ON_FIRST_EXEC)){
			this.chain.setCompiled(false);
			return this;
		}

		List<String> errorList = new ArrayList<>();
		try {
			DefaultContext<String, Object> context = new DefaultContext<>();

			// 这里一定要先放chain，再放node，因为node优先于chain，所以当重名时，node会覆盖掉chain
			// 往上下文里放入所有的chain，是的el表达式可以直接引用到chain
			FlowBus.getChainMap().values().forEach(chain -> context.put(chain.getChainId(), chain));

			// 往上下文里放入所有的node，使得el表达式可以直接引用到nodeId
			FlowBus.getNodeMap().keySet().forEach(nodeId -> context.put(nodeId, FlowBus.getNode(nodeId)));

			// 放入当前主chain的ID
			context.put(ChainConstant.CURR_CHAIN_ID, this.chain.getChainId());

			// 解析el成为一个Condition
			// 为什么这里只是一个Condition，而不是一个List<Condition>呢
			// 这里无论多复杂的，外面必定有一个最外层的Condition，所以这里只有一个，内部可以嵌套很多层，这点和以前的不太一样
			Condition condition = (Condition) EXPRESS_RUNNER.execute(elStr, context, errorList, true, true);

			if (Objects.isNull(condition)){
				throw new QLException(StrUtil.format("parse el fail,el:[{}]", elStr));
			}

			if (liteflowConfig.getEnableNodeInstanceId()) {
				setNodesInstanceId(condition);
			}

			// 把主要的condition加入
			this.conditionList.add(condition);
			return this;
		} catch (QLException e) {
			// EL 底层会包装异常，这里是曲线处理
			if (ObjectUtil.isNotNull(e.getCause()) && Objects.equals(e.getCause().getMessage(), DataNotFoundException.MSG)) {
				// 构建错误信息
				String msg = buildDataNotFoundExceptionMsg(elStr);
				throw new ELParseException(msg);
			}else if (ObjectUtil.isNotNull(e.getCause())){
				throw new ELParseException(e.getCause().getMessage());
			}else{
				throw new ELParseException(e.getMessage());
			}
		} catch (Exception e) {
			String errMsg = StrUtil.format("parse el fail in this chain[{}];\r\n", chain.getChainId());
			throw new ELParseException(errMsg + e.getMessage());
		}
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
    @Deprecated
    public static boolean validate(String elStr) {
        return validateWithEx(elStr).isSuccess();
    }

    /**
     * 校验
     *
     * @param elStr
     * @return
     */
    public static ValidationResp validateWithEx(String elStr) {
        ValidationResp resp = new ValidationResp();
        try {
            LiteFlowChainELBuilder.createChain().setEL(elStr);
            resp.setSuccess(true);
        } catch (Exception e) {
            LOG.error("validate error", e);
            resp.setSuccess(false);
            resp.setCause(e);
        }
        return resp;
    }

	public void build() {
		this.chain.setRouteItem(this.route);
		this.chain.setConditionList(this.conditionList);

		//暂且去掉循环依赖检测，因为有发现循环依赖检测在对大的EL进行检测的时候，会导致CPU飙升，也或许是jackson低版本的问题
		//checkBuild();

		FlowBus.addChain(this.chain);
	}

	// #region private method

	/**
	 * build 前简单校验
	 */
	private void checkBuild() {
		List<String> errorList = new ArrayList<>();
		if (StrUtil.isBlank(this.chain.getChainId())) {
			errorList.add("name is blank");
		}
		if (CollUtil.isNotEmpty(errorList)) {
			throw new RuntimeException(CollUtil.join(errorList, ",", "[", "]"));
		}
		// 对每一个 chain 进行循环引用检测
		try {
			objectMapper.writeValueAsString(this.chain);
		} catch (Exception e) {
			if (e instanceof JsonMappingException) {
				throw new CyclicDependencyException(StrUtil.format("There is a circular dependency in the chain[{}], please check carefully.", chain.getChainId(), e));
			} else {
				throw new ParseException(e.getMessage());
			}
		}
	}

	/**
	 * 解析 EL 表达式，查找未定义的 id 并构建错误信息
	 * @param elStr el 表达式
	 */
	private static String buildDataNotFoundExceptionMsg(String elStr) {
		String msg = String.format("[node/chain is not exist or node/chain not register]\n EL: %s",
				StrUtil.trim(elStr));
		try {
			InstructionSet parseResult = EXPRESS_RUNNER.getInstructionSetFromLocalCache(elStr);
			if (parseResult == null) {
				return msg;
			}

			String[] outAttrNames = parseResult.getOutAttrNames();
			if (ArrayUtil.isEmpty(outAttrNames)) {
				return msg;
			}

			List<String> chainIds = CollUtil.map(FlowBus.getChainMap().values(), Chain::getChainId, true);
			List<String> nodeIds = CollUtil.map(FlowBus.getNodeMap().values(), Node::getId, true);
			for (String attrName : outAttrNames) {
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
						// 需要加上 "EL: " 的长度 4，再加上 “node("” 长度 6，再加上 "^" 的长度 1，indexOf 从 0
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
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();

		// 如果chain已经有Condition了，那说明已经解析过了，这里只对未解析的chain进行解析
		if (CollUtil.isNotEmpty(chain.getConditionList())){
			return;
		}

		List<String> errorList = new ArrayList<>();
		try {
			DefaultContext<String, Object> context = new DefaultContext<>();

			// 这里一定要先放chain，再放node，因为node优先于chain，所以当重名时，node会覆盖掉chain
			// 往上下文里放入所有的chain，是的el表达式可以直接引用到chain
			FlowBus.getChainMap().values().forEach(chainItem -> context.put(chainItem.getChainId(), chainItem));

			// 往上下文里放入所有的node，使得el表达式可以直接引用到nodeId
			FlowBus.getNodeMap().keySet().forEach(nodeId -> context.put(nodeId, FlowBus.getNode(nodeId)));

			// 放入当前主chain的ID
			context.put(ChainConstant.CURR_CHAIN_ID, chain.getChainId());


			// 只有当PARSE_ONE_ON_FIRST_EXEC时才会执行这个方法
			// 那么会有一种级联的情况：这个EL中含有其他的chain，如果这时候不先解析其他chain，就到导致诸如循环场景无法设置index或者obj的情况
			// 所以这里要判断表达式里有没有其他的chain，如果有，进行先行解析

			String[] itemArray = EXPRESS_RUNNER.getOutVarNames(chain.getEl());
			Arrays.stream(itemArray).forEach(item -> {
                if (FlowBus.containChain(item)){
					Chain itemChain = FlowBus.getChain(item);
					if (!itemChain.isCompiled()){
						buildUnCompileChain(FlowBus.getChain(item));
					}
                }
            });

			// 解析el成为一个Condition
			// 为什么这里只是一个Condition，而不是一个List<Condition>呢
			// 这里无论多复杂的，外面必定有一个最外层的Condition，所以这里只有一个，内部可以嵌套很多层，这点和以前的不太一样
			Condition condition = (Condition) EXPRESS_RUNNER.execute(chain.getEl(), context, errorList, true, true);

			if (Objects.isNull(condition)){
				throw new QLException(StrUtil.format("parse el fail,el:[{}]", chain.getEl()));
			}

			// 设置实例id
			if (liteflowConfig.getEnableNodeInstanceId()) {
				NodeInstanceIdManageSpi nodeInstanceIdManageSpi = NodeInstanceIdManageSpiHolder.getInstance().getNodeInstanceIdManageSpi();
				nodeInstanceIdManageSpi.setNodesInstanceId(condition, chain);
			}

			// 把主要的condition加入
			chain.setConditionList(CollUtil.toList(condition));

			// 把chain的isCompiled设置为true
			chain.setCompiled(true);

			FlowBus.addChain(chain);
		} catch (QLException e) {
			// EL 底层会包装异常，这里是曲线处理
			if (ObjectUtil.isNotNull(e.getCause()) && Objects.equals(e.getCause().getMessage(), DataNotFoundException.MSG)) {
				// 构建错误信息
				String msg = buildDataNotFoundExceptionMsg(chain.getEl());
				throw new ELParseException(msg);
			}else if (ObjectUtil.isNotNull(e.getCause())){
				throw new ELParseException(e.getCause().getMessage());
			}else{
				throw new ELParseException(e.getMessage());
			}
		} catch (Exception e) {
			String errMsg = StrUtil.format("parse el fail in this chain[{}];\r\n", chain.getChainId());
			throw new ELParseException(errMsg + e.getMessage());
		}
	}

}
