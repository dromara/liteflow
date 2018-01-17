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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thebeastshop.liteflow.core.NodeComponent;
import com.thebeastshop.liteflow.entity.config.Chain;
import com.thebeastshop.liteflow.entity.config.Condition;
import com.thebeastshop.liteflow.entity.config.Node;
import com.thebeastshop.liteflow.entity.config.ThenCondition;
import com.thebeastshop.liteflow.entity.config.WhenCondition;
import com.thebeastshop.liteflow.flow.FlowBus;
import com.thebeastshop.liteflow.spring.ComponentScaner;
import com.thebeastshop.liteflow.util.Dom4JReader;
import com.thebeastshop.liteflow.util.IOUtil;

@SuppressWarnings("unchecked")
public class FlowParser {

	private static final Logger LOG = LoggerFactory.getLogger(FlowParser.class);

	private static final String ENCODING_FORMAT = "UTF-8";
	
	private static Map<String, Node> nodeMap = new HashMap<String, Node>();

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

			//判断是以spring方式注册节点，还是以xml方式注册
			if(ComponentScaner.nodeComponentMap.isEmpty()){
				// 解析node节点
				List<Element> nodeList = rootElement.element("nodes").elements("node");
				String id = null;
				String clazz = null;
				Node node = null;
				NodeComponent component = null;
				for (Element e : nodeList) {
					node = new Node();
					id = e.attributeValue("id");
					clazz = e.attributeValue("class");
					node.setId(id);
					node.setClazz(clazz);
					component = (NodeComponent) Class.forName(clazz).newInstance();
					if (component == null) {
						LOG.error("couldn't find component class [{}] ", clazz);
					}
					component.setNodeId(id);
					node.setInstance(component);
					nodeMap.put(id, node);
				}
			}else{
				for(Entry<String, NodeComponent> componentEntry : ComponentScaner.nodeComponentMap.entrySet()){
					nodeMap.put(componentEntry.getKey(), new Node(componentEntry.getKey(), componentEntry.getValue().getClass().getName(), componentEntry.getValue()));
				}
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
					RegexEntity regexEntity = null;
					Node node = null;
					for (int i = 0; i < condArray.length; i++) {
						regexEntity = parseNodeStr(condArray[i].trim());
						node = nodeMap.get(regexEntity.getCondNode());
						chainNodeList.add(node);
						if(regexEntity.getRealNodeArray() != null){
							for(String key : regexEntity.getRealNodeArray()){
								Node condNode = nodeMap.get(key);
								if(condNode != null){
									node.setCondNode(condNode.getId(), condNode);
								}
							}
						}
					}
					if (condE.getName().equals("then")) {
						conditionList.add(new ThenCondition(chainNodeList));
					} else if (condE.getName().equals("when")) {
						conditionList.add(new WhenCondition(chainNodeList));
					}
				}
				FlowBus.addChain(chainName, new Chain(chainName,conditionList));
			}
		} catch (Exception e) {
			LOG.error("FlowParser parser exception: {}", e);
		}

	}
	
	public static Node getNode(String nodeId){
		return nodeMap.get(nodeId);
	}
	
	private static class RegexEntity{
		
		private String condNode;
		
		private String[] realNodeArray;

		public String getCondNode() {
			return condNode;
		}

		public void setCondNode(String condNode) {
			this.condNode = condNode;
		}

		public String[] getRealNodeArray() {
			return realNodeArray;
		}

		public void setRealNodeArray(String[] realNodeArray) {
			this.realNodeArray = realNodeArray;
		}

		@Override
		public String toString() {
			return "RegexEntity [condNode=" + condNode + ", realNodeArray="
					+ Arrays.toString(realNodeArray) + "]";
		}
	}
	
	public static RegexEntity parseNodeStr(String str) {
	    List<String> list = new ArrayList<String>();
	    Pattern p = Pattern.compile("[^\\)\\(]+");
	    Matcher m = p.matcher(str);
	    while(m.find()){
	        list.add(m.group());
	    }
	    RegexEntity regexEntity = new RegexEntity();
	    regexEntity.setCondNode(list.get(0).trim());
	    if(list.size() > 1){
	    	String[] realNodeArray = list.get(1).split("\\|");
	    	for (int i = 0; i < realNodeArray.length; i++) {
	    		realNodeArray[i] = realNodeArray[i].trim();
			}
	    	regexEntity.setRealNodeArray(realNodeArray);
	    }
	    return regexEntity;
	}
	
	public static void main(String[] args) {
		System.out.println(parseNodeStr("aaaa ( xxxx | yyyy | vvvv )"));
	}
}
