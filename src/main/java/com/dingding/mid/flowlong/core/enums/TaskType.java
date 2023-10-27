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
package com.dingding.mid.flowlong.core.enums;

import java.util.Arrays;

/**
 * 任务类型
 *
 * <p>
 * 尊重知识产权，CV 请保留版权，爱组搭 http://aizuda.com 出品，不允许非法使用，后果自负
 * </p>
 *
 * @author hubin
 * @since 1.0
 */
public enum TaskType {
    /**
     * 主办
     */
    major(0),
    /**
     * 转办、代理人办理完任务直接进入下一个节点
     */
    transfer(1),
    /**
     * 委派、代理人办理完任务该任务重新归还给原处理人
     */
    delegate(2),
    /**
     * 会签
     */
    countersign(3),
    trigger(5);

    private final int value;

    TaskType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static TaskType get(int value) {
        return Arrays.stream(TaskType.values()).filter(s -> s.getValue() == value).findFirst().orElseGet(null);
    }
}
