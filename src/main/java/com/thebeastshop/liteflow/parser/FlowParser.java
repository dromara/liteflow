/**
 * <p>Title: liteFlow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2017-7-28
 * @version 1.0
 */
package com.thebeastshop.liteflow.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thebeastshop.liteflow.core.Component;
import com.thebeastshop.liteflow.entity.config.Chain;
import com.thebeastshop.liteflow.entity.config.Condition;
import com.thebeastshop.liteflow.entity.config.Node;
import com.thebeastshop.liteflow.entity.config.ThenCondition;
import com.thebeastshop.liteflow.entity.config.WhenCondition;
import com.thebeastshop.liteflow.flow.FlowBus;
import com.thebeastshop.liteflow.util.Dom4JReader;
import com.thebeastshop.liteflow.util.IOUtil;

@SuppressWarnings("unchecked")
public class FlowParser {

	private static final Logger LOG = LoggerFactory.getLogger(FlowParser.class);

	private static final String ENCODING_FORMAT = "UTF-8";

	public static void parseLocal(String rulePath) throws Exception {
		String ruleContent = IOUtil.read(rulePath, ENCODING_FORMAT);
		parse(ruleContent);
	}

	public static void parse(String content) throws Exception {
		Document document = Dom4JReader.getFormatDocument(content);
		parse(document);
	}

	public static void parse(Document document) throws Exception {
		try {
			Element rootElement = document.getRootElement();

			// 解析node节点
			List<Element> nodeList = rootElement.element("nodes").elements("node");
			String id = null;
			String clazz = null;
			Node node = null;
			Component component = null;
			Map<String, Node> nodeMap = new HashMap<String, Node>();
			for (Element e : nodeList) {
				node = new Node();
				id = e.attributeValue("id");
				clazz = e.attributeValue("class");
				node.setId(id);
				node.setClazz(clazz);
				component.setNodeId(id);
				component = (Component) Class.forName(clazz).newInstance();
				if (component == null) {
					LOG.error("couldn't find component class [{}] ", clazz);
				}
				node.setInstance(component);
				nodeMap.put(id, node);
			}

			// 解析chain节点
			String chainName = null;
			String condArrayStr = null;
			String[] condArray = null;
			List<Node> chainNodeList = null;
			List<Condition> conditionList = null;

			List<Element> chainList = rootElement.elements("chain");
			for (Element e : chainList) {
				chainName = e.attributeValue("name");
				conditionList = new ArrayList<Condition>();
				for (Iterator<Element> it = e.elementIterator(); it.hasNext();) {
					Element condE = it.next();
					condArrayStr = condE.attributeValue("value");
					if (StringUtils.isBlank(condArrayStr)) {
						continue;
					}
					chainNodeList = new ArrayList<Node>();
					condArray = condArrayStr.split(",");
					for (int i = 0; i < condArray.length; i++) {
						chainNodeList.add(nodeMap.get(condArray[i]));
					}
					if (condE.getName().equals("then")) {
						conditionList.add(new ThenCondition(chainNodeList));
					} else if (condE.getName().equals("when")) {
						conditionList.add(new WhenCondition(chainNodeList));
					}
				}
				FlowBus.addChain(chainName, new Chain(conditionList));
			}
		} catch (Exception e) {
			LOG.error("FlowParser parser exception: {}", e);
		}

	}
}
