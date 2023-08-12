package com.dingding.mid.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dingding.mid.dto.json.*;
import com.dingding.mid.dto.json.Properties;
import com.dingding.mid.entity.Users;
import com.dingding.mid.enums.ModeEnums;
import com.dingding.mid.exception.WorkFlowException;
import com.dingding.mid.service.UserService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.*;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.springframework.util.CollectionUtils;

import java.io.UnsupportedEncodingException;
import java.lang.Process;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static com.dingding.mid.common.WorkFlowConstants.*;

/**
 * @author LoveMyOrange
 * @create 2022-10-10 17:47
 */
@SuppressWarnings("all")
public class BpmnModelUtils {

    private static String id(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }


    public static SequenceFlow connect(AbstractFlowNodeBuilder fromNode,AbstractFlowNodeBuilder toNode,String from, String to, List<SequenceFlow> sequenceFlows, Map<String,ChildNode> childNodeMap) {
        String  sequenceFlowId = id("sequenceFlow");
        AbstractFlowNodeBuilder paramNode = moveToNode(fromNode, from);
        BpmnModelInstance bpmnModelInstance = paramNode.done();
        FlowNode element = (FlowNode) paramNode.getElement();
        SequenceFlow sequenceFlow = iteratorSequenceFlow(element, from, to);
        if(sequenceFlow==null){
                    paramNode.connectTo(to);
                     sequenceFlow= iteratorSequenceFlow(element, from, to);
        }
        if(bpmnModelInstance.getModelElementById(from) !=null && bpmnModelInstance.getModelElementById(from) instanceof ExclusiveGateway){
            ChildNode childNode = childNodeMap.get(to);
            if(childNode!=null){
                String parentId = childNode.getParentId();
                if(StringUtils.isNotBlank(parentId)){
                    ChildNode parentNode = childNodeMap.get(parentId);
                    if(parentNode!=null){
                        if(Type.CONDITION.type.equals(parentNode.getType()) ){
                            sequenceFlowId=parentNode.getId();
                            sequenceFlow.setName(parentNode.getName());

                            if(Boolean.FALSE.equals(parentNode.getTypeElse())){
                                //解析条件表达式
                                Properties props = parentNode.getProps();
                                String expression = props.getExpression();
                                List<GroupsInfo> groups = props.getGroups();
                                String groupsType = props.getGroupsType();
                                if(StringUtils.isNotBlank(expression)){
                                    paramNode.done().newInstance(ConditionExpression.class).setTextContent("${"+expression+"}");
                                }
                                else {

                                    StringBuffer conditionExpression=new StringBuffer();
                                    conditionExpression.append("${ ");

                                    for (int i = 0; i < groups.size(); i++) {
                                        conditionExpression.append(" ( ");
                                        GroupsInfo group = groups.get(i);
                                        List<String> cids = group.getCids();
                                        String groupType = group.getGroupType();
                                        List<ConditionInfo> conditions = group.getConditions();
                                        for (int j = 0; j < conditions.size(); j++) {
                                            conditionExpression.append(" ");
                                            ConditionInfo condition = conditions.get(j);
                                            String compare = condition.getCompare();
                                            String id = condition.getId();
                                            String title = condition.getTitle();
                                            List<Object> value = condition.getValue();
                                            String valueType = condition.getValueType();
                                            if("String".equals(valueType)){
                                                if("=".equals(compare)){
                                                    String str = StringUtils.join(value, ",");
                                                    str="'"+str+"'";
                                                    conditionExpression.append(" "+ EXPRESSION_CLASS+"strEqualsMethod("+id+","+str+") " );
                                                }
                                                else{
                                                    List<String> tempList=new ArrayList<>();
                                                    for (Object o : value) {
                                                        String s = o.toString();
                                                        s="'"+s+"'";
                                                        tempList.add(s);
                                                    }
                                                    String str = StringUtils.join(tempList, ",");
//                                                String str = StringUtils.join(value, ",");
                                                    conditionExpression.append(" "+ EXPRESSION_CLASS+"strContainsMethod("+id+","+str+") " );
                                                }
                                            }
                                            else if("Number".equals(valueType)){
                                                String str = StringUtils.join(value, ",");
                                                if("=".equals(compare)){
                                                    conditionExpression.append(" "+ EXPRESSION_CLASS+"numberEquals("+id+","+str+") " );
                                                }
                                                else if(">".equals(compare)){
                                                    conditionExpression.append(" "+ EXPRESSION_CLASS+"numberGt("+id+","+str+") " );
                                                }
                                                else if(">=".equals(compare)){
                                                    conditionExpression.append(" "+ EXPRESSION_CLASS+"numberGtEquals("+id+","+str+") " );
                                                }
                                                else if("<".equals(compare)){
                                                    conditionExpression.append(" "+ EXPRESSION_CLASS+"numberLt("+id+","+str+") " );
                                                }
                                                else if("<=".equals(compare)){
                                                    conditionExpression.append(" "+ EXPRESSION_CLASS+"numberLtEquals("+id+","+str+") " );
                                                }
                                                else if("IN".equals(compare)){
                                                    conditionExpression.append(" "+ EXPRESSION_CLASS+"numberContains("+id+","+str+") " );
                                                }
                                                else if("B".equals(compare)){
                                                    conditionExpression.append("  "+ EXPRESSION_CLASS+"b("+id+","+str+") " );
                                                }
                                                else if("AB".equals(compare)){
                                                    conditionExpression.append("  "+ EXPRESSION_CLASS+"ab("+id+","+str+") " );
                                                }
                                                else if("BA".equals(compare)){
                                                    conditionExpression.append("  "+ EXPRESSION_CLASS+"ba("+id+","+str+") " );
                                                }
                                                else if("ABA".equals(compare)){
                                                    conditionExpression.append("  "+ EXPRESSION_CLASS+"aba("+id+","+str+") " );
                                                }
                                            }
                                            else if("User".equals(valueType)){
                                                List<String> userIds=new ArrayList<>();
                                                for (Object o : value) {
                                                    JSONObject obj=(JSONObject)o;
                                                    userIds.add(obj.getString("id"));
                                                }
                                                String str = StringUtils.join(userIds, ",");
                                                conditionExpression.append(" "+ EXPRESSION_CLASS+"userStrContainsMethod("+id+","+str+") " );
                                            }
                                            else if("Dept".equals(valueType)){
                                                List<String> userIds=new ArrayList<>();
                                                for (Object o : value) {
                                                    JSONObject obj=(JSONObject)o;
                                                    userIds.add(obj.getString("id"));
                                                }
                                                String str = StringUtils.join(userIds, ",");
                                                conditionExpression.append(" "+ EXPRESSION_CLASS+"deptStrContainsMethod("+id+","+str+") " );
                                            }
                                            else{
                                                continue;
                                            }

                                            if(conditions.size()>1 && j!=(conditions.size()-1)){
                                                if("OR".equals(groupType)){
                                                    conditionExpression.append(" || ");
                                                }
                                                else {
                                                    conditionExpression.append(" && ");
                                                }
                                            }

                                            if(i==(conditions.size()-1)){
                                                conditionExpression.append(" ");
                                            }
                                        }


                                        conditionExpression.append(" ) ");

                                        if(groups.size()>1 && i!=(groups.size()-1) ){
                                            if("OR".equals(groupsType)){
                                                conditionExpression.append(" || ");
                                            }
                                            else {
                                                conditionExpression.append(" && ");
                                            }
                                        }


                                    }
                                    conditionExpression.append("} ");
                                    ConditionExpression exp = sequenceFlow.builder().done().newInstance(ConditionExpression.class);
                                    exp.setTextContent(conditionExpression.toString());
                                    sequenceFlow.setConditionExpression(exp);
//                                    paramNode.done().newInstance(ConditionExpression.class).setTextContent(conditionExpression.toString());
                                }
                            }
                        }
                    }
                }
            }
        }
        sequenceFlow.setId(sequenceFlowId);
        sequenceFlows.add(sequenceFlow);
        return null;
    }

