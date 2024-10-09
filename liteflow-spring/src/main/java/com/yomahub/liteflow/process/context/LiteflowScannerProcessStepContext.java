package com.yomahub.liteflow.process.context;

/**
 * 上下文
 *
 * @author tkc
 * @since 2.12.4
 */
public class LiteflowScannerProcessStepContext {
    /**
     * 原始 bean
     */
    private Object bean;
    /**
     * bean 名称
     */
    private String beanName;
    /**
     * bean class
     */
    private Class clazz;

    /**
     * 前置判断的输出结果，可为空
     */
    private Object outPut;

    public Object getOutPut() {
        return outPut;
    }

    public void setOutPut(Object outPut) {
        this.outPut = outPut;
    }

    public Class getClazz() {
        return clazz;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public Object getBean() {
        return bean;
    }

    public void setBean(Object bean) {
        this.bean = bean;
    }

    public static LiteflowScannerProcessStepContext of(Object bean, String beanName, Class clazz) {
        LiteflowScannerProcessStepContext context = new LiteflowScannerProcessStepContext();
        context.setBean(bean);
        context.setBeanName(beanName);
        context.setClazz(clazz);
        return context;
    }
}
