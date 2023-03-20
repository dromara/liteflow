package com.yomahub.liteflow.test.iterator.cmp;

import com.yomahub.liteflow.core.NodeIteratorComponent;

import java.util.Iterator;
import java.util.List;

public class ITCmp extends NodeIteratorComponent {

	@Override
	public Iterator<?> processIterator() throws Exception {
		List<String> list = this.getRequestData();
		return list.iterator();
	}

}