    private static SequenceFlow iteratorSequenceFlow(FlowNode element, String from, String to) {
        Collection<SequenceFlow> outgoing = element.getOutgoing();
        SequenceFlow resultSequenceFlow=null;
        for (SequenceFlow sequenceFlow : outgoing) {
            if((ObjectUtil.isNotEmpty(sequenceFlow.getSource()) && sequenceFlow.getSource().getId().equals(from))
            && (ObjectUtil.isNotEmpty(sequenceFlow.getTarget()) && sequenceFlow.getTarget().getId().equals(to))
            ){
                resultSequenceFlow=sequenceFlow;
                break;
            }
        }
        return resultSequenceFlow;
    }

    private static String stringEquals(ConditionInfo condition) {
        return null;
    }


    public static StartEventBuilder createStartEvent(ProcessBuilder executableProcess) {
        StartEventBuilder startEventBuilder = executableProcess.startEvent();
        startEventBuilder.id(START_EVENT_ID);
        startEventBuilder.camundaInitiator("applyUserId");
        return startEventBuilder;
    }



    public static String create(AbstractFlowNodeBuilder abstractFlowNodeBuilder, String fromId, ChildNode flowNode, /*Process process,*/ List<SequenceFlow> sequenceFlows, Map<String,ChildNode> childNodeMap) throws InvocationTargetException, IllegalAccessException {
        String nodeType = flowNode.getType();
        if (Type.CONCURRENTS.isEqual(nodeType)) {
            return createParallelGatewayBuilder(abstractFlowNodeBuilder,fromId,/*process,*/flowNode,sequenceFlows,childNodeMap);
        } else if (Type.CONDITIONS.isEqual(nodeType)) {
            return createExclusiveGatewayBuilder(abstractFlowNodeBuilder,fromId,/*process,*/flowNode,sequenceFlows,childNodeMap);
        } else if (Type.USER_TASK.isEqual(nodeType)) {
            childNodeMap.put(flowNode.getId(),flowNode);
            JSONObject incoming = flowNode.getIncoming();
            incoming.put("incoming", Collections.singletonList(fromId));
            String id = createTask(abstractFlowNodeBuilder/*,process*/,flowNode,sequenceFlows,childNodeMap);
            // 如果当前任务还有后续任务，则遍历创建后续任务
            ChildNode children = flowNode.getChildren();
            if (Objects.nonNull(children) &&StringUtils.isNotBlank(children.getId())) {
                AbstractFlowNodeBuilder<?, ?> currentTaskBuilder = moveToNode(abstractFlowNodeBuilder, id);
                return create(currentTaskBuilder,id, children/*,process*/,sequenceFlows,childNodeMap);
            } else {
                return id;
            }
        }
        else if(Type.ROOT.isEqual(nodeType)){
            childNodeMap.put(flowNode.getId(),flowNode);
            JSONObject incoming = flowNode.getIncoming();
            incoming.put("incoming", Collections.singletonList(fromId));
            String id = createTask(abstractFlowNodeBuilder/*,process*/,flowNode,sequenceFlows,childNodeMap);
            // 如果当前任务还有后续任务，则遍历创建后续任务
            ChildNode children = flowNode.getChildren();
            if (Objects.nonNull(children) &&StringUtils.isNotBlank(children.getId())) {
                AbstractFlowNodeBuilder<?, ?> currentTaskBuilder = moveToNode(abstractFlowNodeBuilder, id);
                return create(currentTaskBuilder,id, children/*,process*/,sequenceFlows,childNodeMap);
            } else {
                return id;
            }
        }
        else if(Type.DELAY.isEqual(nodeType)){
            throw new WorkFlowException("由于代码被其他开源项目抄袭,在github版本提供了延时节点的实现!(免费),请联系V:ProcessEngine 提供公司名字以及GitHub 用户名后 拉你进仓库! 实际上吃透这个项目代码之后,也能自己写出来");
        }
        else if(Type.TRIGGER.isEqual(nodeType)){
            throw new WorkFlowException("由于代码被其他开源项目抄袭,在github版本提供了触发器节点的实现!(免费),请联系V:ProcessEngine 提供公司名字以及GitHub 用户名后 拉你进仓库! 实际上吃透这个项目代码之后,也能自己写出来");
        }
        else if(Type.CC.isEqual(nodeType)){
            throw new WorkFlowException("由于代码被其他开源项目抄袭,在github版本提供了触发器节点的实现!(免费),请联系V:ProcessEngine 提供公司名字以及GitHub 用户名后 拉你进仓库! 实际上吃透这个项目代码之后,也能自己写出来");
        }
        else {
            throw new RuntimeException("未知节点类型: nodeType=" + nodeType);
        }
    }

