/*
 * 爱组搭 http://aizuda.com 低代码组件化开发平台
 * ------------------------------------------
 * 受知识产权保护，请勿删除版权申明
 */
package com.dingding.mid.flowlong.model;

import cn.hutool.core.bean.BeanUtil;
import com.dingding.mid.dto.json.ChildNode;
import com.dingding.mid.dto.json.HttpInfo;
import com.dingding.mid.flowlong.Expression;
import com.dingding.mid.flowlong.ModelInstance;
import com.dingding.mid.flowlong.assist.Assert;
import com.dingding.mid.flowlong.assist.ObjectUtils;
import com.dingding.mid.flowlong.core.Execution;
import com.dingding.mid.flowlong.core.FlowLongContext;
import com.dingding.mid.flowlong.handler.impl.CreateTaskHandler;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * 爱组搭 http://aizuda.com
 * ----------------------------------------
 * JSON BPM 节点
 *
 * @author 青苗
 * @since 2023-03-17
 */
@Getter
@Setter
public class NodeModel extends BaseEx implements ModelInstance {
    ///////////触发器start/////////////////
    private HttpInfo httpInfo;
    ///////////触发器end//////////////////



    //todo 暂时把childNode存进去,因为不想去修改前端渲染方式
    private ChildNode wChildNode;
    /**
     * 节点名称
     */
    private String nodeName;
    /**
     * 节点类型
     * <p>
     * 0，发起人
     * 1，审批人
     * 2，抄送人
     * 3，条件审批
     * 4，条件分支
     * 5. 触发器
     *
     * </p>
     */
    private Integer type;
    /**
     * 审核人类型
     * <p>
     * 1，指定成员
     * 2，主管
     * 3，角色
     * 4，发起人自选
     * 5，发起人自己
     * 6. 表单选人
     * 7，连续多级主管
     * </p>
     */
    private Integer setType;
    /**
     * 表单选人的话,,这个值
     */
    private String formField;
    /**
     * 审核人成员
     */
    private List<NodeAssignee> nodeUserList;
    /**
     * 审核角色
     */
    private List<NodeAssignee> nodeRoleList;
    /**
     * 指定主管层级
     */
    private Integer examineLevel;
    /**
     * 自定义连续主管审批层级
     */
    private Integer directorLevel;
    /**
     * 发起人自选类型
     * <p>
     * 1，自选一个人
     * 2，自选多个人
     * </p>
     */
    private Integer selectMode;
    /**
     * 审批期限超时自动审批
     */
    private Boolean termAuto;
    /**
     * 审批期限
     */
    private Integer term;
    /**
     * 审批期限超时后执行类型
     */
    private Integer termMode;
    /**
     * 多人审批时审批方式 {@link }
     * <p>
     * 1，按顺序依次审批
     * 2，会签 (可同时审批，每个人必须审批通过)
     * 3，或签 (有一人审批通过即可)
     * </p>
     */
    private Integer examineMode;
    /**
     * 连续主管审批方式
     * <p>
     * 1，直到最上级主管
     * 2，自定义审批终点
     * </p>
     */
    private Integer directorMode;
    /**
     * 条件节点列表
     */
//    private List<ConditionNode> conditionNodes;
    /**
     * 允许发起人自选抄送人
     */
    private Boolean userSelectFlag;
    /**
     * 子节点
     */
//    private NodeModel childNode;
    /**
     * 父节点，模型 json 不存在该属性、属于逻辑节点
     */
    private NodeModel parentNode;

