package com.yomahub.liteflow.test.iterator.cmp;

import com.yomahub.liteflow.core.NodeIteratorComponent;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Component("it")
public class ITCmp extends NodeIteratorComponent {

	@Override
	public Iterator<?> processIterator() throws Exception {
		List<String> list = this.getRequestData();
		return list.iterator();
	}

}