    private static String createExclusiveGatewayBuilder(AbstractFlowNodeBuilder startFlowNodeBuilder,String formId,ChildNode flowNode,List<SequenceFlow> sequenceFlows,Map<String,ChildNode> childNodeMap) throws InvocationTargetException, IllegalAccessException {
        childNodeMap.put(flowNode.getId(),flowNode);
        String name =flowNode.getName();
        String exclusiveGatewayId = flowNode.getId();
        ExclusiveGatewayBuilder exclusiveGatewayBuilder = startFlowNodeBuilder.exclusiveGateway(exclusiveGatewayId).name(name);
        AbstractFlowNodeBuilder<?, ?> fromBuilder = moveToNode(startFlowNodeBuilder, formId);
        AbstractFlowNodeBuilder<?, ?> toBuilder = moveToNode(startFlowNodeBuilder, exclusiveGatewayId);
        connect(fromBuilder,toBuilder,formId, exclusiveGatewayId,sequenceFlows,childNodeMap);
        if (Objects.isNull(flowNode.getBranchs()) && Objects.isNull(flowNode.getChildren())) {
            return exclusiveGatewayId;
        }
        List<ChildNode> flowNodes = flowNode.getBranchs();
        List<String> incoming = Lists.newArrayListWithCapacity(flowNodes.size());
        List<JSONObject> conditions = Lists.newCopyOnWriteArrayList();
        for (ChildNode element : flowNodes) {
            Boolean typeElse = element.getTypeElse();
            childNodeMap.put(element.getId(),element);
            ChildNode childNode = element.getChildren();

            String nodeName = element.getName();
            Properties props = element.getProps();
            String expression = props.getExpression();


            if (Objects.isNull(childNode) ||  StringUtils.isBlank(childNode.getId())) {

                incoming.add(exclusiveGatewayId);
                JSONObject condition = new JSONObject();
                condition.fluentPut("nodeName", nodeName)
                        .fluentPut("expression", expression)
                        .fluentPut("groups",props.getGroups())
                        .fluentPut("groupsType",props.getGroupsType()
                                )
                        .fluentPut("elseSequenceFlowId",element.getId());
                conditions.add(condition);
                continue;
            }
            // 只生成一个任务，同时设置当前任务的条件
            JSONObject incomingObj = childNode.getIncoming();
            incomingObj.put("incoming", Collections.singletonList(exclusiveGatewayId));

            String identifier = create(exclusiveGatewayBuilder,exclusiveGatewayId, childNode,sequenceFlows,childNodeMap);
            List<SequenceFlow> flows = sequenceFlows.stream().filter(flow -> StringUtils.equals(exclusiveGatewayId, flow.getSource().getId()))
                    .collect(Collectors.toList());
            flows.stream().forEach(
                    e -> {
                        if (StringUtils.isBlank(e.getName()) && StringUtils.isNotBlank(nodeName)) {
                            e.setName(nodeName);
                        }
                        // 设置条件表达式
                        if (Objects.isNull(e.getConditionExpression()) && StringUtils.isNotBlank(expression)) {
                            Method createInstance = getDeclaredMethod(exclusiveGatewayBuilder, "createInstance", Class.class);
                            createInstance.setAccessible(true);
                            ConditionExpression conditionExpression = null;
                            try {
                                conditionExpression = (ConditionExpression) createInstance.invoke(exclusiveGatewayBuilder, ConditionExpression.class);
                            } catch (IllegalAccessException ex) {
                                throw new RuntimeException(ex);
                            } catch (InvocationTargetException ex) {
                                throw new RuntimeException(ex);
                            }
                            conditionExpression.setTextContent(expression);


                            e.setConditionExpression(conditionExpression);
                        }
                    }
            );
            if (Objects.nonNull(identifier)) {
                incoming.add(identifier);
            }
            if(Boolean.TRUE.equals(typeElse)){
                exclusiveGatewayBuilder.defaultFlow(exclusiveGatewayBuilder.done().getModelElementById(element.getId()));
            }
        }


        ChildNode childNode = flowNode.getChildren();

        if (Objects.nonNull(childNode) &&StringUtils.isNotBlank(childNode.getId()) ) {
            String parentId = childNode.getParentId();
            ChildNode parentChildNode = childNodeMap.get(parentId);
            boolean conFlag = Type.CONCURRENTS.type
                .equals(parentChildNode.getType());
            if(!conFlag) {
                String type = childNode.getType();
                if(!Type.EMPTY.type.equals(type)){
                }
                else{
                    if(Type.CONDITIONS.type.equals(parentChildNode.getType())){
                      String endExId=  parentChildNode.getId()+"ex";
                        if (incoming == null || incoming.isEmpty()) {
                            AbstractFlowNodeBuilder<?, ?> abstractFlowNodeBuilder = moveToNode(startFlowNodeBuilder, exclusiveGatewayId);
                            return create(abstractFlowNodeBuilder,exclusiveGatewayId, childNode, sequenceFlows,
                                childNodeMap);
                        }
                        else {
                            JSONObject incomingObj = childNode.getIncoming();
                            // 所有 service task 连接 end exclusive gateway
                            incomingObj.put("incoming", incoming);
                            // 1.0 先进行边连接, 暂存 nextNode
                            ChildNode nextNode = childNode.getChildren();
                            childNode.setChildren(null);
                            String identifier = endExId;
                            for (int i = 0; i < incoming.size(); i++) {
                                AbstractFlowNodeBuilder<?, ?> abstractFlowNodeBuilder = moveToNode(startFlowNodeBuilder, incoming.get(0));
                                ModelElementInstance endExNode = startFlowNodeBuilder.done().getModelElementById(identifier);
                                if(endExNode==null){
                                    abstractFlowNodeBuilder.exclusiveGateway(identifier);
                                }
                                AbstractFlowNodeBuilder<?, ?> toNode = moveToNode(startFlowNodeBuilder, identifier);
                                connect(abstractFlowNodeBuilder,toNode, incoming.get(i),identifier, sequenceFlows,childNodeMap);
                            }

                            //  针对 gateway 空任务分支 添加条件表达式
                            if (!conditions.isEmpty()) {
                                FlowElement flowElement1 = startFlowNodeBuilder.done().getModelElementById(identifier);
                                // 获取从 gateway 到目标节点 未设置条件表达式的节点
                                List<SequenceFlow> flows = sequenceFlows.stream().filter(
                                    flow -> StringUtils.equals(flowElement1.getId(), flow.getTarget().getId()))
                                    .filter(
                                        flow -> StringUtils.equals(flow.getSource().getId(), exclusiveGatewayId))
                                    .collect(Collectors.toList());
                                flows.stream().forEach(sequenceFlow -> {
                                    if (!conditions.isEmpty()) {
                                        JSONObject condition = conditions.get(0);
                                        String nodeName = condition.getString("nodeName");
                                        String expression = condition.getString("expression");

                                        if (StringUtils.isBlank(sequenceFlow.getName()) && StringUtils
                                            .isNotBlank(nodeName)) {
                                            sequenceFlow.setName(nodeName);
                                        }
                                        // 设置条件表达式
                                        if (Objects.isNull(sequenceFlow.getConditionExpression())
                                            && StringUtils.isNotBlank(expression)) {
                                            Method createInstance = getDeclaredMethod(exclusiveGatewayBuilder, "createInstance", Class.class);
                                            createInstance.setAccessible(true);
                                            ConditionExpression conditionExpression = null;
                                            try {
                                                conditionExpression = (ConditionExpression) createInstance.invoke(exclusiveGatewayBuilder, ConditionExpression.class);
                                            } catch (IllegalAccessException e) {
                                                throw new RuntimeException(e);
                                            } catch (InvocationTargetException e) {
                                                throw new RuntimeException(e);
                                            }
                                            conditionExpression.setTextContent(expression);
                                            sequenceFlow.setConditionExpression(conditionExpression);
                                        }

                                        FlowElement flowElement2 = startFlowNodeBuilder.done().getModelElementById(sequenceFlow.getId());
                                        if(flowElement2!=null){
                                            flowElement2.setId(condition.getString("elseSequenceFlowId"));
                                            exclusiveGatewayBuilder.defaultFlow(exclusiveGatewayBuilder.done().getModelElementById(flowElement2.getId()));;
                                        }

                                        conditions.remove(0);
                                    }
                                });

                            }

                            // 1.1 边连接完成后，在进行 nextNode 创建
                            if (Objects.nonNull(nextNode) &&StringUtils.isNotBlank(nextNode.getId())) {
                                AbstractFlowNodeBuilder<?, ?> abstractFlowNodeBuilder = moveToNode(startFlowNodeBuilder, identifier);
                                return create(abstractFlowNodeBuilder,identifier, nextNode,  sequenceFlows,
                                    childNodeMap);
                            } else {
                                return identifier;
                            }
                        }


                    }
                }
            }
            else{
                System.err.println("-");
            }
        }
        return exclusiveGatewayId;
    }


