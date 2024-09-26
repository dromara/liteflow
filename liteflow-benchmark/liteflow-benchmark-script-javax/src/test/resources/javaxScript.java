import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.benchmark.cmp.Person;
import com.yomahub.liteflow.benchmark.cmp.TestDomain;
import com.yomahub.liteflow.script.body.CommonScriptBody;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.script.ScriptExecuteWrap;

import java.util.List;
import java.util.function.ToIntFunction;

public class Demo implements CommonScriptBody {
    public Void body(ScriptExecuteWrap wrap) {
        int v1 = 2;
        int v2 = 3;
        DefaultContext ctx = wrap.getCmp().getFirstContextBean();
        ctx.setData("s1", v1 * v2);

        TestDomain domain = ContextAwareHolder.loadContextAware().getBean(TestDomain.class);
        String str = domain.sayHello("jack");
        ctx.setData("hi", str);

        List<Person> personList = ListUtil.toList(
                new Person("jack", 15000),
                new Person("tom", 13500),
                new Person("peter", 18600)
        );

        int totalSalary = personList.stream().mapToInt(Person::getSalary).sum();

        ctx.setData("salary", 47100);

        return null;
    }
}