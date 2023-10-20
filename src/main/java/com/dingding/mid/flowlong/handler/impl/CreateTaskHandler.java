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

import com.dingding.mid.flowlong.FlowLongInterceptor;
import com.dingding.mid.flowlong.core.Execution;
import com.dingding.mid.flowlong.core.FlowLongContext;
import com.dingding.mid.flowlong.entity.FlwTask;
import com.dingding.mid.flowlong.exception.FlowLongException;
import com.dingding.mid.flowlong.handler.FlowLongHandler;
import com.dingding.mid.flowlong.model.NodeModel;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 任务创建操作的处理器
 *
 * <p>
 * 尊重知识产权，CV 请保留版权，爱组搭 http://aizuda.com 出品，不允许非法使用，后果自负
 * </p>
 *
 * @author hubin
 * @since 1.0
 */
@Slf4j
public class CreateTaskHandler implements FlowLongHandler {
    /**
     * 任务模型
     */
    private NodeModel nodeModel;

    /**
     * 调用者需要提供任务模型
     *
     * @param nodeModel 节点模型
     */
    public CreateTaskHandler(NodeModel nodeModel) {
        this.nodeModel = nodeModel;
    }

    /**
     * 根据任务模型、执行对象，创建下一个任务，并添加到execution对象的tasks集合中
     */
    @Override
    public void handle(FlowLongContext flowLongContext, Execution execution) {
        List<FlwTask> flwTasks = execution.getEngine().taskService().createTask(nodeModel, execution);
        execution.addTasks(flwTasks);
        /**
         * 从服务上下文中查找任务拦截器列表，依次对task集合进行拦截处理
         */
        try {
            List<FlowLongInterceptor> interceptors = flowLongContext.getInterceptors();
            if (null != interceptors) {
                interceptors.forEach(i -> i.handle(flowLongContext, execution));
            }
        } catch (Exception e) {
            log.error("拦截器执行失败={}", e.getMessage());
            throw new FlowLongException(e);
        }
    }
}