    private static String createParallelGatewayBuilder(AbstractFlowNodeBuilder startFlowNodeBuilder,String formId,ChildNode flowNode,List<SequenceFlow> sequenceFlows,Map<String,ChildNode> childNodeMap) throws InvocationTargetException, IllegalAccessException {
        childNodeMap.put(flowNode.getId(),flowNode);
        String name = flowNode.getName();
        String parallelGatewayId = flowNode.getId();
        ParallelGatewayBuilder parallelGatewayBuilder = startFlowNodeBuilder.parallelGateway(parallelGatewayId).name(name);
        AbstractFlowNodeBuilder<?, ?> fromBuilder = moveToNode(startFlowNodeBuilder, formId);
        connect(fromBuilder,parallelGatewayBuilder,formId, parallelGatewayId,sequenceFlows,childNodeMap);
        if (Objects.isNull(flowNode.getBranchs()) && Objects.isNull(flowNode.getChildren())) {
            return parallelGatewayId;
        }

        List<ChildNode> flowNodes = flowNode.getBranchs();
        List<String> incoming = Lists.newArrayListWithCapacity(flowNodes.size());
        for (ChildNode element : flowNodes) {
            childNodeMap.put(element.getId(),element);
            ChildNode childNode = element.getChildren();
            if (Objects.isNull(childNode) ||  StringUtils.isBlank(childNode.getId())) {
                incoming.add(parallelGatewayId);
                continue;
            }
            String identifier = create(parallelGatewayBuilder,parallelGatewayId, childNode,sequenceFlows,childNodeMap);
            if (Objects.nonNull(identifier)) {
                incoming.add(identifier);
            }
        }

        ChildNode childNode = flowNode.getChildren();
        if (Objects.nonNull(childNode) &&StringUtils.isNotBlank(childNode.getId())) {
            String parentId = childNode.getParentId();
            ChildNode parentChildNode = childNodeMap.get(parentId);
            boolean conFlag = Type.CONCURRENTS.type
                .equals(parentChildNode.getType());
            if(!conFlag){
                String type = childNode.getType();
                if(!Type.EMPTY.type.equals(type)){

                }
                else{
                    if(Type.CONCURRENTS.type.equals(parentChildNode.getType())){
                        String endExId=  parentChildNode.getId()+"ex";
                        // 普通结束网关
                        if (CollectionUtils.isEmpty(incoming)) {
                            return create(parallelGatewayBuilder,parallelGatewayId, childNode,sequenceFlows,childNodeMap);
                        }
                        else {
                            JSONObject incomingObj = childNode.getIncoming();
                            // 所有 service task 连接 end parallel gateway
                            incomingObj.put("incoming", incoming);
                            // 1.0 先进行边连接, 暂存 nextNode
                            ChildNode nextNode = childNode.getChildren();
                            childNode.setChildren(null);
                            String identifier = endExId;
                            for (int i = 0; i < incoming.size(); i++) {
                                AbstractFlowNodeBuilder<?, ?> abstractFlowNodeBuilder = moveToNode(startFlowNodeBuilder, incoming.get(0));
                                ModelElementInstance endExNode = startFlowNodeBuilder.done().getModelElementById(identifier);
                                if(endExNode==null){
                                    abstractFlowNodeBuilder.parallelGateway(identifier);
                                }
                                AbstractFlowNodeBuilder<?, ?> toNode = moveToNode(startFlowNodeBuilder, identifier);
                                connect(abstractFlowNodeBuilder,toNode, incoming.get(i),identifier, sequenceFlows,childNodeMap);
                            }
                            // 1.1 边连接完成后，在进行 nextNode 创建
                            if (Objects.nonNull(nextNode)&&StringUtils.isNotBlank(nextNode.getId())) {
                                AbstractFlowNodeBuilder<?, ?> abstractFlowNodeBuilder = moveToNode(startFlowNodeBuilder, identifier);
                                return create(abstractFlowNodeBuilder,identifier, nextNode,sequenceFlows,childNodeMap);
                            } else {
                                return identifier;
                            }
                        }
                    }
                }
            }
            else{
                String type = childNode.getType();
                if(!Type.EMPTY.type.equals(type)){

                }
                else{
                    if(Type.CONCURRENTS.type.equals(parentChildNode.getType())){
                        String endExId=  parentChildNode.getId()+"ex";
                        // 普通结束网关
                        if (CollectionUtils.isEmpty(incoming)) {
                            return create(parallelGatewayBuilder,parallelGatewayId, childNode,sequenceFlows,childNodeMap);
                        }
                        else {
                            JSONObject incomingObj = childNode.getIncoming();
                            // 所有 service task 连接 end parallel gateway
                            incomingObj.put("incoming", incoming);
                            // 1.0 先进行边连接, 暂存 nextNode
                            ChildNode nextNode = childNode.getChildren();
                            childNode.setChildren(null);
                            String identifier = endExId;
                            for (int i = 0; i < incoming.size(); i++) {
                                AbstractFlowNodeBuilder<?, ?> abstractFlowNodeBuilder = moveToNode(startFlowNodeBuilder, incoming.get(0));
                                ModelElementInstance endExNode = startFlowNodeBuilder.done().getModelElementById(identifier);
                                if(endExNode==null){
                                    abstractFlowNodeBuilder.parallelGateway(identifier);
                                }
                                AbstractFlowNodeBuilder<?, ?> toNode = moveToNode(startFlowNodeBuilder, identifier);
                                connect(abstractFlowNodeBuilder,toNode, incoming.get(i),identifier, sequenceFlows,childNodeMap);
                            }
                            // 1.1 边连接完成后，在进行 nextNode 创建
                            if (Objects.nonNull(nextNode) &&StringUtils.isNotBlank(nextNode.getId())) {
                                AbstractFlowNodeBuilder<?, ?> abstractFlowNodeBuilder = moveToNode(startFlowNodeBuilder, identifier);
                                return create(abstractFlowNodeBuilder,identifier, nextNode,sequenceFlows,childNodeMap);
                            } else {
                                return identifier;
                            }
                        }
                    }
                }
            }

        }
        return parallelGatewayId;
    }
    private static AbstractFlowNodeBuilder<?, ?> moveToNode(AbstractFlowNodeBuilder<?, ?> startEventBuilder, String identifier) {
        return startEventBuilder.moveToNode(identifier);
    }
    /**
     * 循环向上转型, 获取对象的 DeclaredMethod
     *
     * @param object         : 子类对象
     * @param methodName     : 父类中的方法名
     * @param parameterTypes : 父类中的方法参数类型
     * @return 父类中的方法对象
     */
    private static Method getDeclaredMethod(Object object, String methodName, Class<?>... parameterTypes) {
        Method method = null;
        for (Class<?> clazz = object.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                method = clazz.getDeclaredMethod(methodName, parameterTypes);
                return method;
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    private static String createTask(AbstractFlowNodeBuilder startFlowNodeBuilder,ChildNode flowNode,List<SequenceFlow> sequenceFlows,Map<String,ChildNode> childNodeMap) throws InvocationTargetException, IllegalAccessException {
        JSONObject incomingJson = flowNode.getIncoming();
        List<String> incoming = incomingJson.getJSONArray("incoming").toJavaList(String.class);
        // 自动生成id
//        String id = id("serviceTask");
        String id=flowNode.getId();
        if (incoming != null && !incoming.isEmpty()) {
            // 创建 serviceTask
            AbstractFlowNodeBuilder<?, ?> abstractFlowNodeBuilder = moveToNode(startFlowNodeBuilder, incoming.get(0));
            // 自动生成id
            Method createTarget = getDeclaredMethod(abstractFlowNodeBuilder, "createTarget", Class.class);
            // 手动传入id
            createTarget.setAccessible(true);
            Object target = createTarget.invoke(abstractFlowNodeBuilder, UserTask.class);
            UserTask userTask = (UserTask)target;
            userTask.setName(flowNode.getName());
            userTask.setId(id);
            UserTaskBuilder builder = userTask.builder();
            connect(startFlowNodeBuilder,builder,incoming.get(0), id,sequenceFlows,childNodeMap);
            builder.camundaTaskListenerDelegateExpression(TaskListener.EVENTNAME_CREATE,"${taskCreatedListener}");

            if("root".equalsIgnoreCase(id)){
                userTask.setCamundaAssignee("${applyUserId}");
            }
            else{
                Properties props = flowNode.getProps();
                String mode = props.getMode();
                MultiInstanceLoopCharacteristicsBuilder multiInstanceLoopCharacteristics = builder.multiInstance().camundaCollection("${camundaFlowUtils.calculateTaskCandidateUsers(execution)}").camundaElementVariable("assignee");
                if(ModeEnums.OR.getTypeName().equals(mode)){
                    multiInstanceLoopCharacteristics.completionCondition("${nrOfCompletedInstances/nrOfInstances > 0}");
                }
                else if (ModeEnums.NEXT.getTypeName().equals(mode)){
                    multiInstanceLoopCharacteristics.sequential();
                }
                userTask.setCamundaAssignee("${assignee}");
                JSONObject timeLimit = props.getTimeLimit();
                if(timeLimit!=null && !timeLimit.isEmpty()){
                    JSONObject timeout = timeLimit.getJSONObject("timeout");
                    if(timeout!=null && !timeout.isEmpty()){
                        String unit = timeout.getString("unit");
                        Integer value = timeout.getInteger("value");
                        if(value>0){
//                            BoundaryEvent boundaryEvent = builder.done().newInstance(BoundaryEvent.class);
//                            boundaryEventBuilder.cancelActivity()
//                            List<BoundaryEvent> boundaryEvents= new ArrayList<>();
//                            boundaryEvent.setId(id("boundaryEvent"));
//                            boundaryEvent.setAttachedTo(userTask);
//                            boundaryEvent.setCancelActivity(Boolean.TRUE);
//                            TimerEventDefinition timerEventDefinition = new TimerEventDefinition();
//                            if("D".equals(unit)){
//                                timerEventDefinition.setTimeDuration("P"+value+unit);
//                            }
//                            else{
//                                timerEventDefinition.setTimeDuration("PT"+value+unit);
//                            }
//                            timerEventDefinition.setId(id("timerEventDefinition"));
//                            boundaryEvent.getEventDefinitions(timerEventDefinition);
//                            FlowableListener flowableListener = new FlowableListener();
//                            flowableListener.setEvent(ExecutionListener.EVENTNAME_END);
//                            flowableListener.setImplementationType(IMPLEMENTATION_TYPE_CLASS);
//                            flowableListener.setImplementation("com.dingding.mid.listener.TimerListener");
//                            List<FlowableListener> listenerList= new ArrayList<>();
//                            listenerList.add(flowableListener);
//                            boundaryEvent.setExecutionListeners(listenerList);
//                            process.addFlowElement(boundaryEvent);
//                            boundaryEvents.add(boundaryEvent);
//                            userTask.setBoundaryEvents(boundaryEvents);
                        }
                    }
                }

            }
        }
        return id;
    }




    private enum Type {

        /**
         * 并行事件
         */
        CONCURRENTS("CONCURRENTS", ParallelGateway.class),
        CONCURRENT("CONCURRENT", SequenceFlow.class),
        /**
         * 排他事件
         */
        CONDITION("CONDITION", ExclusiveGateway.class),
        CONDITIONS("CONDITIONS", ExclusiveGateway.class),
        /**
         * 任务
         */
        USER_TASK("APPROVAL", UserTask.class),
        EMPTY("EMPTY", Object.class),
        ROOT("ROOT", UserTask.class),
        CC("CC", ServiceTask.class),
        TRIGGER("TRIGGER", ServiceTask.class),
        DELAY("DELAY", IntermediateCatchEvent.class);
        private String type;

        private Class<?> typeClass;

        Type(String type, Class<?> typeClass) {
            this.type = type;
            this.typeClass = typeClass;
        }

        public final static Map<String, Class<?>> TYPE_MAP = Maps.newHashMap();

        static {
            for (Type element : Type.values()) {
                TYPE_MAP.put(element.type, element.typeClass);
            }
        }

        public boolean isEqual(String type) {
            return this.type.equals(type);
        }

    }


    public static  ChildNode getChildNode(ChildNode childNode,String nodeId){
        Map<String,ChildNode> childNodeMap =new HashMap<>();
        if(StringUtils.isNotBlank(childNode.getId())){
            getChildNode(childNode,childNodeMap);
        }

        Set<String> set = childNodeMap.keySet();
        for (String s : set) {
            if(StringUtils.isNotBlank(s)){
                if(s.equals(nodeId)){
                    return childNodeMap.get(s);
                }
            }
        }
        return null;
    }

    private  static  void getChildNode(ChildNode childNode,Map<String,ChildNode> childNodeMap){
        childNodeMap.put(childNode.getId(),childNode);
        List<ChildNode> branchs = childNode.getBranchs();
        ChildNode children = childNode.getChildren();
        if(branchs!=null && branchs.size()>0){
            for (ChildNode branch : branchs) {
                if(StringUtils.isNotBlank(branch.getId())){
                    childNodeMap.put(branch.getId(),branch);
                    getChildNode(branch,childNodeMap);
                }
            }
        }

        if(children!=null ){
            childNodeMap.put(children.getId(),children);
            getChildNode(children,childNodeMap);
        }

    }

}