    @Override
    public void execute(FlowLongContext flowLongContext, Execution execution) {
        if (ObjectUtils.isNotEmpty(this.getConditionNodes())) {
            /**
             * 执行条件分支
             */
            Map<String, Object> args = execution.getArgs();
            Assert.illegalArgument(ObjectUtils.isEmpty(args), "Execution parameter cannot be empty");
            Expression expression = flowLongContext.getExpression();
            Assert.isNull(expression, "Interface Expression not implemented");

            List<ConditionNode> conditionNodes = this.getConditionNodes();
            List<ConditionNode> newArrConditionNodes=new ArrayList<>();
            for (ConditionNode conditionNode : conditionNodes) {
                ConditionNode conditionNode1 = new ConditionNode();
                BeanUtil.copyProperties(conditionNode,conditionNode1);
                newArrConditionNodes.add(conditionNode1);
            }


            ConditionNode defaultConditionNode= null;
            for (ConditionNode node : newArrConditionNodes) {
                if(Boolean.TRUE.equals(node.getTypeElse())){
                    defaultConditionNode=node;
                }
            }
            //先移除默认条件分支
            newArrConditionNodes.remove(defaultConditionNode);

            ConditionNode trueConditionNode=null;
            //
            Boolean result=Boolean.FALSE;
            for (ConditionNode node : newArrConditionNodes) {
                String methodStr = node.getMethodStr();
                if (null != methodStr) {
                    try {
                        result = expression.eval(Boolean.class, methodStr, args);
                        trueConditionNode=node;
                        break;//有true直接返回
                    } catch (Throwable e) {
                        result = false;
                        e.printStackTrace();
                    }
                }

            }

            //说明非默认都为true
            if(Boolean.FALSE.equals(result)){
                /**
                 * 执行创建条件任务
                 */
                this.createTask(defaultConditionNode.getChildNode(), flowLongContext, execution);
            }
            else{
                /**
                 * 执行创建条件任务
                 */
                this.createTask(trueConditionNode.getChildNode(), flowLongContext, execution);
            }
        }

        /**
         * 执行创建抄送、审批任务
         */
        if (Objects.equals(2, this.type) || Objects.equals(1, this.type) ||Objects.equals(5, this.type)) {
            this.createTask(flowLongContext, execution);
        }
    }

    public void createTask(FlowLongContext flowLongContext, Execution execution) {
        this.createTask(this, flowLongContext, execution);
    }

    protected void createTask(NodeModel nodeModel, FlowLongContext flowLongContext, Execution execution) {
        new CreateTaskHandler(nodeModel).handle(flowLongContext, execution);
    }

    /**
     * 获取process定义的指定节点名称的节点模型
     *
     * @param nodeName 节点名称
     * @return {@link NodeModel}
     */
    public NodeModel getNode(String nodeName) {
        if (Objects.equals(this.nodeName, nodeName)) {
            return this;
        }
        if (null != this.getConditionNodes()) {
            NodeModel fromConditionNode = getFromConditionNodes(nodeName);
            if (fromConditionNode != null) {
                return fromConditionNode;
            }
        }
        // 条件节点中没有找到 那么去它的同级子节点中继续查找
        if (null != this.getChildNode()) {
            return this.getChildNode().getNode(nodeName);
        }
        return null;
    }

    /**
     * 从条件节点中获取节点
     *
     * @param nodeName 节点名称
     * @return {@link NodeModel}
     */
    private NodeModel getFromConditionNodes(String nodeName) {
        for (ConditionNode conditionNode : getConditionNodes()) {
            NodeModel conditionChildNode = conditionNode.getChildNode();
            if (null != conditionChildNode) {
                NodeModel nodeModel = conditionChildNode.getNode(nodeName);
                if (null != nodeModel) {
                    return nodeModel;
                }
            }
        }
        return null;
    }

    private NodeModel getFromConditionNodesById(String nodeName) {
        for (ConditionNode conditionNode : getConditionNodes()) {
            NodeModel conditionChildNode = conditionNode.getChildNode();
            if (null != conditionChildNode) {
                NodeModel nodeModel = conditionChildNode.getNodeById(nodeName);
                if (null != nodeModel) {
                    return nodeModel;
                }
            }
        }
        return null;
    }


    /**
     * 判断是否为条件节点
     */
    public boolean isConditionNode() {
        return 3 == type || 4 == type;
    }

    public NodeModel getNodeById(String nodeName) {
        if (Objects.equals(this.getNodeId(), nodeName)) {
            return this;
        }
        if (null != this.getConditionNodes()) {
            NodeModel fromConditionNode = getFromConditionNodesById(nodeName);
            if (fromConditionNode != null) {
                return fromConditionNode;
            }
        }
        // 条件节点中没有找到 那么去它的同级子节点中继续查找
        if (null != this.getChildNode()) {
            return this.getChildNode().getNodeById(nodeName);
        }
        return null;
    }
}