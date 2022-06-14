package com.yomahub.liteflow.core.proxy;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.*;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.exception.ComponentMethodDefineErrorException;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    public Object getProxy() throws Exception{
        //这里要判断bean是否是spring代理过的bean，如果是代理过的bean需要取到原class对象
        Class<?> beanClazz;
        if (LiteFlowProxyUtil.isCglibProxyClass(bean.getClass())){
            beanClazz = LiteFlowProxyUtil.getUserClass(bean.getClass());
        }else{
            beanClazz = bean.getClass();
        }

        //得到当前bean里所覆盖的组件方法(一定是被@LiteFlowMethod修饰的)，自己定义的不算
        List<String> methodStrList = Arrays.stream(beanClazz.getDeclaredMethods()).filter(
                m -> m.getAnnotation(LiteflowMethod.class) != null
        ).map(m -> {
            LiteflowMethod liteflowMethod = m.getAnnotation(LiteflowMethod.class);
            return liteflowMethod.value().getMethodName();
        }).collect(Collectors.toList());

        //创建对象
        //这里package进行了重设，放到了被代理对象的所在目录
        //生成的对象也加了上被代理对象拥有的注解
        //被拦截的对象也根据被代理对象根据@LiteFlowMethod所标注的进行了动态判断
        return new ByteBuddy().subclass(clazz)
                .name(StrUtil.format("{}.ByteBuddy${}${}",
                        ClassUtil.getPackage(bean.getClass()),
                        nodeId,
                        SerialsUtil.generateShortUUID()))
                .method(ElementMatchers.namedOneOf(methodStrList.toArray(new String[]{})))
                .intercept(InvocationHandlerAdapter.of(new AopInvocationHandler(bean)))
                .annotateType(bean.getClass().getAnnotations())
                .make()
                .load(ComponentProxy.class.getClassLoader())
                .getLoaded()
                .newInstance();
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
