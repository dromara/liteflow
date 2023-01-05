package com.yomahub.liteflow.core.proxy;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.annotation.LiteflowRetry;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.ComponentMethodDefineErrorException;
import com.yomahub.liteflow.exception.LiteFlowException;
import com.yomahub.liteflow.util.LiteFlowProxyUtil;
import com.yomahub.liteflow.util.SerialsUtil;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 声明式组件的代理核心生成类
 * @author Bryan.Zhang
 * @since 2.6.14
 */
public class ComponentProxy {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private final String nodeId;

    private final Object bean;

    private final Class<?> clazz;

    public ComponentProxy(String nodeId, Object bean, Class<?> clazz) {
        this.nodeId = nodeId;
        this.bean = bean;
        this.clazz = clazz;
    }

    public List<NodeComponent> getProxyList() throws Exception{
        //这里要判断bean是否是spring代理过的bean，如果是代理过的bean需要取到原class对象
        Class<?> beanClazz;
        if (LiteFlowProxyUtil.isCglibProxyClass(bean.getClass())){
            beanClazz = LiteFlowProxyUtil.getUserClass(bean.getClass());
        }else{
            beanClazz = bean.getClass();
        }
        //得到当前bean里所覆盖的LiteflowMethod(一定是被@LiteFlowMethod修饰的)，自己定义的不算
        Map<String, List<Method>> methodListMap = Arrays.stream(beanClazz.getMethods()).filter(
                m -> m.getAnnotation(LiteflowMethod.class) != null
        ).collect(Collectors.groupingBy(
                m -> m.getAnnotation(LiteflowMethod.class).nodeId()
        ));

        return methodListMap.entrySet().stream().map(entry -> {
            // 获取当前节点的原有注解，如：LiteFlowRetry 之类的规则注解
            Annotation[] beanClassAnnotation = beanClazz.getAnnotations();
            // 如果entry的key为空字符串，则是为了兼容老版本的写法，即：没有指定nodeId的情况
            // 判断是否是方法级创造节点
            boolean isMethodCreate = !StrUtil.isEmpty(entry.getKey());
            // 获取当前bean 真实的nodeId
            String activeNodeId = isMethodCreate ? entry.getKey() : nodeId;
            // 获取当前节点所有的@LiteflowRetry @LiteflowMethod注解对
            List<Tuple> tupleList = entry.getValue().stream().map(m ->
                    new Tuple(m.getAnnotation(LiteflowRetry.class), m.getAnnotation(LiteflowMethod.class))
            ).collect(Collectors.toList());
            // 获取当前节点的所有LiteFlowMethod注解
            List<LiteflowMethod> methodList = tupleList.stream().map(tuple -> ((LiteflowMethod)tuple.get(1)))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            // nodeType去重
            List<? extends Class<? extends NodeComponent>> classes = methodList.stream()
                    .map(LiteflowMethod::nodeType)
                    .map(NodeTypeEnum::getMappingClazz)
                    .distinct()
                    .collect(Collectors.toList());
            // 相同nodeId里只能定义同一种的类型的NodeComponent
            boolean legal = classes.size() == 1;
            if (!legal){
                throw new LiteFlowException("The cmpClass of the same nodeId must be the same,you declared nodeId:" + activeNodeId + ",cmpClass:" + classes);
            }
            // 当前节点实际LiteflowRetry注解
            AtomicReference<LiteflowRetry> liteflowRetryAtomicReference = new AtomicReference<>(null);
            // 相同nodeId只能有一个LiteflowRetry定义方法,且必须再Process方法上
            boolean illegal = tupleList.stream().anyMatch(
                    tuple -> {
                        LiteflowRetry liteflowRetry = tuple.get(0);
                        LiteflowMethod liteflowMethod = tuple.get(1);
                        boolean existRetry = liteflowRetry != null;
                        boolean isProcess = liteflowMethod.value().isMainMethod();
                        // 如果是再Process方法上的liteflowRetry注解，则默认为真实节点。
                        if (isProcess && existRetry) {
                            liteflowRetryAtomicReference.set(liteflowRetry);
                        }
                        // 如果存在existRetry注解，但是不是在Process方法上，则为非法
                        return existRetry && !isProcess;
                    }
            );
            if (illegal){
                throw new LiteFlowException("the retry annotation (@LiteflowRetry) must be declared on the PROCESS method");
            }
            // 生成nodeCmp的类型，默认为全局定义的clazz
            Class<?> cmpClazz;
            cmpClazz = clazz;
            // 判断是否是方法声明的组件
            if (isMethodCreate){
                cmpClazz = methodList.iterator().next().nodeType().getMappingClazz();
                LiteflowRetry liteflowRetry;
                if ((liteflowRetry = liteflowRetryAtomicReference.get()) != null){
                    // 增加LiteFlowRetry注解到注解数组里
                    List<Annotation> annotations = Arrays.stream(beanClassAnnotation)
                            .filter(a -> !a.annotationType().equals(LiteflowRetry.class))
                            .collect(Collectors.toList());
                    annotations.add(liteflowRetry);
                    beanClassAnnotation = new Annotation[annotations.size()];
                    annotations.toArray(beanClassAnnotation);
                }
            }
            try {
                //创建对象
                //这里package进行了重设，放到了被代理对象的所在目录
                //生成的对象也加了上被代理对象拥有的注解
                //被拦截的对象也根据被代理对象根据@LiteFlowMethod所标注的进行了动态判断
                Object instance = new ByteBuddy().subclass(cmpClazz)
                        .name(StrUtil.format("{}.ByteBuddy${}${}",
                                ClassUtil.getPackage(beanClazz),
                                activeNodeId,
                                SerialsUtil.generateShortUUID()))
                        .method(ElementMatchers.namedOneOf(methodList.stream().map(m -> m.value().getMethodName()).toArray(String[]::new)))
                        .intercept(InvocationHandlerAdapter.of(new AopInvocationHandler(bean)))
                        .annotateType(beanClassAnnotation)
                        .make()
                        .load(ComponentProxy.class.getClassLoader())
                        .getLoaded()
                        .newInstance();
                NodeComponent nodeComponent = (NodeComponent) instance;
                // 重设nodeId
                nodeComponent.setNodeId(activeNodeId);
                return nodeComponent;
            } catch (Exception e) {
                throw new LiteFlowException(e);
            }
        }).collect(Collectors.toList());
    }

