package com.yomahub.liteflow.builder.el;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.el.vo.RetryELVo;
import com.yomahub.liteflow.util.JsonUtil;
import com.yomahub.liteflow.util.SelectiveJavaEscaper;

import java.util.*;

/**
 * ELWrapper是所有组件的抽象父类
 * 定义所有EL表达式的公有变量 tag、id、data、maxWaitSeconds 以及 子表达式列表 ELWrapperList
 *
 * @author gezuao
 * @author Bryan.Zhang
 * @since 2.11.1
 */
public abstract class ELWrapper {

    private final List<ELWrapper> elWrapperList = new ArrayList<>();

    private String tag;
    private String id;
    private String dataName;
    private String data;
    private final Map<String, String> bindData = new HashMap<>();
    private Integer maxWaitSeconds;
    private RetryELVo retry;

    protected void addWrapper(ELWrapper wrapper){
        this.elWrapperList.add(wrapper);
    }

    protected void addWrapper(ELWrapper... wrappers){
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

    protected String getBindData(String key) {
        return bindData.get(key);
    }

    protected void putBindData(String key, String value) {
        this.bindData.put(key, value);
    }

    protected Map<String, String> getBindData() {
        return bindData;
    }

    protected void setMaxWaitSeconds(Integer maxWaitSeconds){
        this.maxWaitSeconds = maxWaitSeconds;
    }

    protected Integer getMaxWaitSeconds(){
        return this.maxWaitSeconds;
    }

    protected RetryELVo getRetry() {
        return retry;
    }

    protected void setRetry(RetryELVo retry) {
        this.retry = retry;
    }

    /**
     * 设置组件标记内容
     *
     * @param tag 标记内容
     * @return {@link ELWrapper}
     */
    public ELWrapper tag(String tag){
        this.setTag(tag);
        return this;
    }

    /**
     * 设置组件的id
     *
     * @param id 编号
     * @return {@link ELWrapper}
     */
    public ELWrapper id(String id){
        this.setId(id);
        return this;
    }

    /**
     * 设置表达式data属性
     *
     * @param dataName 数据名称
     * @param object   JavaBean
     * @return {@link ELWrapper}
     */
    protected ELWrapper data(String dataName, Object object){
        setData(StrUtil.format("\"{}\"", SelectiveJavaEscaper.escape(JsonUtil.toJsonString(object))));
        setDataName(dataName);
        return this;
    }

    /**
     * 设置表达式data属性
     *
     * @param dataName   数据名称
     * @param jsonString JSON格式字符串
     * @return {@link ELWrapper}
     */
    protected ELWrapper data(String dataName, String jsonString){
        setData(StrUtil.format("\"{}\"",SelectiveJavaEscaper.escape(jsonString)));
        setDataName(dataName);
        return this;
    }

    /**
     * 设置data属性
     *
     * @param dataName 数据名称
     * @param jsonMap  键值映射
     * @return {@link ELWrapper}
     */
    protected ELWrapper data(String dataName, Map<String, Object> jsonMap){
        setData(StrUtil.format("\"{}\"", SelectiveJavaEscaper.escape(JsonUtil.toJsonString(jsonMap))));
        setDataName(dataName);
        return this;
    }

    protected ELWrapper bind(String key, String value){
        putBindData(key, value);
        return this;
    }


    protected ELWrapper maxWaitSeconds(Integer maxWaitSeconds){
        setMaxWaitSeconds(maxWaitSeconds);
        return this;
    }

    protected ELWrapper retry(int count){
        RetryELVo item = new RetryELVo(count);
        setRetry(item);
        return this;
    }

    protected ELWrapper retry(int count, String... exceptions){
        RetryELVo item = new RetryELVo(count, exceptions);
        setRetry(item);
        return this;
    }

    /**
     * 非格式化输出EL表达式
     *
     * @return {@link String}
     */
    public String toEL(){
        return toEL(false);
    }

    /**
     * 是否格式化输出树形结构的表达式
     *
     * @param format 格式
     * @return {@link String}
     */
    public String toEL(boolean format){
        StringBuilder paramContext = new StringBuilder();
        String elContext;
        if(!format){
            elContext = toEL(null, paramContext);
        } else {
            elContext = toEL(0, paramContext);
        }
        return paramContext.append(elContext).append(";").toString();
    }

    /**
     * 格式化输出EL表达式
     *
     * @param depth        深度
     * @param paramContext 参数上下文，用于输出data参数内容
     * @return {@link String}
     */
    protected abstract String toEL(Integer depth, StringBuilder paramContext);

    /**
     * 处理EL表达式的共有属性
     *
     * @param elContext    EL表达式上下文
     * @param paramContext 参数上下文
     */
    protected void processWrapperProperty(StringBuilder elContext, StringBuilder paramContext){
        if(this.getId() != null){
            elContext.append(StrUtil.format(".id(\"{}\")", this.getId()));
        }
        if(this.getTag() != null){
            elContext.append(StrUtil.format(".tag(\"{}\")", SelectiveJavaEscaper.escape(this.getTag())));
        }
        if(this.getData() != null){
            elContext.append(StrUtil.format(".data({})", this.getDataName()));
            paramContext.append(StrUtil.format("{} = {}", this.getDataName(), this.getData())).append(";\n");
        }
        if(MapUtil.isNotEmpty(this.getBindData())){
            this.getBindData().forEach((key, value) -> elContext.append(StrUtil.format(".bind(\"{}\", \"{}\")", key, SelectiveJavaEscaper.escape(value))));
        }
        if(this.getMaxWaitSeconds() != null){
            elContext.append(StrUtil.format(".maxWaitSeconds({})", String.valueOf(this.getMaxWaitSeconds())));
        }
        if (this.getRetry() != null){
            elContext.append(StrUtil.format(".retry({})", this.getRetry().toString()));
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
