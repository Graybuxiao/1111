package com.dingding.mid.job;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.jobexecutor.JobHandler;
import org.camunda.bpm.engine.impl.jobexecutor.JobHandlerConfiguration;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.JobEntity;

/**
 * @author LoveMyOrange
 * @create 2022-10-15 21:42
 */
@Slf4j
public class CustomJobHandler  implements JobHandler {
    public static final String TYPE = "custom-handler";
    @Override
    public String getType() {
        return "custom-handler";
    }

    @Override
    public void execute(JobHandlerConfiguration configuration, ExecutionEntity execution, CommandContext commandContext, String tenantId) {

    }

    @Override
    public JobHandlerConfiguration newConfiguration(String canonicalString) {
        return null;
    }

    @Override
    public void onDelete(JobHandlerConfiguration configuration, JobEntity jobEntity) {

    }

}
