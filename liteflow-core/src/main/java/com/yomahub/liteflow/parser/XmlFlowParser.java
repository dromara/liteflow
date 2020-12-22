package com.yomahub.liteflow.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.yomahub.liteflow.entity.flow.*;
import com.yomahub.liteflow.exception.ExecutableItemNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.spring.ComponentScaner;
import com.yomahub.liteflow.util.Dom4JReader;

public abstract class XmlFlowParser {

	private final Logger LOG = LoggerFactory.getLogger(XmlFlowParser.class);

	public abstract void parseMain(String path) throws Exception;

	public void parse(String content) throws Exception {
		Document document = Dom4JReader.getFormatDocument(content);
		parse(document);
	}

	@SuppressWarnings("unchecked")
	public void parse(Document document) throws Exception {
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
					FlowBus.addNode(id, node);
				}
			}else{
				for(Entry<String, NodeComponent> componentEntry : ComponentScaner.nodeComponentMap.entrySet()){
					if(!FlowBus.containNode(componentEntry.getKey())){
						FlowBus.addNode(componentEntry.getKey(), new Node(componentEntry.getKey(), componentEntry.getValue().getClass().getName(), componentEntry.getValue()));
					}
				}
			}

			// 解析chain节点
			List<Element> chainList = rootElement.elements("chain");
			for (Element e : chainList) {
				parseOneChain(e);
			}
		} catch (Exception e) {
			LOG.error("FlowParser parser exception: {}", e);
			throw e;
		}
	}

	private void parseOneChain(Element e) throws Exception{
		String condArrayStr;
		String[] condArray;
		List<Executable> chainNodeList;
		List<Condition> conditionList;

		String chainName = e.attributeValue("name");
		// 增加循环加载组件时的加载过滤
		if (FlowBus.containChain(chainName)){
			return;
		}

		conditionList = new ArrayList<>();
		for (Iterator<Element> it = e.elementIterator(); it.hasNext();) {
			Element condE = it.next();
			condArrayStr = condE.attributeValue("value");
			if (StringUtils.isBlank(condArrayStr)) {
				continue;
			}
			chainNodeList = new ArrayList<>();
			condArray = condArrayStr.split(",");
			RegexEntity regexEntity;
			String itemExpression;
			String item;
			for (int i = 0; i < condArray.length; i++) {
				itemExpression = condArray[i].trim();
				regexEntity = parseNodeStr(itemExpression);
				item = regexEntity.getItem();
				if(FlowBus.containNode(item)){
					Node node = FlowBus.getNode(item);
					chainNodeList.add(node);
					if(regexEntity.getRealItemArray() != null){
						for(String key : regexEntity.getRealItemArray()){
							if(FlowBus.containNode(key)){
								Node condNode = FlowBus.getNode(key);
								node.setCondNode(condNode.getId(), condNode);
							}else if(hasChain(e,key)){
								Chain chain = FlowBus.getChain(key);
								node.setCondNode(chain.getChainName(), chain);
							}
						}
					}
				}else if(hasChain(e,item)){
					Chain chain = FlowBus.getChain(item);
					chainNodeList.add(chain);
				}else{
					throw new ExecutableItemNotFoundException();
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

	private boolean hasChain(Element e,String chainName) throws Exception{
		Element rootElement = e.getParent();
		List<Element> chainList = rootElement.elements("chain");
		for(Element ce : chainList){
			String ceName = ce.attributeValue("name");
			if(ceName.equals(chainName)){
				if(!FlowBus.containChain(chainName)){
					parseOneChain(ce);
				}
				return true;
			}
		}
		return false;
	}

	public static RegexEntity parseNodeStr(String str) {
	    List<String> list = new ArrayList<String>();
	    Pattern p = Pattern.compile("[^\\)\\(]+");
	    Matcher m = p.matcher(str);
	    while(m.find()){
	        list.add(m.group());
	    }
	    RegexEntity regexEntity = new RegexEntity();
	    regexEntity.setItem(list.get(0).trim());
	    if(list.size() > 1){
	    	String[] realNodeArray = list.get(1).split("\\|");
	    	for (int i = 0; i < realNodeArray.length; i++) {
	    		realNodeArray[i] = realNodeArray[i].trim();
			}
	    	regexEntity.setRealItemArray(realNodeArray);
	    }
	    return regexEntity;
	}
}
