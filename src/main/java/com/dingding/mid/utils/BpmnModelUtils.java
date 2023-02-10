package com.dingding.mid.utils;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dingding.mid.dto.json.ChildNode;
import com.dingding.mid.dto.json.ConditionInfo;
import com.dingding.mid.dto.json.GroupsInfo;
import com.dingding.mid.dto.json.Properties;
import com.dingding.mid.entity.Users;
import com.dingding.mid.enums.ModeEnums;
import com.dingding.mid.exception.WorkFlowException;
import com.dingding.mid.service.UserService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import liquibase.pro.packaged.J;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.TaskListener;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import static com.dingding.mid.common.WorkFlowConstants.*;
import static org.flowable.bpmn.model.ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION;

/**
 * @author LoveMyOrange
 * @create 2022-10-10 17:47
 */
public class BpmnModelUtils {

    /**
     * 根据前缀生成唯一UUID
     * @param prefix    前缀
     * @return  prefix_xxxx
     */
    private static String id(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }

    /**
     * 根据名称创建ServiceTask
     * @param name 任务名称
     * @return
     */
    private static ServiceTask serviceTask(String name) {
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setName(name);
        return serviceTask;
    }

    /**
     * 节点连线，从上一个节点到当前节点的连线
     * @param from 上游节点ID
     * @param to    当前节点ID
     * @param sequenceFlows 连线列表
     * @param childNodeMap
     * @param process   流程定义
     * @return
     */
    public static SequenceFlow connect(String from, String to,List<SequenceFlow> sequenceFlows,Map<String,ChildNode> childNodeMap,Process process) {
        SequenceFlow flow = new SequenceFlow();
        String sequenceFlowId = id("sequenceFlow");

        if(process.getFlowElement(from) !=null && process.getFlowElement(from) instanceof ExclusiveGateway){ // 上游节点为排他网关
            ExclusiveGateway exclusiveGateway = (ExclusiveGateway)process.getFlowElement(from); // 上游节点
            ChildNode childNode = childNodeMap.get(to);
            if (childNode != null) {
                String parentId = childNode.getParentId();
                if (StringUtils.isNotBlank(parentId)) {
                    ChildNode parentNode = childNodeMap.get(parentId);
                    if (parentNode != null) {
                        if (Type.CONDITION.type.equals(parentNode.getType())) {

                            StringBuffer conditionExpression = new StringBuffer();

                            sequenceFlowId = parentNode.getId();
                            flow.setName(parentNode.getName());
                            //解析条件表达式
                            Properties props = parentNode.getProps();
                            String expression = props.getExpression();
                            List<GroupsInfo> groups = props.getGroups();
                            String groupsType = props.getGroupsType();
                            if (StringUtils.isNotBlank(expression)) {
                                conditionExpression.append("${").append(expression).append("}");
                            } else {
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
                                        if ("String".equals(valueType)) {
                                            if ("=".equals(compare)) {
                                                String str = StringUtils.join(value, ",");
                                                str = "'" + str + "'";
                                                conditionExpression.append(" " + EXPRESSION_CLASS + "strEqualsMethod(" + id + "," + str + ") ");
                                            } else {
                                                List<String> tempList = new ArrayList<>();
                                                for (Object o : value) {
                                                    String s = o.toString();
                                                    s = "'" + s + "'";
                                                    tempList.add(s);
                                                }
                                                String str = StringUtils.join(tempList, ",");
//                                                String str = StringUtils.join(value, ",");
                                                conditionExpression.append(" " + EXPRESSION_CLASS + "strContainsMethod(" + id + "," + str + ") ");
                                            }
                                        } else if ("Number".equals(valueType)) {
                                            String str = StringUtils.join(value, ",");
                                            if ("=".equals(compare)) {
                                                conditionExpression.append(" " + id + " == " + str + " ");
                                            } else if (">".equals(compare)) {
                                                conditionExpression.append("" + id + " > " + str + " ");
                                            } else if (">=".equals(compare)) {
                                                conditionExpression.append(" " + id + " >= " + str + " ");
                                            } else if ("<".equals(compare)) {
                                                conditionExpression.append(" " + id + " < " + str + " ");
                                            } else if ("<=".equals(compare)) {
                                                conditionExpression.append(" " + id + " <= " + str + " ");
                                            } else if ("IN".equals(compare)) {
                                                conditionExpression.append(" " + EXPRESSION_CLASS + "numberContains(" + id + "," + str + ") ");
                                            } else if ("B".equals(compare)) {
                                                conditionExpression.append("  " + EXPRESSION_CLASS + "b(" + id + "," + str + ") ");
                                            } else if ("AB".equals(compare)) {
                                                conditionExpression.append("  " + EXPRESSION_CLASS + "ab(" + id + "," + str + ") ");
                                            } else if ("BA".equals(compare)) {
                                                conditionExpression.append("  " + EXPRESSION_CLASS + "ba(" + id + "," + str + ") ");
                                            } else if ("ABA".equals(compare)) {
                                                conditionExpression.append("  " + EXPRESSION_CLASS + "aba(" + id + "," + str + ") ");
                                            }
                                        } else if ("User".equals(valueType)) {
                                            List<String> userIds = new ArrayList<>();
                                            for (Object o : value) {
                                                JSONObject obj = (JSONObject) o;
                                                userIds.add(obj.getString("id"));
                                            }
                                            String str = StringUtils.join(userIds, ",");
                                            conditionExpression.append(" " + EXPRESSION_CLASS + "strContainsMethod(" + id + "," + str + ") ");
                                        } else if ("Dept".equals(valueType)) {
                                            List<String> userIds = new ArrayList<>();
                                            List<String> deptIds = new ArrayList<>();
                                            for (Object o : value) {
                                                JSONObject obj = (JSONObject) o;
                                                String type = obj.getString("type");
                                                if ("dept".equals(type)) {
                                                    deptIds.add(obj.getString("id"));
                                                } else {
                                                    userIds.add(obj.getString("id"));
                                                }
                                            }

                                            if (CollUtil.isNotEmpty(deptIds)) {
                                                UserService userService = SpringContextHolder.getBean(UserService.class);
                                                LambdaQueryWrapper<Users> lambdaQueryWrapper = new LambdaQueryWrapper<>();
                                                lambdaQueryWrapper.in(Users::getDepartmentIds, deptIds);
                                                List<Users> list = userService.list(lambdaQueryWrapper);
                                                for (Users users : list) {
                                                    userIds.add(users.getUserId() + "");
                                                }

                                                if (userIds.isEmpty()) {userIds.add("0");}
                                                String str = StringUtils.join(userIds, ",");
                                                conditionExpression.append(" " + EXPRESSION_CLASS + "strContains(" + id + "," + str + ") ");
                                            }

                                        } else {
                                            continue;
                                        }

                                        if (conditions.size() > 1 && j != (conditions.size() - 1)) {
                                            if ("OR".equals(groupType)) {
                                                conditionExpression.append(" || ");
                                            } else {
                                                conditionExpression.append(" && ");
                                            }
                                        }

                                        if (i == (conditions.size() - 1)) {
                                            conditionExpression.append(" ");
                                        }
                                    }
                                    conditionExpression.append(" ) ");

                                    if (groups.size() > 1 && i != (groups.size() - 1)) {
                                        if ("OR".equals(groupsType)) {
                                            conditionExpression.append(" || ");
                                        } else {
                                            conditionExpression.append(" && ");
                                        }
                                    }
                                }
                                conditionExpression.append("} ");
                            }

                            // 排他网关默认流为空默认流不是本节点，才进行条件的设置
                            if(StringUtils.isNotBlank(exclusiveGateway.getDefaultFlow())
                                    && exclusiveGateway.getDefaultFlow().equals(parentId)){ // 排他网关的默认流

                            }else if(conditionExpression.length() > 0){
                                flow.setConditionExpression(conditionExpression.toString());
                            }
                        }
                    }
                }
            }
        }
        flow.setId(sequenceFlowId);
        flow.setSourceRef(from);
        flow.setTargetRef(to);
        sequenceFlows.add(flow);
        return flow;
    }

    private static String stringEquals(ConditionInfo condition) {
        return null;
    }

    /**
     * 创建开始节点
     * @return
     */
    public static StartEvent createStartEvent() {
        StartEvent startEvent = new StartEvent();
        startEvent.setId(START_EVENT_ID);
        startEvent.setInitiator("applyUserId");
        return startEvent;
    }

    /**
     * 创建结束节点
     * @return
     */
    public static EndEvent createEndEvent() {
        EndEvent endEvent = new EndEvent();
        endEvent.setId(END_EVENT_ID);
        return endEvent;
    }


    public static List<String> create(String fromId, ChildNode flowNode, Process process,BpmnModel bpmnModel,List<SequenceFlow> sequenceFlows,Map<String,ChildNode> childNodeMap) throws InvocationTargetException, IllegalAccessException {
        String nodeType = flowNode.getType(); // 当前节点的网关类型
        if (Type.CONCURRENTS.isEqual(nodeType)) { // 并行网关
            return createParallelGatewayBuilder(fromId, flowNode,process,bpmnModel,sequenceFlows,childNodeMap);
        } else if (Type.CONDITIONS.isEqual(nodeType)) { // 排他网关
            return createExclusiveGatewayBuilder(fromId, flowNode,process,bpmnModel,sequenceFlows,childNodeMap);
        } else if (Type.USER_TASK.isEqual(nodeType)) { // 用户网关
            childNodeMap.put(flowNode.getId(),flowNode);

            JSONObject incoming = flowNode.getIncoming();
            incoming.put("incoming", Collections.singletonList(fromId));

            String id = createTask(process,flowNode,sequenceFlows,childNodeMap);
            // 如果当前任务还有后续任务，则遍历创建后续任务
            ChildNode children = flowNode.getChildren();
            if (Objects.nonNull(children) &&StringUtils.isNotBlank(children.getId())) { // 有子项，递归创建
                return create(id, children,process,bpmnModel,sequenceFlows,childNodeMap);
            } else { // 终止条件：没有子项
                return Arrays.asList(id);
            }
        } else if(Type.ROOT.isEqual(nodeType)){ // 根网关
            childNodeMap.put(flowNode.getId(),flowNode);
            JSONObject incoming = flowNode.getIncoming();
            incoming.put("incoming", Collections.singletonList(fromId));
            String id = createTask(process,flowNode,sequenceFlows,childNodeMap);
            // 如果当前任务还有后续任务，则遍历创建后续任务
            ChildNode children = flowNode.getChildren();
            if (Objects.nonNull(children) &&StringUtils.isNotBlank(children.getId())) { // 有子项，递归创建
                return create(id, children,process,bpmnModel,sequenceFlows,childNodeMap);
            } else { // 终止条件：没有子项
                return Arrays.asList(id);
            }
        } else if(Type.DELAY.isEqual(nodeType)){ // 延时
            throw new WorkFlowException("还不想写这个功能");
//            childNodeMap.put(flowNode.getId(),flowNode);
//            JSONObject incoming = flowNode.getIncoming();
//            incoming.put("incoming", Collections.singletonList(fromId));
//            String id = createTask(process,flowNode,sequenceFlows,childNodeMap);
//            // 如果当前任务还有后续任务，则遍历创建后续任务
//            ChildNode children = flowNode.getChildren();
//            if (Objects.nonNull(children) &&StringUtils.isNotBlank(children.getId())) {
//                return create(id, children,process,bpmnModel,sequenceFlows,childNodeMap);
//            } else {
//                return id;
//            }
        }
        else if(Type.TRIGGER.isEqual(nodeType)){ // 定时
            throw new WorkFlowException("还不想写这个功能");
        }
        else if(Type.CC.isEqual(nodeType)){
            throw new WorkFlowException("代码呗回滚了 丢了,暂时先不做");
//            childNodeMap.put(flowNode.getId(),flowNode);
//            JSONObject incoming = flowNode.getIncoming();
//            incoming.put("incoming", Collections.singletonList(fromId));
//            String id = createServiceTask(process,flowNode,sequenceFlows,childNodeMap);
//            // 如果当前任务还有后续任务，则遍历创建后续任务
//            ChildNode children = flowNode.getChildren();
//            if (Objects.nonNull(children) &&StringUtils.isNotBlank(children.getId())) {
//                return create(id, children,process,bpmnModel,sequenceFlows,childNodeMap);
//            } else {
//                return id;
//            }
        }
        else {
            throw new RuntimeException("未知节点类型: nodeType=" + nodeType);
        }
    }

    private static List<String> createExclusiveGatewayBuilder(String formId,  ChildNode flowNode,Process process,BpmnModel bpmnModel,List<SequenceFlow> sequenceFlows,Map<String,ChildNode> childNodeMap) throws InvocationTargetException, IllegalAccessException {
        childNodeMap.put(flowNode.getId(),flowNode);
        String name =flowNode.getName();
        String exclusiveGatewayId = flowNode.getId(); // 排他网关节点ID（当前）

        // 1. 创建排他网关
        ExclusiveGateway exclusiveGateway = new ExclusiveGateway();
        exclusiveGateway.setId(exclusiveGatewayId);
        exclusiveGateway.setName(name);

        process.addFlowElement(exclusiveGateway);

        // 2. 顺序流：父节点->当前排他网关
        process.addFlowElement(connect(formId, exclusiveGatewayId, sequenceFlows, childNodeMap, process));

        if (Objects.isNull(flowNode.getBranchs()) && Objects.isNull(flowNode.getChildren())) { // 排他网关下面没有分支
            return Arrays.asList(exclusiveGatewayId);
        }

        List<ChildNode> flowNodes = flowNode.getBranchs(); // 排他网关分支
        List<String> incoming = Lists.newCopyOnWriteArrayList();

        List<JSONObject> conditions = Lists.newCopyOnWriteArrayList(); // 线程安全

        Map<String,JSONObject> branchConditions = new HashMap<>();

        // 创建排他网关的分支branch
        for (ChildNode element : flowNodes) {
            childNodeMap.put(element.getId(),element);
            ChildNode childNode = element.getChildren(); // 获取分支的子节点

            String nodeName = element.getName();
            Properties props = element.getProps();
            String expression = props.getExpression();

            if(props !=null && props.isDefaultBranch()){ // 默认流
                exclusiveGateway.setDefaultFlow(element.getId());
            }

            JSONObject condition = new JSONObject();
            condition.fluentPut("nodeName", nodeName)
                    .fluentPut("expression", expression)
                    .fluentPut("groups",props.getGroups())
                    .fluentPut("groupsType",props.getGroupsType())
                    .fluentPut("finalSequenceFlowId",element.getId());
            conditions.add(condition);

            if(childNode==null || StringUtils.isBlank(childNode.getId())){ // 没有子项
                incoming.add(element.getId());
            }else{ // 有子项继续创建

                incoming.addAll(create(exclusiveGatewayId, childNode,process,bpmnModel,sequenceFlows,childNodeMap));

                List<SequenceFlow> flows = sequenceFlows.stream()
                        .filter(flow -> StringUtils.equals(exclusiveGatewayId, flow.getSourceRef()))
                        .collect(Collectors.toList());
                flows.forEach(
                        e -> {
                            if (StringUtils.isBlank(e.getName()) && StringUtils.isNotBlank(nodeName)) {
                                e.setName(nodeName);
                            }
                            // 设置条件表达式
                            if (Objects.isNull(e.getConditionExpression())
                                    && StringUtils.isNotBlank(expression)
                                    && !e.getId().equals(exclusiveGateway.getDefaultFlow())) {
                                e.setConditionExpression(expression);
                            }
                        }
                );
            }
        }

        // 遍历child
//        ChildNode childNode = flowNode.getChildren();
//        if (Objects.nonNull(childNode) &&StringUtils.isNotBlank(childNode.getId()) ) {
//            String parentId = childNode.getParentId();
//            ChildNode parentChildNode = childNodeMap.get(parentId);
//            boolean conFlag = Type.CONCURRENTS.type.equals(parentChildNode.getType()); // 并行
//            if(!conFlag) { // 非并行网关
//                String type = childNode.getType();
//                if(!Type.EMPTY.type.equals(type)){
//                }
//                else{
//                    if(Type.CONDITIONS.type.equals(parentChildNode.getType())){ // 网关分支
//
//                        // 这里为什么塞个排他网关在后面
//                        String endExId=parentChildNode.getId()+"end";
//                        process.addFlowElement(createExclusiveGateWayEnd(endExId));
//
//
//                        if (incoming.isEmpty()) {
//                            return create(exclusiveGatewayId, childNode, process, bpmnModel, sequenceFlows, childNodeMap);
//                        }
//                        else {
//                            // 1.0 先进行边连接, 暂存 nextNode
//                            ChildNode nextNode = childNode.getChildren();
//                            childNode.setChildren(null);
//                            for (int i = 0; i < incoming.size(); i++) {
//                                process.addFlowElement(connect(incoming.get(i), endExId, sequenceFlows,childNodeMap,process));
//                            }
//
//                            //  针对 gateway 空任务分支 添加条件表达式
//                            if (!branchConditions.isEmpty()) {
//                                FlowElement flowElement1 = bpmnModel.getFlowElement(endExId);
//                                // 获取从 gateway 到目标节点 未设置条件表达式的节点
//                                List<SequenceFlow> flows = sequenceFlows.stream().filter(
//                                                flow -> StringUtils.equals(flowElement1.getId(), flow.getTargetRef()))
//                                        .filter(
//                                                flow -> StringUtils.equals(flow.getSourceRef(), exclusiveGatewayId))
//                                        .collect(Collectors.toList());
//
//                                flows.forEach(sequenceFlow -> {
//                                    if (!condition.isEmpty()) {
//                                        String nodeName = condition.getString("nodeName");
//                                        String expression = condition.getString("expression");
//
//                                        if (StringUtils.isBlank(sequenceFlow.getName()) && StringUtils
//                                                .isNotBlank(nodeName)) {
//                                            sequenceFlow.setName(nodeName);
//                                        }
//                                        // 设置条件表达式
//                                        if (Objects.isNull(sequenceFlow.getConditionExpression())
//                                                && StringUtils.isNotBlank(expression)) {
//                                            sequenceFlow.setConditionExpression(expression);
//                                        }
//
//                                        FlowElement flowElement2 = process.getFlowElement(sequenceFlow.getId());
//                                        if(flowElement2!=null){
//                                            flowElement2.setId(condition.getString("finalSequenceFlowId"));
//                                            exclusiveGateway.setDefaultFlow(flowElement2.getId());;
//                                        }
//
//                                        conditions.remove(0);
//                                    }
//                                });
//
//                            }
//
//                            // 1.1 边连接完成后，在进行 nextNode 创建
//                            if (Objects.nonNull(nextNode) &&StringUtils.isNotBlank(nextNode.getId())) {
//                                return create(endExId, nextNode, process, bpmnModel, sequenceFlows,
//                                        childNodeMap);
//                            } else {
//                                return endExId;
//                            }
//                        }
//                    }
//                }
//            }
//            else{
//                System.err.println("-");
//            }
//        }
        return incoming;
    }

    public static ExclusiveGateway createExclusiveGateWayEnd(String id){
        ExclusiveGateway exclusiveGateway=new ExclusiveGateway();
        exclusiveGateway.setId(id);
        return exclusiveGateway;
    }

    private static ParallelGateway createParallelGateWayEnd(String id){
        ParallelGateway parallelGateway=new ParallelGateway();
        parallelGateway.setId(id);
        return parallelGateway;
    }

    /**
     * 创建并行网关
     * @param formId
     * @param flowNode
     * @param process
     * @param bpmnModel
     * @param sequenceFlows
     * @param childNodeMap
     * @return
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private static List<String> createParallelGatewayBuilder(String formId, ChildNode flowNode,Process process,BpmnModel bpmnModel,List<SequenceFlow> sequenceFlows,Map<String,ChildNode> childNodeMap) throws InvocationTargetException, IllegalAccessException {
        childNodeMap.put(flowNode.getId(),flowNode);
        String name = flowNode.getName();
        String parallelGatewayId = flowNode.getId();

        // 并行网关
        ParallelGateway parallelGateway = new ParallelGateway();
        parallelGateway.setId(parallelGatewayId);
        parallelGateway.setName(name);

        process.addFlowElement(parallelGateway); // 新增并行网关
        process.addFlowElement(connect(formId, parallelGatewayId,sequenceFlows,childNodeMap,process)); // 新增连线

        if (Objects.isNull(flowNode.getBranchs()) && Objects.isNull(flowNode.getChildren())) { // 没有分支/子项：返回并行网关ID
            return Arrays.asList(parallelGatewayId);
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
            incoming.addAll(create(parallelGatewayId, childNode,process,bpmnModel,sequenceFlows,childNodeMap));
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
                        String endExId=  parentChildNode.getId()+"end";
                        process.addFlowElement(createParallelGateWayEnd(endExId));
                        // 普通结束网关
                        if (CollectionUtils.isEmpty(incoming)) {
                            return create(parallelGatewayId, childNode,process,bpmnModel,sequenceFlows,childNodeMap);
                        }
                        else {
                            JSONObject incomingObj = childNode.getIncoming();
                            // 所有 service task 连接 end parallel gateway
                            incomingObj.put("incoming", incoming);
                            FlowElement flowElement = bpmnModel.getFlowElement(incoming.get(0));
                            // 1.0 先进行边连接, 暂存 nextNode
                            ChildNode nextNode = childNode.getChildren();
                            childNode.setChildren(null);
                            String identifier = endExId;
                            for (int i = 0; i < incoming.size(); i++) {
                                FlowElement flowElement1 = bpmnModel.getFlowElement(incoming.get(i));
                                process.addFlowElement(connect(flowElement1.getId(), identifier,sequenceFlows,childNodeMap,process));
                            }
                            // 1.1 边连接完成后，在进行 nextNode 创建
                            if (Objects.nonNull(nextNode)&&StringUtils.isNotBlank(nextNode.getId())) {
                                return create(identifier, nextNode,process,bpmnModel,sequenceFlows,childNodeMap);
                            } else {
                                return Arrays.asList(identifier);
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
                        String endExId=  parentChildNode.getId()+"end";
                        process.addFlowElement(createParallelGateWayEnd(endExId));
                        // 普通结束网关
                        if (CollectionUtils.isEmpty(incoming)) {
                            return create(parallelGatewayId, childNode,process,bpmnModel,sequenceFlows,childNodeMap);
                        }
                        else {
                            JSONObject incomingObj = childNode.getIncoming();
                            // 所有 service task 连接 end parallel gateway
                            incomingObj.put("incoming", incoming);
                            FlowElement flowElement = bpmnModel.getFlowElement(incoming.get(0));
                            // 1.0 先进行边连接, 暂存 nextNode
                            ChildNode nextNode = childNode.getChildren();
                            childNode.setChildren(null);
                            String identifier = endExId;
                            for (int i = 0; i < incoming.size(); i++) {
                                FlowElement flowElement1 = bpmnModel.getFlowElement(incoming.get(i));
                                process.addFlowElement(connect(flowElement1.getId(), identifier,sequenceFlows,childNodeMap,process));
                            }
                            // 1.1 边连接完成后，在进行 nextNode 创建
                            if (Objects.nonNull(nextNode) &&StringUtils.isNotBlank(nextNode.getId())) {
                                return create(identifier, nextNode,process,bpmnModel,sequenceFlows,childNodeMap);
                            } else {
                                return Arrays.asList(identifier);
                            }
                        }
                    }
                }
            }

        }
        return Arrays.asList(parallelGatewayId);
    }

    /**
     * 创建用户任务
     * @param process 流程实例
     * @param flowNode  json配置实例
     * @param sequenceFlows 连线列表
     * @param childNodeMap  节点列表
     * @return
     */
    private static String createTask(Process process,ChildNode flowNode,List<SequenceFlow> sequenceFlows,Map<String,ChildNode> childNodeMap) {
        JSONObject incomingJson = flowNode.getIncoming();
        List<String> incoming = incomingJson.getJSONArray("incoming").toJavaList(String.class);
        // 自动生成id
//        String id = id("serviceTask");
        String id=flowNode.getId(); // 当前节点
        if (incoming != null && !incoming.isEmpty()) { // 上游节点
            UserTask userTask = new UserTask();
            userTask.setName(flowNode.getName());
            userTask.setId(id);
            process.addFlowElement(userTask);

            // 上游节点Id
            String fromId = incoming.get(0);

            process.addFlowElement(connect(fromId, id,sequenceFlows,childNodeMap,process));

            ArrayList<FlowableListener> taskListeners = new ArrayList<>();
            FlowableListener taskListener = new FlowableListener();
            // 事件类型,
            taskListener.setEvent(TaskListener.EVENTNAME_CREATE);
            // 监听器类型
            taskListener.setImplementationType(IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
            // 设置实现了，这里设置监听器的类型是delegateExpression，这样可以在实现类注入Spring bean.
            taskListener.setImplementation("${taskCreatedListener}");
            taskListeners.add(taskListener);
            userTask.setTaskListeners(taskListeners);
            if("root".equalsIgnoreCase(id)){
                return id;
            }
            else{
                ArrayList<FlowableListener> listeners = new ArrayList<>();
                FlowableListener activitiListener = new FlowableListener();
                // 事件类型,
                activitiListener.setEvent(ExecutionListener.EVENTNAME_START);
                // 监听器类型
                activitiListener.setImplementationType(IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
                // 设置实现了，这里设置监听器的类型是delegateExpression，这样可以在实现类注入Spring bean.
                activitiListener.setImplementation("${counterSignListener}");
                listeners.add(activitiListener);
                userTask.setExecutionListeners(listeners);
                Properties props = flowNode.getProps();
                String mode = props.getMode();
                MultiInstanceLoopCharacteristics multiInstanceLoopCharacteristics = new MultiInstanceLoopCharacteristics();
                // 审批人集合参数
                multiInstanceLoopCharacteristics.setInputDataItem(userTask.getId()+"assigneeList");
                // 迭代集合
                multiInstanceLoopCharacteristics.setElementVariable("assigneeName");
                // 并行
                multiInstanceLoopCharacteristics.setSequential(false);
                userTask.setAssignee("${assigneeName}");
                // 设置多实例属性
                userTask.setLoopCharacteristics(multiInstanceLoopCharacteristics);
                if(ModeEnums.OR.getTypeName().equals(mode)){
                    multiInstanceLoopCharacteristics.setCompletionCondition("${nrOfCompletedInstances/nrOfInstances > 0}");
                }
                else if (ModeEnums.NEXT.getTypeName().equals(mode)){
                    multiInstanceLoopCharacteristics.setSequential(true);
                }
            }
        }
        return id;
    }

    private static String createServiceTask(Process process,ChildNode flowNode,List<SequenceFlow> sequenceFlows,Map<String,ChildNode> childNodeMap) {
        JSONObject incomingJson = flowNode.getIncoming();
        List<String> incoming = incomingJson.getJSONArray("incoming").toJavaList(String.class);
        String id=flowNode.getId();
        if (incoming != null && !incoming.isEmpty()) {

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
