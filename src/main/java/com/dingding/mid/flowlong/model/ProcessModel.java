/*
 * 爱组搭 http://aizuda.com 低代码组件化开发平台
 * ------------------------------------------
 * 受知识产权保护，请勿删除版权申明
 */
package com.dingding.mid.flowlong.model;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.dingding.mid.dto.json.*;
import com.dingding.mid.enums.AssigneeTypeEnums;
import com.dingding.mid.enums.TypesEnums;
import com.dingding.mid.exception.WorkFlowException;
import com.dingding.mid.flowlong.assist.Assert;
import com.dingding.mid.flowlong.cache.FlowCache;
import com.dingding.mid.flowlong.core.FlowLongContext;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 爱组搭 http://aizuda.com
 * ----------------------------------------
 * JSON BPM 模型
 *
 * @author 青苗
 * @since 2023-03-17
 */
@Getter
@Setter
public class ProcessModel {
    /**
     * 节点名称
     */
    private String name;
    /**
     * 实例地址
     */
    private String instanceUrl;
    /**
     * 节点信息
     */
    private NodeModel nodeConfig;

    /**
     * 获取process定义的指定节点名称的节点模型
     *
     * @param nodeName 节点名称
     * @return {@link NodeModel}
     */
    public NodeModel getNode(String nodeName) {
        return null == nodeConfig ? null : nodeConfig.getNode(nodeName);
    }

    /**
     * 流程文件字节码解析为流程模型
     *
     * @param content   流程定义内容
     * @param processId 流程定义ID
     */
    public static ProcessModel parse(String content, Long processId) {
        // 缓存解析逻辑
        if (null != processId) {
            final String cacheKey = "flwProcessModel#" + processId;
            FlowCache flowCache = FlowLongContext.FLOW_CACHE;
            ProcessModel processModel = flowCache.get(cacheKey);
            if (null == processModel) {
                processModel = parseProcessModel(content);
                flowCache.put(cacheKey, processModel);
            }
            return processModel;
        }

        // 未缓存解析逻辑
        return parseProcessModel(content);
    }

    private static ProcessModel parseProcessModel(String content) {
        ProcessModel parseModel =new ProcessModel();
        parseModel.setName("测试");
        parseModel.setInstanceUrl("---");
        ChildNode childNode = JSONObject.parseObject(content, new TypeReference<ChildNode>() {
        });

        iteratorChildNode(childNode,parseModel,null);

//        throw new WorkFlowException("--");
        return parseModel;
//        ProcessModel processModel = FlowLongContext.fromJson(content, ProcessModel.class);
//        Assert.isNull(processModel, "process model json parser error");
//        processModel.buildParentNode(processModel.getNodeConfig());
//        return processModel;
    }

