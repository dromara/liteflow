package com.yomahub.liteflow.core.proxy;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.*;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.exception.ComponentMethodDefineErrorException;
import com.yomahub.liteflow.exception.LiteFlowException;
import com.yomahub.liteflow.util.LiteFlowProxyUtil;
import com.yomahub.liteflow.util.SerialsUtil;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
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
        Map<String, List<LiteflowMethod>> methodListMap = Arrays.stream(beanClazz.getDeclaredMethods()).filter(
                m -> m.getAnnotation(LiteflowMethod.class) != null
        ).map(m -> m.getAnnotation(LiteflowMethod.class)).collect(Collectors.groupingBy(LiteflowMethod::nodeId));
        return methodListMap.entrySet().stream().map(entry -> {
            String activeNodeId = StrUtil.isEmpty(entry.getKey()) ? nodeId : entry.getKey();
            List<LiteflowMethod> methodList = entry.getValue();
            try {
                //创建对象
                //这里package进行了重设，放到了被代理对象的所在目录
                //生成的对象也加了上被代理对象拥有的注解
                //被拦截的对象也根据被代理对象根据@LiteFlowMethod所标注的进行了动态判断
                Object instance = new ByteBuddy().subclass(clazz)
                        .name(StrUtil.format("{}.ByteBuddy${}${}",
                                ClassUtil.getPackage(bean.getClass()),
                                activeNodeId,
                                SerialsUtil.generateShortUUID()))
                        .method(ElementMatchers.namedOneOf(methodList.stream().map(m -> m.value().getMethodName()).toArray(String[]::new)))
                        .intercept(InvocationHandlerAdapter.of(new AopInvocationHandler(bean)))
                        .annotateType(bean.getClass().getAnnotations())
                        .make()
                        .load(ComponentProxy.class.getClassLoader())
                        .getLoaded()
                        .newInstance();
                NodeComponent nodeComponent = (NodeComponent) instance;
                nodeComponent.setNodeId(activeNodeId);
                return nodeComponent;
            } catch (Exception e) {
                throw new LiteFlowException(e);
            }
        }).collect(Collectors.toList());
    }

    public class AopInvocationHandler implements InvocationHandler {

        private final Object bean;

        public AopInvocationHandler(Object bean) {
            this.bean = bean;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            //这里做了2件事情
            //先是从普通的bean里过滤出含有@LiteFlowMethod这个标注的方法
            //然后进行转换成LiteFlowMethodBean对象List,形成<methodName,Method>键值对的对象
            List<LiteFlowMethodBean> liteFlowMethodBeanList = Arrays.stream(ReflectUtil.getMethods(bean.getClass())).filter(m -> {
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
            //beforeProcess和afterProcess这2个方法除外
            if (!ListUtil.toList("beforeProcess","afterProcess").contains(liteFlowMethodBean.getMethodName())) {
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
            try{
                return liteFlowMethodBean.getMethod().invoke(bean, args);
            }catch (Exception e){
                InvocationTargetException targetEx = (InvocationTargetException)e;
                throw targetEx.getTargetException();
            }
        }
    }
}
