package com.thebeastshop.liteflow.parser;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 类型转换
 * @author gongjun[jun.gong@thebeastshop.com]
 * @since 2017-11-22 15:13
 */
@Component
public class TempConvert {



    private static List<String> match(String input) {
        List<String> list = new ArrayList<String>();
        Stack<Character> stack = new Stack<>();
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '(') {
                stack.push(c);
                if (stack.size() == 1 && buffer.length() > 0) {
                    list.add(buffer.toString());
                    buffer = new StringBuffer();
                }else {
                    buffer.append(c);
                }
            }else if (c == ')') {
                if (stack.size() > 0) {
                    stack.pop();
                    if (stack.size() == 0) {
                        if (buffer.length() > 0) {
                            list.add(buffer.toString());
                            buffer = new StringBuffer();
                        }
                    }else {
                        buffer.append(c);
                    }
                }
            }else {
                buffer.append(c);
            }
        }
        if (buffer.length() > 0) {
            list.add(buffer.toString());
        }
        return list;
    }


    public static void main(String[] args) {
        List<String> list = new ArrayList<String>();
        String input = "aaaa(bbb(xxxxx|yyyy))";
        list = match(input);
        System.out.println(list);
    }
}
