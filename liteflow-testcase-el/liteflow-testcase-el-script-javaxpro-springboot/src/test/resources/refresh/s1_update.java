import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;

public class Demo extends NodeComponent {

    @Override
    public void process() {
        DefaultContext context = this.getFirstContextBean();
        context.setData("testFlag","2");
    }

}