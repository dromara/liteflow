package com.yomahub.liteflow.builder.el;

import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * ELWrapper是所有组件的抽象父类
 * 定义所有EL表达式的公有变量 tag、id、data、maxWaitSeconds 以及 子表达式列表 ELWrapperList
 *
 * @author gezuao
 * @since 2.11.1
 */
public abstract class ELWrapper {

    private final List<ELWrapper> elWrapperList = new ArrayList<>();

    private String tag;
    private String id;
    private String dataName;
    private String data;
    private Integer maxWaitSeconds;

    protected void addWrapper(ELWrapper wrapper){
        this.elWrapperList.add(wrapper);
    }

    protected void addWrapper(ELWrapper ... wrappers){
        this.elWrapperList.addAll(Arrays.asList(wrappers));
    }

    protected void addWrapper(ELWrapper wrapper, int index){
        this.elWrapperList.add(index, wrapper);
    }

    protected void setWrapper(ELWrapper wrapper, int index){
        this.elWrapperList.set(index, wrapper);
    }

    protected ELWrapper getFirstWrapper(){
        return this.elWrapperList.get(0);
    }

    protected List<ELWrapper> getElWrapperList(){
        return this.elWrapperList;
    }

    protected void setTag(String tag){
        this.tag = tag;
    }

    protected String getTag(){
        return this.tag;
    }

    protected void setId(String id){
        this.id = id;
    }

    protected String getId(){
        return this.id;
    }

    protected void setData(String data){
        this.data = data;
    }

    protected String getData(){
        return this.data;
    }

    protected void setDataName(String dataName){
        this.dataName = dataName;
    }

    protected String getDataName(){
        return this.dataName;
    }

    protected void setMaxWaitSeconds(Integer maxWaitSeconds){
        this.maxWaitSeconds = maxWaitSeconds;
    }

    protected Integer getMaxWaitSeconds(){
        return this.maxWaitSeconds;
    }

    protected abstract ELWrapper tag(String tag);

    protected abstract ELWrapper id(String id);

    /**
     * 设置data属性，参数为
     * @param dataName
     * @param object JavaBean
     * @return
     */
    protected abstract ELWrapper data(String dataName, Object object);

    /**
     * 设置data属性，参数为
     * @param dataName
     * @param jsonString json格式字符串
     * @return
     */
    protected abstract ELWrapper data(String dataName, String jsonString);

    /**
     * 设置data属性，参数为
     * @param dataName
     * @param jsonMap Map映射
     * @return
     */
    protected abstract ELWrapper data(String dataName, Map<String, Object> jsonMap);

    protected ELWrapper maxWaitSeconds(Integer maxWaitSeconds){
        setMaxWaitSeconds(maxWaitSeconds);
        return this;
    }

    /**
     * 输出EL表达式的默认方法
     * 非格式化输出EL表达式
     *
     * @return {@link String}
     */
    public String toEL(){
        StringBuilder paramContext = new StringBuilder();
        String ELContext = toEL(null, paramContext);
        return paramContext.append(ELContext).toString();
    }

    /**
     * 是否格式化输出树形结构的规则表达式
     *
     * @param format 格式
     * @return {@link String}
     */
    public String toEL(boolean format){
        StringBuilder paramContext = new StringBuilder();
        String ELContext;
        if(!format){
            ELContext = toEL(null, paramContext);
        } else {
            ELContext = toEL(0, paramContext);
        }
        return paramContext.append(ELContext).toString();
    }

    /**
     * 格式化输出EL表达式
     * @param depth 深度
     * @param paramContext 参数输出内容，用于输出data属性
     * @return
     */
    protected abstract String toEL(Integer depth, StringBuilder paramContext);

    /**
     * 处理EL表达式的属性
     *
     * @param elContext    EL 输出内容
     * @param paramContext 参数 输出内容
     */
    protected void processWrapperProperty(StringBuilder elContext, StringBuilder paramContext){
        if(this.getId() != null){
            elContext.append(StrUtil.format(".id(\"{}\")", this.getId()));
        }
        if(this.getTag() != null){
            elContext.append(StrUtil.format(".tag(\"{}\")", this.getTag()));
        }
        if(this.getData() != null){
            elContext.append(StrUtil.format(".data({})", this.getDataName()));
            paramContext.append(StrUtil.format("{} = '{}';\n", this.getDataName(), this.getData()));
        }
        if(this.getMaxWaitSeconds() != null){
            elContext.append(StrUtil.format(".maxWaitSeconds({})", String.valueOf(this.getMaxWaitSeconds())));
        }
    }

    /**
     * 处理格式化输出 EL表达式换行符
     *
     * @param elContext EL 上下文
     * @param depth     深度
     */
    protected void processWrapperNewLine(StringBuilder elContext, Integer depth){
        if(depth != null){
            elContext.append("\n");
        }
    }

    /**
     * 处理格式化输出 EL表达式制表符
     *
     * @param elContext EL 上下文
     * @param depth     深度
     */
    protected void processWrapperTabs(StringBuilder elContext, Integer depth){
        if(depth != null) {
            elContext.append(StrUtil.repeat(ELBus.TAB, depth));
        }
    }
}
