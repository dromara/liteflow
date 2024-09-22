package com.yomahub.liteflow.test.script.javax.common.cmp;

import cn.hutool.core.collection.ListUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.ToIntFunction;

@Component
public class TestDomain {

    public String sayHello(String name){
        return "hello," + name;
    }

    public static void main(String[] args) {
        List<Person> personList = ListUtil.toList(
                new Person("jack", 15000),
                new Person("tom", 13500),
                new Person("peter", 18600)
                );

        int totalSalary = personList.stream().mapToInt(Person::getSalary).sum();
        System.out.println(totalSalary);
    }
}
