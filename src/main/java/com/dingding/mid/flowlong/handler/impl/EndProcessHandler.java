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
package com.dingding.mid.flowlong.handler.impl;


import com.dingding.mid.flowlong.FlowLongEngine;
import com.dingding.mid.flowlong.assist.Assert;
import com.dingding.mid.flowlong.core.Execution;
import com.dingding.mid.flowlong.core.FlowCreator;
import com.dingding.mid.flowlong.core.FlowLongContext;
import com.dingding.mid.flowlong.entity.FlwInstance;
import com.dingding.mid.flowlong.entity.FlwTask;
import com.dingding.mid.flowlong.handler.FlowLongHandler;

import java.util.List;
import java.util.Map;

import static com.dingding.mid.common.CommonConstants.BUSINESS_STATUS_4;
import static com.dingding.mid.common.CommonConstants.PROCESS_STATUS;

/**
 * 结束流程实例的处理器
 *
 * <p>
 * 尊重知识产权，CV 请保留版权，爱组搭 http://aizuda.com 出品，不允许非法使用，后果自负
 * </p>
 *
 * @author hubin
 * @since 1.0
 */
public class EndProcessHandler implements FlowLongHandler {

    /**
     * 结束当前流程实例，如果存在父流程，则触发父流程继续执行
     */
    @Override
    public void handle(FlowLongContext flowLongContext, Execution execution) {
        FlowLongEngine engine = execution.getEngine();
        FlwInstance flwInstance = execution.getFlwInstance();
        List<FlwTask> flwTasks = engine.queryService().getTasksByInstanceId(flwInstance.getId());
        for (FlwTask flwTask : flwTasks) {
            Assert.illegalArgument(flwTask.major(), "存在未完成的主办任务");
            engine.taskService().complete(flwTask.getId(), FlowCreator.ADMIN);
        }
        /**
         * 结束当前流程实例
         */

        Map<String, Object> variableMap = flwInstance.getVariableMap();
        variableMap.put(PROCESS_STATUS,BUSINESS_STATUS_4);
        engine.getContext().getRuntimeService().addVariable(flwInstance.getId(),variableMap);
        engine.runtimeService().complete(flwInstance.getId());
    }
}