    private static void iteratorChildNode(ChildNode childNode,ProcessModel processModel,BaseEx viewNodeModel) {
        if(TypesEnums.ROOT.getType().equals(childNode.getType())){
            NodeModel nodeModel = new NodeModel();
            nodeModel.setNodeName(childNode.getName());
            nodeModel.setType(typeConvert(childNode.getType()));

            Properties props = childNode.getProps();
            List<UserInfo> assignedUser = props.getAssignedUser();
            List<NodeAssignee> nodeAssigneeList = new ArrayList<>();
            for (UserInfo userInfo : assignedUser) {
                    NodeAssignee nodeAssignee = new NodeAssignee();
                    nodeAssignee.setId(userInfo.getId());
                    nodeAssignee.setName(userInfo.getName());
                    nodeAssigneeList.add(nodeAssignee);
            }
            nodeModel.setNodeRoleList(nodeAssigneeList);
            processModel.setNodeConfig(nodeModel);
            iteratorChildNode(childNode.getChildren(),processModel,nodeModel);
        }
        else if(TypesEnums.USER_TASK.getType().equals(childNode.getType())){
            NodeModel nodeModel = new NodeModel();
            nodeModel.setNodeName(childNode.getName());
            nodeModel.setType(typeConvert(childNode.getType()));
            setTypeConvert(childNode.getProps().getAssignedType(),nodeModel,childNode);
            //设置超时时间
            JSONObject timeLimit = childNode.getProps().getTimeLimit();
            if(timeLimit!=null && !timeLimit.isEmpty()){

                JSONObject timeout = timeLimit.getJSONObject("timeout");
                String value = timeout.getString("value");
                String unit = timeout.getString("unit");
                if(value!=null&& Integer.valueOf(value)>0){
                    nodeModel.setTermAuto(Boolean.TRUE);
                    Integer integer = Integer.valueOf(value);
                    if("D".equals(unit)){
                        nodeModel.setTerm(integer*24);
                    }
                    else if("H".equals(unit)){
                        nodeModel.setTerm(integer);
                    }

                    //设置->
                    JSONObject handler = timeLimit.getJSONObject("handler");
                    String type = handler.getString("type");
                    if("PASS".equals(type)){
                        nodeModel.setTermMode(0);
                    }
                    else if("REFUSE".equals(type)){
                        nodeModel.setTermMode(1);
                    }
                }
                else{
                    nodeModel.setTermAuto(Boolean.FALSE);
                }
            }
            else{
                nodeModel.setTermAuto(Boolean.FALSE);
            }
            //设置会签
            /*
            *           <el-radio label="NEXT">会签 （按选择顺序审批，每个人必须同意）</el-radio>
            <el-radio label="AND">会签（可同时审批，每个人必须同意）</el-radio>
            <el-radio label="OR">或签（有一人同意即可）</el-radio>
            * */
            String mode = childNode.getProps().getMode();
            if("NEXT".equals(mode)){
                nodeModel.setExamineMode(1);
            }
            else if("AND".equals(mode)){
                nodeModel.setExamineMode(2);
            }
            else if("OR".equals(mode)){
                nodeModel.setExamineMode(3);
            }
            viewNodeModel.setChildNode(nodeModel);
            iteratorChildNode(childNode.getChildren(),processModel,nodeModel);
        }
        else if(TypesEnums.CC.getType().equals(childNode.getType())){
            NodeModel nodeModel = new NodeModel();
            nodeModel.setNodeName(childNode.getName());
            nodeModel.setType(typeConvert(childNode.getType()));

            List<UserInfo> assignedUser = childNode.getProps().getAssignedUser();
            List<NodeAssignee> nodeAssigneeList = new ArrayList<>();
            for (UserInfo userInfo : assignedUser) {
                NodeAssignee nodeAssignee = new NodeAssignee();
                nodeAssignee.setId(userInfo.getId());
                nodeAssignee.setName(userInfo.getName());
                nodeAssigneeList.add(nodeAssignee);
            }
            nodeModel.setNodeUserList(nodeAssigneeList);

            Boolean shouldAdd = childNode.getProps().getShouldAdd();
            if(shouldAdd){
                nodeModel.setUserSelectFlag(Boolean.TRUE);
            }
            else{
                nodeModel.setUserSelectFlag(Boolean.FALSE);
            }
            viewNodeModel.setChildNode(nodeModel);
            iteratorChildNode(childNode.getChildren(),processModel,nodeModel);
        }
        else if(TypesEnums.CONDITIONS.getType().equals(childNode.getType())){
            NodeModel nodeModel = new NodeModel();
            nodeModel.setNodeName(childNode.getName());
            nodeModel.setType(typeConvert(childNode.getType()));
            viewNodeModel.setChildNode(nodeModel);
            List<ChildNode> branchs = childNode.getBranchs();
            List<ConditionNode> conditionNodes = new ArrayList<>();
            for (ChildNode branch : branchs) {
                ConditionNode conditionModel=new ConditionNode();
                conditionModel.setNodeName(branch.getName());
                conditionModel.setType(4);
                conditionModel.setPriorityLevel(1);
                //TODO flowong 目前只支持单条件组
                List<GroupsInfo> groups = branch.getProps().getGroups();
                GroupsInfo groupsInfo = groups.get(0);
                String groupType = groupsInfo.getGroupType();
                conditionModel.setConditionMode("OR".equals(groupType)?2:1);
                List<ConditionInfo> conditions = groupsInfo.getConditions();
                List<NodeExpression> nodeExpressions = new ArrayList<>();
                for (ConditionInfo condition : conditions) {
                   NodeExpression nodeExpression = new NodeExpression();
                   nodeExpression.setLabel("===");
                   nodeExpression.setField(condition.getId());
                   nodeExpression.setOperator(condition.getValueType());
                   nodeExpression.setValue(JSONObject.toJSONString(condition.getValue()));
                    nodeExpressions.add(nodeExpression);
                }
                //
                conditionNodes.add(conditionModel);
                iteratorChildNode(branch.getChildren(),processModel,conditionModel);

            }
            viewNodeModel.setConditionNodes(conditionNodes);
        }
    }

