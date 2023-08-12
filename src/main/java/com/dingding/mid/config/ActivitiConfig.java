/*
 *    Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the pig4cloud.com developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 */

package com.dingding.mid.config;

import cn.hutool.extra.spring.SpringUtil;
import com.dingding.mid.job.IdWorkerIdGenerator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.sql.DataSource;

import com.dingding.mid.job.CustomJobHandler;
import com.dingding.mid.utils.SpringContextHolder;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.jobexecutor.JobHandler;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author
 * @date
 * @desc
 */
@Configuration
public class ActivitiConfig {
	@Component
	public class CamundaGlobalListenerPlugin extends AbstractProcessEnginePlugin {
		@Override
		public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
			IdWorkerIdGenerator idWorkerIdGenerator = SpringContextHolder.getBean(IdWorkerIdGenerator.class);
			processEngineConfiguration.setIdGenerator(idWorkerIdGenerator);
		}
	}
}
