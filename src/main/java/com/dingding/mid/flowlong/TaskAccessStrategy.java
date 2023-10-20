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


import com.dingding.mid.flowlong.entity.FlwTaskActor;

import java.util.List;

/**
 * 任务访问策略类
 * <p>
 * 用于判断给定的创建人员是否允许执行某个任务
 * </p>
 *
 * <p>
 * 尊重知识产权，CV 请保留版权，爱组搭 http://aizuda.com 出品，不允许非法使用，后果自负
 * </p>
 *
 * @author hubin
 * @since 1.0
 */
public interface TaskAccessStrategy {

    /**
     * 根据创建人ID、参与者集合判断是否允许访问所属任务
     *
     * @param userId     用户ID
     * @param taskActors 参与者列表 传递至该接口的实现类中的参与者都是为非空
     * @return boolean 是否允许访问
     */
    boolean isAllowed(String userId, List<FlwTaskActor> taskActors);
}
