package com.yomahub.liteflow.script.pythonjep;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.ScriptExecutor;
import com.yomahub.liteflow.script.exception.ScriptLoadException;
import com.yomahub.liteflow.util.CopyOnWriteHashMap;
import jep.Interpreter;
import jep.JepConfig;

import java.util.*;
import java.util.stream.Collectors;

public class PythonJepExecutor  extends ScriptExecutor {
    private static final String RESULT_KEY = "result";

    private final Map<String, String> compiledScriptMap = new CopyOnWriteHashMap<>();

    @Override
    public void load(String nodeId, String script) {
        try {
            compiledScriptMap.put(nodeId, (String) compile(script));
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
    public Object executeScript(ScriptExecuteWrap wrap) {
        String source = compiledScriptMap.get(wrap.getNodeId());
        if (source == null) {
            String errorMsg = StrUtil.format("script for node[{}] is not loaded", wrap.getNodeId());
            throw new ScriptLoadException(errorMsg);
        }

        try (Interpreter interpreter = JepRuntime.createInterpreter(
                new JepConfig().addSharedModules("numpy"))) {
            Set<String> boundNames = new HashSet<>();
            bindParam(wrap, (name, value) -> {
                interpreter.set(name, value);
                boundNames.add(name);
            }, (name, value) -> {
                if (boundNames.add(name)) {
                    interpreter.set(name, value);
                }
            });

            interpreter.exec(source);
            if (!interpreter.getValue("'" + RESULT_KEY + "' in globals()", Boolean.class)) {
                return null;
            }

            switch (wrap.getCmp().getType()) {
                case BOOLEAN_SCRIPT:
                    return interpreter.getValue(RESULT_KEY, Boolean.class);
                case FOR_SCRIPT:
                    return interpreter.getValue(RESULT_KEY, Integer.class);
                default:
                    return interpreter.getValue(RESULT_KEY);
            }
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
    public Object compile(String script) {
        String source = convertScript(script);
        try (Interpreter interpreter = JepRuntime.createInterpreter(
                new JepConfig().addSharedModules("numpy"))) {
            interpreter.set("__yth_script", source);
            interpreter.exec("compile(__yth_script, '<liteflow>', 'exec')");
        }
        return source;
    }

    private String convertScript(String script) {
        String[] lineArray = script.split("\\n");
        List<String> noBlankLineList = Arrays.stream(lineArray)
                .filter(s -> !CharSequenceUtil.isBlank(s))
                .collect(Collectors.toList());
        if (noBlankLineList.isEmpty()) {
            return StrUtil.EMPTY;
        }

        String blankStr = ReUtil.getGroup0("^[ ]*", noBlankLineList.get(0));
        StringBuilder scriptSB = new StringBuilder();
        noBlankLineList.forEach(
                s -> scriptSB.append(StrUtil.format("{}\n", s.replaceFirst(blankStr, StrUtil.EMPTY))));

        return scriptSB.toString().replaceAll("(?m)^return\\b", RESULT_KEY + "=");
    }
}
