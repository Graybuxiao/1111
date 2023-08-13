package com.dingding.mid.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dingding.mid.dto.json.ChildNode;
import com.dingding.mid.dto.json.Properties;
import com.dingding.mid.dto.json.UserInfo;
import com.dingding.mid.entity.NodeJsonData;
import com.dingding.mid.enums.AssigneeTypeEnums;
import com.dingding.mid.exception.WorkFlowException;
import com.dingding.mid.service.NodeJsonDataService;
import com.dingding.mid.utils.SpringContextHolder;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.runtime.Execution;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

import static com.dingding.mid.common.CommonConstants.START_USER_INFO;
import static com.dingding.mid.common.WorkFlowConstants.*;
import static com.dingding.mid.utils.BpmnModelUtils.getChildNode;

@Component
@Slf4j
public class CamundaFlowUtils {

    @Resource
    private RuntimeService runtimeService;
    @Resource
    private HistoryService historyService;
    public List<String> calculateTaskCandidateUsers(DelegateExecution execution) {
        if(StringUtils.isBlank(execution.getCurrentActivityId())){
            Map<String, Object> variables = execution.getVariables();
            Set<String> strings = variables.keySet();
            String variableName="";
            for (String string : strings) {
                if(string.endsWith("AssigneeList")){
                    variableName=string;
                }
            }
            List list = MapUtil.get(variables, variableName, List.class);
            return list;
        }
        List<String> assigneeList = new ArrayList<>();
        NodeJsonDataService nodeJsonDataService = SpringContextHolder.getBean(NodeJsonDataService.class);
        LambdaQueryWrapper<NodeJsonData> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(NodeJsonData::getProcessDefinitionId,execution.getProcessDefinitionId());
        NodeJsonData nodeJsonData = nodeJsonDataService.getOne(lambdaQueryWrapper);
        String currentActivityId=execution.getCurrentActivityId();
        if(StringUtils.endsWith(execution.getCurrentActivityId(),MULTI_BODY)){
            currentActivityId=currentActivityId.replace(MULTI_BODY,"");
        }

        ChildNode childNode = JSONObject.parseObject(nodeJsonData.getNodeJsonData(), new TypeReference<ChildNode>(){});
        ChildNode currentNode = getChildNode(childNode,currentActivityId);
        if(currentNode==null){
            throw new WorkFlowException("查找审批人失败,请联系管理员重试");
        }
        Properties props = currentNode.getProps();
        String assignedType = props.getAssignedType();
        Map<String, Object> nobody = props.getNobody();
        String variable=currentActivityId+"AssigneeList";
        if(AssigneeTypeEnums.ASSIGN_USER.getTypeName().equals(assignedType)){
            List<UserInfo> assignedUser = props.getAssignedUser();
            for (UserInfo userInfo : assignedUser) {
                assigneeList.add(userInfo.getId());
            }
        }
        else if(AssigneeTypeEnums.SELF_SELECT.getTypeName().equals(assignedType)){
            List<String> assigneeUsers = (List<String>) execution.getVariable(currentActivityId);
            if(assigneeUsers!=null){
                assigneeList.addAll(assigneeUsers);
            }
        }
        else if(AssigneeTypeEnums.LEADER_TOP.getTypeName().equals(assignedType)){
            //来自于users表的admin列
            throw new WorkFlowException("此项目没有RBAC功能,所以没法做这个功能,可以看一下我写的Ruoyi-Vue-Camunda的那个版本,里面有复杂的找人代码实现");
        }
        else if(AssigneeTypeEnums.LEADER.getTypeName().equals(assignedType)){
            //向上找就行了
            throw new WorkFlowException("此项目没有RBAC功能,所以没法做这个功能,可以看一下我写的Ruoyi-Vue-Camunda的那个版本,里面有复杂的找人代码实现");
        }
        else if(AssigneeTypeEnums.ROLE.getTypeName().equals(assignedType)){
            //向上找就行了
            throw new WorkFlowException("此项目没有RBAC功能,所以没法做这个功能,可以看一下我写的Ruoyi-Vue-Camunda的那个版本,里面有复杂的找人代码实现");
        }
        else if(AssigneeTypeEnums.SELF.getTypeName().equals(assignedType)){
            String startUserJson = (String) execution.getVariable(START_USER_INFO);
            UserInfo userInfo = JSONObject.parseObject(startUserJson, new TypeReference<UserInfo>() {
            });
            assigneeList.add(userInfo.getId());
        }
        else if(AssigneeTypeEnums.FORM_USER.getTypeName().equals(assignedType)){
            String formUser = props.getFormUser();
            List<JSONObject> assigneeUsers = (List<JSONObject>) execution.getVariable(formUser);
            if(assigneeUsers!=null){
                for (JSONObject assigneeUser : assigneeUsers) {
                    assigneeList.add(assigneeUser.getString("id"));
                }
            }

        }

        if(CollUtil.isEmpty(assigneeList)){
            String handler = MapUtil.getStr(nobody, "handler");
            if("TO_PASS".equals(handler)){
                assigneeList.add(DEFAULT_NULL_ASSIGNEE);
                execution.setVariable(variable,assigneeList);
            }
            else if("TO_REFUSE".equals(handler)){
                execution.setVariable("autoRefuse",Boolean.TRUE);
                assigneeList.add(DEFAULT_NULL_ASSIGNEE);
                execution.setVariable(variable,assigneeList);
            }
            else if("TO_ADMIN".equals(handler)){
                assigneeList.add(DEFAULT_ADMIN_ASSIGNEE);
                execution.setVariable(variable,assigneeList);
            }
            else if("TO_USER".equals(handler)){
                Object assignedUserObj = nobody.get("assignedUser");
                if(assignedUserObj!=null ){
                    List<JSONObject> assignedUser =(List<JSONObject>)assignedUserObj;
                    if(assignedUser.size()>0){
                        for (JSONObject object : assignedUser) {
                            assigneeList.add(object.getString("id"));
                        }
                        execution.setVariable(variable,assigneeList);
                    }
                    else{
                        assigneeList.add(DEFAULT_NULL_ASSIGNEE);
                        execution.setVariable(variable,assigneeList);
                    }

                }

            }
            else{
                throw new WorkFlowException("找不到审批人,请检查配置!!!");
            }
        }
        else{
            execution.setVariable(variable,assigneeList);
        }


        execution.setVariableLocal(currentActivityId+"AssigneeList",assigneeList);
        return assigneeList;
    }



}
