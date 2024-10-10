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

    private static final List<PostProcessAfterScriptEngineInitLifeCycle> postProcessAfterScriptEngineInitLifeCycleList = new ArrayList<>();

    private static final List<PostProcessAfterChainBuildLifeCycle> postProcessAfterChainBuildLifeCycleList = new ArrayList<>();

    private static final List<PostProcessAfterNodeBuildLifeCycle> postProcessAfterNodeBuildLifeCycleList = new ArrayList<>();


    public static void addLifeCycle(LifeCycle lifeCycle){
        if (PostProcessAfterScriptEngineInitLifeCycle.class.isAssignableFrom(lifeCycle.getClass())){
            postProcessAfterScriptEngineInitLifeCycleList.add((PostProcessAfterScriptEngineInitLifeCycle)lifeCycle);
        }else if(PostProcessAfterChainBuildLifeCycle.class.isAssignableFrom(lifeCycle.getClass())){
            postProcessAfterChainBuildLifeCycleList.add((PostProcessAfterChainBuildLifeCycle)lifeCycle);
        }else if(PostProcessAfterNodeBuildLifeCycle.class.isAssignableFrom(lifeCycle.getClass())){
            postProcessAfterNodeBuildLifeCycleList.add((PostProcessAfterNodeBuildLifeCycle)lifeCycle);
        }
    }

    public static List<PostProcessAfterScriptEngineInitLifeCycle> getPostProcessAfterScriptEngineInitLifeCycleList() {
        return postProcessAfterScriptEngineInitLifeCycleList;
    }

    public static List<PostProcessAfterChainBuildLifeCycle> getPostProcessAfterChainBuildLifeCycleList() {
        return postProcessAfterChainBuildLifeCycleList;
    }

    public static List<PostProcessAfterNodeBuildLifeCycle> getPostProcessAfterNodeBuildLifeCycleList() {
        return postProcessAfterNodeBuildLifeCycleList;
    }
}
