package com.yomahub.liteflow.lifecycle;

import java.util.ArrayList;
import java.util.List;

/**
 * 生命周期接口持有器
 * 经过启动后把相应的生命周期接口实现类扫入这个持有器中
 *
 * @author Bryan.Zhang
 * @since 2.12.4
 */
public class LifeCycleHolder {

    private static final List<PostProcessScriptEngineInitLifeCycle> POST_PROCESS_SCRIPT_ENGINE_INIT_LIFE_CYCLE_LIST = new ArrayList<>();

    private static final List<PostProcessChainBuildLifeCycle> POST_PROCESS_CHAIN_BUILD_LIFE_CYCLE_LIST = new ArrayList<>();

    private static final List<PostProcessNodeBuildLifeCycle> POST_PROCESS_NODE_BUILD_LIFE_CYCLE_LIST = new ArrayList<>();

    private static final List<PostProcessFlowExecuteLifeCycle> POST_PROCESS_FLOW_EXECUTE_LIFE_CYCLE_LIST = new ArrayList<>();

    private static final List<PostProcessChainExecuteLifeCycle> POST_PROCESS_CHAIN_EXECUTE_LIFE_CYCLE_LIST = new ArrayList<>();


    public static void addLifeCycle(LifeCycle lifeCycle){
        if (PostProcessScriptEngineInitLifeCycle.class.isAssignableFrom(lifeCycle.getClass())){
            POST_PROCESS_SCRIPT_ENGINE_INIT_LIFE_CYCLE_LIST.add((PostProcessScriptEngineInitLifeCycle)lifeCycle);
        }else if(PostProcessChainBuildLifeCycle.class.isAssignableFrom(lifeCycle.getClass())){
            POST_PROCESS_CHAIN_BUILD_LIFE_CYCLE_LIST.add((PostProcessChainBuildLifeCycle)lifeCycle);
        }else if(PostProcessNodeBuildLifeCycle.class.isAssignableFrom(lifeCycle.getClass())){
            POST_PROCESS_NODE_BUILD_LIFE_CYCLE_LIST.add((PostProcessNodeBuildLifeCycle)lifeCycle);
        }else if(PostProcessFlowExecuteLifeCycle.class.isAssignableFrom(lifeCycle.getClass())){
            POST_PROCESS_FLOW_EXECUTE_LIFE_CYCLE_LIST.add((PostProcessFlowExecuteLifeCycle)lifeCycle);
        }else if(PostProcessChainExecuteLifeCycle.class.isAssignableFrom(lifeCycle.getClass())){
            POST_PROCESS_CHAIN_EXECUTE_LIFE_CYCLE_LIST.add((PostProcessChainExecuteLifeCycle)lifeCycle);
        }
    }

    public static List<PostProcessScriptEngineInitLifeCycle> getPostProcessScriptEngineInitLifeCycleList() {
        return POST_PROCESS_SCRIPT_ENGINE_INIT_LIFE_CYCLE_LIST;
    }

    public static List<PostProcessChainBuildLifeCycle> getPostProcessChainBuildLifeCycleList() {
        return POST_PROCESS_CHAIN_BUILD_LIFE_CYCLE_LIST;
    }

    public static List<PostProcessNodeBuildLifeCycle> getPostProcessNodeBuildLifeCycleList() {
        return POST_PROCESS_NODE_BUILD_LIFE_CYCLE_LIST;
    }

    public static List<PostProcessFlowExecuteLifeCycle> getPostProcessFlowExecuteLifeCycleList() {
        return POST_PROCESS_FLOW_EXECUTE_LIFE_CYCLE_LIST;
    }

    public static List<PostProcessChainExecuteLifeCycle> getPostProcessChainExecuteLifeCycleList() {
        return POST_PROCESS_CHAIN_EXECUTE_LIFE_CYCLE_LIST;
    }

    public static void clean(){
        POST_PROCESS_SCRIPT_ENGINE_INIT_LIFE_CYCLE_LIST.clear();
        POST_PROCESS_CHAIN_BUILD_LIFE_CYCLE_LIST.clear();
        POST_PROCESS_NODE_BUILD_LIFE_CYCLE_LIST.clear();
        POST_PROCESS_FLOW_EXECUTE_LIFE_CYCLE_LIST.clear();
        POST_PROCESS_CHAIN_EXECUTE_LIFE_CYCLE_LIST.clear();
    }
}
