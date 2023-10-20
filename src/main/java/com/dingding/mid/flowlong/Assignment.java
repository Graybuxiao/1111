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
package com.dingding.mid.flowlong;


import com.dingding.mid.flowlong.core.Execution;
import com.dingding.mid.flowlong.model.NodeModel;

/**
 * 分配参与者的处理接口
 *
 * <p>
 * 尊重知识产权，CV 请保留版权，爱组搭 http://aizuda.com 出品，不允许非法使用，后果自负
 * </p>
 *
 * @author hubin
 * @since 1.0
 */
public interface Assignment {

    /**
     * 分配参与者方法，可获取到当前的执行对象
     *
     * @param execution 执行对象
     * @return Object 任务参与者
     */
    default Object assign(Execution execution) {
        return assign(null, execution);
    }

    /**
     * 分配参与者方法，可获取到当前的任务模型、执行对象
     *
     * @param nodeModel 节点模型
     * @param execution 执行对象
     * @return Object 任务参与者
     */
    Object assign(NodeModel nodeModel, Execution execution);

}
