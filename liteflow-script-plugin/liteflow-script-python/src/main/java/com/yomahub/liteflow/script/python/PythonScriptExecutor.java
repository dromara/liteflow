package com.yomahub.liteflow.script.python;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.ScriptExecutor;
import com.yomahub.liteflow.script.exception.ScriptLoadException;
import org.python.core.PyCode;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Python脚本语言的执行器实现
 *
 * @author Bryan.Zhang
 * @since 2.12.0
 */
public class PythonScriptExecutor extends ScriptExecutor {

    private PythonInterpreter pythonInterpreter;

    private final String RESULT_KEY = "result";

    private final Map<String, PyCode> compiledScriptMap = new HashMap<>();


    @Override
    public ScriptExecutor init(){
        PySystemState systemState = new PySystemState();
        systemState.setdefaultencoding("UTF-8");
        this.pythonInterpreter = new PythonInterpreter(null, systemState);
        //如果有生命周期则执行相应生命周期实现
        super.lifeCycle(pythonInterpreter);
        return this;
    }

    @Override
    public void load(String nodeId, String script) {
        try {
            PyCode pyCode = (PyCode) compile(script);
            compiledScriptMap.put(nodeId, pyCode);
        }
        catch (Exception e) {
            String errorMsg = StrUtil.format("script loading error for node[{}],error msg:{}", nodeId, e.getMessage());
            throw new ScriptLoadException(errorMsg);
        }
    }

    @Override
    public void unLoad(String nodeId) {
        compiledScriptMap.remove(nodeId);
    }

    @Override
    public List<String> getNodeIds() {
        return new ArrayList<>(compiledScriptMap.keySet());
    }

    @Override
    public Object executeScript(ScriptExecuteWrap wrap) throws Exception {
        if (!compiledScriptMap.containsKey(wrap.getNodeId())) {
            String errorMsg = StrUtil.format("script for node[{}] is not loaded", wrap.getNodeId());
            throw new ScriptLoadException(errorMsg);
        }

        PyCode compiledScript = compiledScriptMap.get(wrap.getNodeId());

        bindParam(wrap, pythonInterpreter::set, pythonInterpreter::set);

        pythonInterpreter.exec(compiledScript);

        PyObject result = pythonInterpreter.get(RESULT_KEY);

        if (result == null){
            return null;
        }

        pythonInterpreter.cleanup();

        switch (wrap.getCmp().getType()){
            case BOOLEAN_SCRIPT:
                return result.__tojava__(Boolean.class);
            case FOR_SCRIPT:
                return result.__tojava__(Integer.class);
            default:
                return result.__tojava__(Object.class);
        }
    }

    @Override
    public void cleanCache() {
        compiledScriptMap.clear();
    }

    @Override
    public ScriptTypeEnum scriptType() {
        return ScriptTypeEnum.PYTHON;
    }

    @Override
    public Object compile(String script) throws Exception {
        return pythonInterpreter.compile(convertScript(script));
    }

    private String convertScript(String script) {
        String[] lineArray = script.split("\\n");
        List<String> noBlankLineList = Arrays.stream(lineArray)
                .filter(s -> !StrUtil.isBlank(s))
                .collect(Collectors.toList());

        // 用第一行的缩进的空格数作为整个代码的缩进量
        String blankStr = ReUtil.getGroup0("^[ ]*", noBlankLineList.get(0));

        // 重新构建脚本
        StringBuilder scriptSB = new StringBuilder();
        noBlankLineList.forEach(s -> scriptSB.append(StrUtil.format("{}\n", s.replaceFirst(blankStr, StrUtil.EMPTY))));

        return scriptSB.toString().replace("return", RESULT_KEY + "=");
    }
}
