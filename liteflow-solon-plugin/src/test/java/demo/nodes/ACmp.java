package demo.nodes;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import org.noear.solon.annotation.Component;

/**
 * @author noear 2022/9/21 created
 */
@Component("a")
public class ACmp extends NodeComponent {

    @Override
    public void process() {
        //do your business
        System.out.println(this.getClass().getName());
    }
}
