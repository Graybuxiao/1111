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
package com.dingding.mid.flowlong.handler;


import com.dingding.mid.flowlong.core.Execution;
import com.dingding.mid.flowlong.core.FlowLongContext;

/**
 * 决策处理器接口
 *
 * <p>
 * 尊重知识产权，CV 请保留版权，爱组搭 http://aizuda.com 出品，不允许非法使用，后果自负
 * </p>
 *
 * @author hubin
 * @since 1.0
 */
public interface DecisionHandler {

    /**
     * 定义决策方法，实现类需要根据执行对象做处理，并返回后置流转的name
     *
     * @param flowLongContext 流程引擎上下文
     * @param execution       {@see Execution}
     * @return String 后置流转的节点名称
     */
    String decide(FlowLongContext flowLongContext, Execution execution);
}
