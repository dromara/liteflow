import com.yomahub.liteflow.benchmark.cmp.TestDomain;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.script.body.JaninoCommonScriptBody;
import com.yomahub.liteflow.script.ScriptExecuteWrap;

public class Demo implements JaninoCommonScriptBody {
    public Void body(ScriptExecuteWrap wrap) {
        int v1 = 2;
        int v2 = 3;
        DefaultContext ctx = (DefaultContext) wrap.getCmp().getFirstContextBean();
        ctx.setData("s1", v1 * v2);

        TestDomain domain = (TestDomain) ContextAwareHolder.loadContextAware().getBean(TestDomain.class);
        String str = domain.sayHello("jack");
        ctx.setData("hi", str);

        return null;
    }
}