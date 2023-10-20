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
package com.dingding.mid.flowlong.core;

import com.dingding.mid.flowlong.FlowLongEngine;
import com.dingding.mid.flowlong.entity.FlwInstance;
import com.dingding.mid.flowlong.entity.FlwProcess;
import com.dingding.mid.flowlong.entity.FlwTask;
import com.dingding.mid.flowlong.entity.FlwTaskActor;
import com.dingding.mid.flowlong.exception.FlowLongException;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程执行过程中所传递的执行对象，其中包含流程定义、流程模型、流程实例对象、执行参数、返回的任务列表
 *
 * <p>
 * 尊重知识产权，CV 请保留版权，爱组搭 http://aizuda.com 出品，不允许非法使用，后果自负
 * </p>
 *
 * @author hubin
 * @since 1.0
 */
@Getter
@Setter
public class Execution implements Serializable {
    /**
     * FlowLongEngine holder
     */
    private FlowLongEngine engine;
    /**
     * 流程定义对象
     */
    private FlwProcess process;
    /**
     * 流程实例对象
     */
    private FlwInstance flwInstance;
    /**
     * 父流程实例
     */
    private FlwInstance parentFlwInstance;
    /**
     * 下一个审批参与者
     */
    private FlwTaskActor nextFlwTaskActor;
    /**
     * 父流程实例节点名称
     */
    private String parentNodeName;
    /**
     * 子流程实例节点名称
     */
    private Long childInstanceId;
    /**
     * 执行参数
     */
    private Map<String, Object> args;
    /**
     * 创建人ID
     */
    private String createId;
    /**
     * 创建人
     */
    private String createBy;
    /**
     * 当前执行任务
     */
    private FlwTask flwTask;
    /**
     * 返回的任务列表
     */
    private List<FlwTask> flwTasks = new ArrayList<>();
    /**
     * 是否已合并
     * 针对join节点的处理
     */
    private boolean isMerged = false;

    /**
     * 用于产生子流程执行对象使用
     *
     * @param execution
     * @param process
     * @param parentNodeName
     */
    Execution(Execution execution, FlwProcess process, String parentNodeName) {
        if (execution == null || process == null || parentNodeName == null) {
            throw new FlowLongException("构造Execution对象失败，请检查execution、process、parentNodeName是否为空");
        }
        this.engine = execution.getEngine();
        this.process = process;
        this.args = execution.getArgs();
        this.parentFlwInstance = execution.getFlwInstance();
        this.parentNodeName = parentNodeName;
        this.createId = execution.getCreateId();
        this.createBy = execution.getCreateBy();
    }

    /**
     * 构造函数，接收流程定义、流程实例对象、执行参数
     *
     * @param process
     * @param flwInstance
     * @param args
     */
    public Execution(FlowLongEngine engine, FlwProcess process, FlwInstance flwInstance, Map<String, Object> args) {
        if (process == null || flwInstance == null) {
            throw new FlowLongException("构造Execution对象失败，请检查process、order是否为空");
        }
        this.engine = engine;
        this.process = process;
        this.flwInstance = flwInstance;
        this.args = args;
    }

    /**
     * 根据当前执行对象execution、子流程定义process、当前节点名称产生子流程的执行对象
     *
     * @param execution
     * @param process
     * @param parentNodeName
     * @return
     */
    public Execution createSubExecution(Execution execution, FlwProcess process, String parentNodeName) {
        return new Execution(execution, process, parentNodeName);
    }

    /**
     * 添加任务集合
     *
     * @param flwTasks
     */
    public void addTasks(List<FlwTask> flwTasks) {
        this.flwTasks.addAll(flwTasks);
    }

    /**
     * 添加任务
     *
     * @param flwTask
     */
    public void addTask(FlwTask flwTask) {
        this.flwTasks.add(flwTask);
    }

    /**
     * 执行参数
     *
     * @return {@link Map<String, Object>}
     */
    public Map<String, Object> getArgs() {
        if (null == this.args) {
            this.args = new HashMap<>();
        }
        return this.args;
    }
}
