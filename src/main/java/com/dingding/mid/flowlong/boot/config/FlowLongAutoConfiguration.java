/* Copyright 2023-2025 jobob@qq.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dingding.mid.flowlong.boot.config;

import com.dingding.mid.flowlong.*;
import com.dingding.mid.flowlong.boot.adapter.FlowJacksonHandler;
import com.dingding.mid.flowlong.boot.adapter.SpelExpression;
import com.dingding.mid.flowlong.boot.adapter.SpringBootScheduler;
import com.dingding.mid.flowlong.core.FlowLongContext;
import com.dingding.mid.flowlong.impl.GeneralAccessStrategy;
import com.dingding.mid.flowlong.listener.InstanceListener;
import com.dingding.mid.flowlong.mapper.*;
import com.dingding.mid.flowlong.scheduling.JobLock;
import com.dingding.mid.flowlong.scheduling.LocalLock;
import com.dingding.mid.flowlong.scheduling.TaskReminder;
import com.dingding.mid.flowlong.service.ProcessServiceImpl;
import com.dingding.mid.flowlong.service.QueryServiceImpl;
import com.dingding.mid.flowlong.service.RuntimeServiceImpl;
import com.dingding.mid.flowlong.service.TaskServiceImpl;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * spring boot starter 启动自动配置处理类
 *
 * <p>
 * 尊重知识产权，CV 请保留版权，爱组搭 http://aizuda.com 出品，不允许非法使用，后果自负
 * </p>
 *
 * @author hubin
 * @since 1.0
 */
@Configuration
@MapperScan("com.flowlong.bpm.mybatisplus.mapper")
@EnableConfigurationProperties(FlowLongProperties.class)

@SuppressWarnings("all")
public class FlowLongAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TaskService taskService(@Autowired(required = false) TaskAccessStrategy taskAccessStrategy, @Autowired(required = false) TaskListener taskListener,
                                   FlwProcessMapper processMapper, FlwInstanceMapper instanceMapper, FlwTaskMapper taskMapper,
                                   FlwTaskCcMapper taskCcMapper, FlwTaskActorMapper taskActorMapper, FlwHisTaskMapper hisTaskMapper,
                                   FlwHisTaskActorMapper hisTaskActorMapper) {
        return new TaskServiceImpl(taskAccessStrategy, taskListener, processMapper, instanceMapper, taskMapper,
                taskCcMapper, taskActorMapper, hisTaskMapper, hisTaskActorMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public QueryService queryService(FlwInstanceMapper instanceMapper, FlwHisInstanceMapper hisInstanceMapper,
                                     FlwTaskMapper taskMapper, FlwTaskActorMapper taskActorMapper,
                                     FlwHisTaskMapper hisTaskMapper, FlwHisTaskActorMapper hisTaskActorMapper) {
        return new QueryServiceImpl(instanceMapper, hisInstanceMapper, taskMapper, taskActorMapper, hisTaskMapper, hisTaskActorMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RuntimeService runtimeService(@Autowired(required = false) InstanceListener instanceListener,
                                         QueryService queryService, TaskService taskService, FlwInstanceMapper instanceMapper,
                                         FlwHisInstanceMapper hisInstanceMapper) {
        return new RuntimeServiceImpl(instanceListener, queryService, taskService, instanceMapper, hisInstanceMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessService processService(RuntimeService runtimeService, FlwProcessMapper processMapper) {
        return new ProcessServiceImpl(runtimeService, processMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public JobLock jobLock() {
        return new LocalLock();
    }

    @Bean
    @ConditionalOnBean({FlowLongContext.class, TaskReminder.class})
    @ConditionalOnMissingBean
    public SpringBootScheduler springBootScheduler(FlowLongContext flowLongContext, FlowLongProperties properties,
                                                   TaskReminder taskReminder, JobLock jobLock) {
        SpringBootScheduler scheduler = new SpringBootScheduler();
        scheduler.setContext(flowLongContext);
        scheduler.setRemindParam(properties.getRemind());
        scheduler.setTaskReminder(taskReminder);
        scheduler.setJobLock(jobLock);
        return scheduler;
    }

    @Bean
    @ConditionalOnMissingBean
    public Expression expression() {
        return new SpelExpression();
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskAccessStrategy taskAccessStrategy() {
        return new GeneralAccessStrategy();
    }

    @Bean
    @ConditionalOnMissingBean
    public FlowLongContext flowLongContext(ProcessService processService, QueryService queryService, RuntimeService runtimeService,
                                           TaskService taskService, Expression expression, TaskAccessStrategy taskAccessStrategy) {
        // 静态注入 Jackson 解析 JSON 处理器
        FlowLongContext.setFlowJsonHandler(new FlowJacksonHandler());
        // 注入 FlowLong 上下文
        FlowLongContext flc = new FlowLongContext();
        flc.setProcessService(processService);
        flc.setQueryService(queryService);
        flc.setRuntimeService(runtimeService);
        flc.setTaskService(taskService);
        flc.setExpression(expression);
        flc.setTaskAccessStrategy(taskAccessStrategy);
        return flc;
    }

    @Bean
    @ConditionalOnMissingBean
    public FlowLongEngine flowLongEngine(FlowLongContext flowLongContext) {
        return flowLongContext.build();
    }

}
