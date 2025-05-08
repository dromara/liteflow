package com.yomahub.liteflow.core.proxy;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.exceptions.InvocationTargetRuntimeException;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.InstructionSet;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.annotation.LiteflowRetry;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.exception.ComponentMethodDefineErrorException;
import com.yomahub.liteflow.exception.LiteFlowException;
import com.yomahub.liteflow.exception.ParameterFactException;
import com.yomahub.liteflow.exception.ProxyException;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.util.LiteflowContextRegexMatcher;
import com.yomahub.liteflow.util.SerialsUtil;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 声明式组件的代理核心生成类
 * @author Bryan.Zhang
 * @since 2.11.4
 */
public class DeclComponentProxy {

    private final LFLog LOG = LFLoggerManager.getLogger(this.getClass());

    private final DeclWarpBean declWarpBean;

    public DeclComponentProxy(DeclWarpBean declWarpBean) {
        this.declWarpBean = declWarpBean;
    }

    public NodeComponent getProxy() throws Exception {
        // 获取当前节点的原有注解，如：LiteFlowRetry 之类的规则注解
        Annotation[] beanClassAnnotation = declWarpBean.getRawClazz().getAnnotations();

        // nodeType去重
        List<? extends Class<? extends NodeComponent>> classes = declWarpBean.getMethodWrapBeanList().stream()
                .map((Function<MethodWrapBean, Class<? extends NodeComponent>>) methodWrapBean -> methodWrapBean.getLiteflowMethod().nodeType().getMappingClazz())
                .distinct()
                .collect(Collectors.toList());
        // 相同nodeId里只能定义同一种的类型的NodeComponent
        boolean legal = classes.size() == 1;
        if (!legal) {
            throw new LiteFlowException("The cmpClass of the same nodeId must be the same,you declared nodeId:"
                    + declWarpBean.getNodeId() + ",cmpClass:" + declWarpBean.getRawClazz());
        }

        //获取方法上的LiteflowRetry，如果有的话（一般都是方法级声明式）
        LiteflowRetry liteflowRetry = declWarpBean.getMethodWrapBeanList().stream().filter(methodWrapBean -> {
            LiteflowRetry lfr = methodWrapBean.getLiteflowRetry();
            LiteflowMethod lfm = methodWrapBean.getLiteflowMethod();
            return lfr != null && lfm.value().isMainMethod();
        }).map(MethodWrapBean::getLiteflowRetry).findFirst().orElse(null);

        //如果存在LiteflowRetry，则加到类上的注解数组中
        if (liteflowRetry != null){
            beanClassAnnotation = ArrayUtil.append(beanClassAnnotation, liteflowRetry);
        }

        try {
            // 创建对象
            // 这里package进行了重设，放到了被代理对象的所在目录
            // 生成的对象也加了上被代理对象拥有的注解
            // 被拦截的对象也根据被代理对象根据@LiteFlowMethod所标注的进行了动态判断
            Object instance = new ByteBuddy().subclass(declWarpBean.getNodeType().getMappingClazz())
                    .name(StrUtil.format("{}$ByteBuddy${}${}", declWarpBean.getRawClazz().getName(), declWarpBean.getNodeId(), SerialsUtil.generateShortUUID()))
                    .implement(declWarpBean.getRawClazz().getInterfaces())
                    .method(ElementMatchers.namedOneOf(declWarpBean.getMethodWrapBeanList().stream().map(wrap -> wrap.getLiteflowMethod().value().getMethodName()).toArray(String[]::new)))
                    .intercept(InvocationHandlerAdapter.of(new AopInvocationHandler(declWarpBean)))
                    .annotateType(beanClassAnnotation)
                    .make()
                    .load(DeclComponentProxy.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded()
                    .newInstance();
            NodeComponent nodeComponent = (NodeComponent) instance;
            // 重设nodeId
            nodeComponent.setNodeId(declWarpBean.getNodeId());
            // 重设nodeName
            nodeComponent.setName(declWarpBean.getNodeName());
            return nodeComponent;
        }
        catch (Exception e) {
            throw new ProxyException(e);
        }
    }

    public class AopInvocationHandler implements InvocationHandler {

        private final DeclWarpBean declWarpBean;

        public AopInvocationHandler(DeclWarpBean declWarpBean) {
            this.declWarpBean = declWarpBean;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 获取当前调用方法，是否在被代理的对象方法里面(根据@LiteFlowMethod这个标注去判断)
            // 如果在里面，则返回那个LiteFlowMethodBean，不在则返回null
            MethodWrapBean currentMethodWrapBean = declWarpBean.getMethodWrapBeanList().stream()
                    .filter(methodWrapBean -> methodWrapBean.getLiteflowMethod().value().getMethodName().equals(method.getName()))
                    .findFirst()
                    .orElse(null);

            if (currentMethodWrapBean == null){
                throw new ProxyException("currentMethodWrapBean is null");
            }

            // 如果被代理的对象里有此标注标的方法，则调用此被代理的对象里的方法，如果没有，则调用父类里的方法
            // 进行检查，检查被代理的bean里是否第一个参数为NodeComponent这个类型的
            boolean checkFlag = currentMethodWrapBean.getMethod().getParameterTypes().length > 0
                    && currentMethodWrapBean.getMethod().getParameterTypes()[0].equals(NodeComponent.class);
            if (!checkFlag) {
                String errMsg = StrUtil.format(
                        "Method[{}.{}] must have NodeComponent parameter(first parameter is NodeComponent)",
                        declWarpBean.getRawClazz().getName(), currentMethodWrapBean.getMethod().getName());
                LOG.error(errMsg);
                throw new ComponentMethodDefineErrorException(errMsg);
            }


            // 这里是针对于参数的处理
            // 首先需要保证第一个参数是NodeComponent
            // 其次需要针对于@LiteflowFact做处理
            try {
                Object[] realArgs = loadMethodParameter(proxy, currentMethodWrapBean, args);
                return ReflectUtil.invoke(declWarpBean.getRawBean(), currentMethodWrapBean.getMethod(), realArgs);
            }catch (InvocationTargetRuntimeException e) {
                InvocationTargetException targetEx = (InvocationTargetException) e.getCause();
                throw targetEx.getTargetException();
            }
        }

    }

    private Object[] loadMethodParameter(Object proxy, MethodWrapBean methodWrapBean, Object[] args){
        NodeComponent thisNodeComponent = (NodeComponent) proxy;

        return IntStream.range(0, methodWrapBean.getParameterWrapBeanList().size()).boxed().map(index -> {
            ParameterWrapBean parameterWrapBean = methodWrapBean.getParameterWrapBeanList().get(index);

            // 如果参数是NodeComponent，那就返回proxy本身
            if (parameterWrapBean.getParameterType().isAssignableFrom(NodeComponent.class)) {
                return proxy;
            }

            if (parameterWrapBean.getFact() == null && ArrayUtil.isNotEmpty(args)){
                if (parameterWrapBean.getParameterType().isAssignableFrom(args[index - 1].getClass())){
                    return args[index -1];
                }
            }

            // 如果没有@LiteflowFact标注，那么不处理，直接赋值null
            if (parameterWrapBean.getFact() == null) {
                return null;
            }

            return LiteflowContextRegexMatcher.searchContext(
                    DataBus.getSlot(thisNodeComponent.getSlotIndex()).getContextBeanList(),
                    parameterWrapBean.getFact().value());
        }).toArray();
    }
}
