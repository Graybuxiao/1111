package com.dingding.mid.listener;

import static com.dingding.mid.common.CommonConstants.BUSINESS_STATUS_4;
import static com.dingding.mid.common.CommonConstants.PROCESS_STATUS;

import javax.annotation.Resource;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

/**
 * @author LoveMyOrange
 * @create 2022-10-15 19:47
 */
@Component
public class ProcessListener implements ExecutionListener {
    @Resource
    private RepositoryService repositoryService;
    
    @Resource
    private RuntimeService runtimeService;

    @Override
    public void notify(DelegateExecution execution) {
        execution.setVariable(PROCESS_STATUS,BUSINESS_STATUS_4);
        runtimeService.updateBusinessStatus(execution.getProcessInstanceId(), BUSINESS_STATUS_4);
    }
}
