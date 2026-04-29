package com.yomahub.liteflow.test.agent.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

/**
 * 自定义工具示例：计算器。被 ReActAgentComponent.tools() 注册后，
 * agent 在推理时会被 toolkit 自动发现并按需调用。
 */
public class CalculatorTool {

    @Tool(name = "calculator", description = "Evaluate a basic arithmetic expression like '1+2*3'")
    public String calc(@ToolParam(name = "expression",
            description = "Arithmetic expression with +, -, *, /, and parentheses") String expression) {
        try {
            return String.valueOf(Eval.run(expression));
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /** 极简递归下降表达式求值器（仅供示例，不保证健壮性）。 */
    static final class Eval {
        private final String s;
        private int pos;
        private Eval(String s) { this.s = s; }

        static double run(String s) {
            Eval e = new Eval(s.replaceAll("\\s+", ""));
            double v = e.expr();
            if (e.pos < e.s.length()) throw new IllegalArgumentException("Unexpected: " + e.s.substring(e.pos));
            return v;
        }
        private double expr() {
            double v = term();
            while (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) {
                char op = s.charAt(pos++);
                double r = term();
                v = (op == '+') ? v + r : v - r;
            }
            return v;
        }
        private double term() {
            double v = factor();
            while (pos < s.length() && (s.charAt(pos) == '*' || s.charAt(pos) == '/')) {
                char op = s.charAt(pos++);
                double r = factor();
                v = (op == '*') ? v * r : v / r;
            }
            return v;
        }
        private double factor() {
            if (pos < s.length() && s.charAt(pos) == '(') {
                pos++;
                double v = expr();
                if (pos >= s.length() || s.charAt(pos) != ')') throw new IllegalArgumentException("Missing )");
                pos++;
                return v;
            }
            int start = pos;
            if (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) pos++;
            while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) pos++;
            if (start == pos) throw new IllegalArgumentException("Number expected at " + pos);
            return Double.parseDouble(s.substring(start, pos));
        }
    }
}
