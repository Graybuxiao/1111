package com.dingding.mid.listener;

import com.dingding.mid.utils.SpringContextHolder;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.dingding.mid.common.WorkFlowConstants.*;
import static com.dingding.mid.utils.BpmnModelUtils.getChildNode;

/**
 * @author LoveMyOrange
 * @create 2022-10-15 14:51
 */
@Component
public class TaskCreatedListener implements TaskListener {
    @Resource
    private TaskService taskService;
    @Override
    public void notify(DelegateTask delegateTask) {
            if(DEFAULT_NULL_ASSIGNEE.equals(delegateTask.getAssignee())){
                Object autoRefuse = delegateTask.getVariable(AUTO_REFUSE_STR);
                if(autoRefuse==null){
                    taskService.addComment(delegateTask.getId(),delegateTask.getProcessInstanceId(),"opinion"+COMMENT_SPLIT+"审批人为空,自动通过");
                    taskService.complete(delegateTask.getId());
                }
                else{
                    taskService.addComment(delegateTask.getId(),delegateTask.getProcessInstanceId(),"opinion"+COMMENT_SPLIT+"审批人为空,自动驳回");
                    RuntimeService runtimeService = SpringContextHolder.getBean(RuntimeService.class);
                    runtimeService.deleteProcessInstance(delegateTask.getProcessInstanceId(),"审批人为空,自动驳回");
                }
            }
    }
}
