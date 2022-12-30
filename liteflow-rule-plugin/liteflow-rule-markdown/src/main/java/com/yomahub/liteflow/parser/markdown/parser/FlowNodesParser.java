package com.yomahub.liteflow.parser.markdown.parser;

import com.yomahub.liteflow.enums.NodeTypeEnum;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FlowNodesParser implements Parser{


    @Override
    public void parse(ParseContext context) {
        final Map<String, FlowChartNode> nodeCache = new HashMap<>();

        FlowChartNode pre = null;
        boolean pointing = false;
        for (int i = 4; i < context.lines.length ; i++) {
            String current = context.lines[i];

            int length = current.length();
            int blockEnd = 0, blockStart = 0;
            String tag = "";
            boolean linkStarted = false;
            for (int j = 0; j < length; j++) {

                if (j <= length - 2) {
                    String tmpStr = current.substring(j, j + 2);
                    if (tmpStr.equals("--") && !linkStarted) {
                        // arrow link start
                        linkStarted = true;

                        blockEnd = j ;
                        if (blockEnd <= blockStart) {
                            continue;
                        }
                        final FlowChartNode flowChartNodeTmp = cutBlock(current.substring(blockStart, blockEnd), tag);
                        FlowChartNode flowChartNode = nodeCache.computeIfAbsent(flowChartNodeTmp.nid, (nid) -> flowChartNodeTmp);

                        if (pre == null){
                            context.head = flowChartNode;
                        } else if (pre != null && pointing) {
                            pre.next.add(flowChartNode);
                            flowChartNode.pre = pre;
                            pointing = false;
                        }
                        pre = flowChartNode;

                        continue;
                    } else if (linkStarted && tmpStr.equals("->")) {
                        // arrow link end
                        linkStarted = false;
                        pointing = true;
                        blockStart = j + 2;

                        int channelStart = -1;
                        for (int k = j + 2 ; k < length; k++) {
                            char charForFindingChannelMark = current.charAt(k);
                            if (charForFindingChannelMark == '|' && channelStart < 0) {
                                channelStart = k;
                                continue;
                            }
                            if (charForFindingChannelMark == '|' && channelStart > 0) {
                                tag = current.substring(channelStart + 1, k);
                                j = k + 1;
                                blockStart = k + 1;
                                break;
                            }
                            if (charForFindingChannelMark != ' ' && charForFindingChannelMark != '|' && channelStart < 0) {
                                j = k;
                                blockStart = k;
                                break;
                            }
                        }


                    } else if (linkStarted) {
                        continue;
                    }
                }
            }

            final FlowChartNode flowChartNodeTmp = cutBlock(current.substring(blockStart), tag);
            if (flowChartNodeTmp.nid.isEmpty()) {
                continue;
            }
            FlowChartNode flowChartNode = nodeCache.computeIfAbsent(flowChartNodeTmp.nid, (nid) -> flowChartNodeTmp);

            if (pre == null){
                context.head = flowChartNode;
            } else if (pre != null && pointing) {
                pre.next.add(flowChartNode);
                flowChartNode.pre = pre;
                pointing = false;
            }
            pre = flowChartNode;
        }


    }

    private static FlowChartNode cutBlock(String rawBlock, String tag) {
        rawBlock = rawBlock.trim();

        if (rawBlock.length() <= 3) {
            return new FlowChartNode(rawBlock, "", NodeTypeEnum.COMMON, tag);
        }

        int i = 0;
        while (i < rawBlock.length()) {
            String tmpBracketL1 = rawBlock.substring(i, i + 1);
            if (!bracketHead.contains(tmpBracketL1)) {
                i ++;
                continue;
            }
            for (int j = bracketL.length - 1; j >= 0 ; j--) {
                String matchTarget;
                if (j <= 3) {
                    matchTarget = tmpBracketL1;
                } else if (j <= 12) {
                    matchTarget = rawBlock.substring(i, i + 2);
                } else {
                    matchTarget = rawBlock.substring(i, i + 3);
                }
                if (matchTarget.equals(bracketL[j]) && bracketR[j].equals(rawBlock.substring(rawBlock.length() - matchTarget.length()))) {
                    // found match
                    int nodeIdEnd = i ;
                    int descStart = i + matchTarget.length() ;
                    int descEnd = rawBlock.length() - matchTarget.length();

                    switch (matchTarget) {
                        case "{":
                            return new FlowChartNode(
                                    rawBlock.substring(0, nodeIdEnd),
                                    descStart > 0? rawBlock.substring(descStart, descEnd): "",
                                    NodeTypeEnum.SWITCH,
                                    tag);
                        case "((":
                            // ignore abend node description
                            return new FlowChartNode("", "", NodeTypeEnum.COMMON, tag);
                        default:
                            return new FlowChartNode(
                                    rawBlock.substring(0, nodeIdEnd),
                                    descStart > 0? rawBlock.substring(descStart, descEnd): "",
                                    NodeTypeEnum.COMMON,
                                    tag);
                    }


                }
            }
            i ++;
        }

        return new FlowChartNode(rawBlock, "", NodeTypeEnum.COMMON, tag);
    }

    private static final String[] bracketL = {"[", "(", ">", "{", "([", "[[", "[(", "((", "{{", "[/", "[\\", "[/", "[\\", "((("};
    private static final String[] bracketR = {"]", ")", "]", "}", "])", "]]", ")]", "))", "}}", "/]", "\\]", "\\]", "/]", ")))"};

    private static final Set<String> bracketHead = Arrays.stream(bracketL).map(c -> c.substring(0, 1)).collect(Collectors.toSet());
}