    private static void setTypeConvert(String assignedType, NodeModel nodeModel,ChildNode childNode) {
        if(AssigneeTypeEnums.ASSIGN_USER.getTypeName().equals(assignedType)){
            nodeModel.setSetType(1);
            List<UserInfo> assignedUser = childNode.getProps().getAssignedUser();
            List<NodeAssignee> nodeAssigneeList = new ArrayList<>();
            for (UserInfo userInfo : assignedUser) {
                NodeAssignee nodeAssignee = new NodeAssignee();
                nodeAssignee.setId(userInfo.getId());
                nodeAssignee.setName(userInfo.getName());
                nodeAssigneeList.add(nodeAssignee);
            }
            nodeModel.setNodeUserList(nodeAssigneeList);
        }
        else if(AssigneeTypeEnums.LEADER.getTypeName().equals(assignedType)){
            nodeModel.setSetType(2);
            Map<String, Object> leader = childNode.getProps().getLeader();
            Integer level = MapUtil.getInt(leader, "level");
            nodeModel.setExamineLevel(level);
        }
        else if(AssigneeTypeEnums.ROLE.getTypeName().equals(assignedType)){
            nodeModel.setSetType(3);
            List<UserInfo> assignedUser = childNode.getProps().getAssignedUser();
            List<NodeAssignee> nodeAssigneeList = new ArrayList<>();
            for (UserInfo userInfo : assignedUser) {
                NodeAssignee nodeAssignee = new NodeAssignee();
                nodeAssignee.setId(userInfo.getId());
                nodeAssignee.setName(userInfo.getName());
                nodeAssigneeList.add(nodeAssignee);
            }
            nodeModel.setNodeRoleList(nodeAssigneeList);
        }
        else if(AssigneeTypeEnums.SELF_SELECT.getTypeName().equals(assignedType)){
            nodeModel.setSetType(4);
            Map<String, Object> selfSelect = childNode.getProps().getSelfSelect();
            Boolean multiple = MapUtil.getBool(selfSelect, "multiple");
            nodeModel.setSelectMode(multiple==true?2:1);
        }
        else if(AssigneeTypeEnums.SELF.getTypeName().equals(assignedType)){
            nodeModel.setSetType(5);
        }
        else if(AssigneeTypeEnums.LEADER_TOP.getTypeName().equals(assignedType)){
            nodeModel.setSetType(7);
            Map<String, Object> leaderTop = childNode.getProps().getLeaderTop();
            String endCondition = MapUtil.getStr(leaderTop, "endCondition");
            if("TOP".equals(endCondition)){
                nodeModel.setDirectorMode(1);
            }
            else{
                Integer endLevel = MapUtil.getInt(leaderTop, "endLevel");
                nodeModel.setDirectorLevel(endLevel);
            }
        }
    }

    private static Integer typeConvert(String type) {
        if(TypesEnums.ROOT.getType().equals(type)){
            return 0;
        }
        /*
        *      * 0，发起人
         * 1，审批人
         * 2，抄送人
         * 3，条件审批
         * 4，条件分支
        * */
        else if(TypesEnums.USER_TASK.getType().equals(type)){
            return 1;
        }
        else if(TypesEnums.CC.getType().equals(type)){
            return 2;
        }
        else if(TypesEnums.CONDITIONS.getType().equals(type)){
            return 3;
        }
        else if(TypesEnums.CONDITION.getType().equals(type)){
            return 4;
        }
        else {
            throw new WorkFlowException("sb+nc");
        }
    }

    /**
     * 构建父节点
     *
     * @param rootNode 根节点
     */
    protected void buildParentNode(NodeModel rootNode) {
        List<ConditionNode> conditionNodes = rootNode.getConditionNodes();
        if (null != conditionNodes) {
            for (ConditionNode conditionNode : conditionNodes) {
                NodeModel conditionChildNode = conditionNode.getChildNode();
                if (null != conditionChildNode) {
                    conditionChildNode.setParentNode(rootNode);
                    this.buildParentNode(conditionChildNode);
                }
            }
        }
        NodeModel childNode = rootNode.getChildNode();
        if (null != childNode) {
            childNode.setParentNode(rootNode);
            this.buildParentNode(childNode);
        }
    }
}