    public class AopInvocationHandler implements InvocationHandler {

        private final Object bean;

        private final Class<?> clazz;

        public AopInvocationHandler(Object bean) {
            this.bean = bean;
            this.clazz = LiteFlowProxyUtil.getUserClass(bean.getClass());
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            //这里做了2件事情
            //先是从普通的bean里过滤出含有@LiteFlowMethod这个标注的方法
            //然后进行转换成LiteFlowMethodBean对象List,形成<methodName,Method>键值对的对象
            List<LiteFlowMethodBean> liteFlowMethodBeanList = Arrays.stream(ReflectUtil.getMethods(clazz)).filter(m -> {
                LiteflowMethod liteFlowMethod = m.getAnnotation(LiteflowMethod.class);
                return ObjectUtil.isNotNull(liteFlowMethod);
            }).filter(m -> {
                // 过滤不属于当前NodeComponent的方法
                LiteflowMethod liteFlowMethod = m.getAnnotation(LiteflowMethod.class);
                return StrUtil.isEmpty(liteFlowMethod.nodeId())|| Objects.equals(liteFlowMethod.nodeId(),((NodeComponent) proxy).getNodeId());
            }).map(m -> {
                LiteflowMethod liteFlowMethod = m.getAnnotation(LiteflowMethod.class);
                return new LiteFlowMethodBean(liteFlowMethod.value().getMethodName(), m);
            }).collect(Collectors.toList());

            //获取当前调用方法，是否在被代理的对象方法里面(根据@LiteFlowMethod这个标注去判断)
            //如果在里面，则返回那个LiteFlowMethodBean，不在则返回null
            LiteFlowMethodBean liteFlowMethodBean = liteFlowMethodBeanList.stream().filter(
                    liteFlowMethodBean1 -> liteFlowMethodBean1.getMethodName().equals(method.getName())
            ).findFirst().orElse(null);

            //如果被代理的对象里有此标注标的方法，则调用此被代理的对象里的方法，如果没有，则调用父类里的方法
            //进行检查，检查被代理的bean里是否有且仅有NodeComponent这个类型的参数
            boolean checkFlag = liteFlowMethodBean.getMethod().getParameterTypes().length == 1
                    && Arrays.asList(liteFlowMethodBean.getMethod().getParameterTypes()).contains(NodeComponent.class);
            if (!checkFlag) {
                String errMsg = StrUtil.format("Method[{}.{}] must have NodeComponent parameter(and only one parameter)", bean.getClass().getName(), liteFlowMethodBean.getMethod().getName());
                LOG.error(errMsg);
                throw new ComponentMethodDefineErrorException(errMsg);
            }

            try{
                return liteFlowMethodBean.getMethod().invoke(bean, proxy);
            }catch (Exception e){
                InvocationTargetException targetEx = (InvocationTargetException)e;
                throw targetEx.getTargetException();
            }
        }
    }
}
