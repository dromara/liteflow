import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.script.javaxpro.common.cmp.Person;
import com.yomahub.liteflow.test.script.javaxpro.common.cmp.TestDomain;

import java.util.List;

public class Demo extends NodeComponent {
    @Override
    public void process() throws Exception {
        int v1 = 2;
        int v2 = 3;
        DefaultContext ctx = this.getFirstContextBean();
        ctx.setData("s1", v1 * v2);

        TestDomain domain = ContextAwareHolder.loadContextAware().getBean(TestDomain.class);
        System.out.println(domain);
        String str = domain.sayHello("jack");
        ctx.setData("hi", str);

        List<Person> personList = ListUtil.toList(
                new Person("jack", 15000),
                new Person("tom", 13500),
                new Person("peter", 18600)
        );

        int totalSalary = personList.stream().mapToInt(Person::getSalary).sum();

        System.out.println(totalSalary);
        ctx.setData("salary", 47100);
    }
}