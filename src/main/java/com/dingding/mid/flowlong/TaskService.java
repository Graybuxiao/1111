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
import com.dingding.mid.flowlong.core.FlowCreator;
import com.dingding.mid.flowlong.core.enums.PerformType;
import com.dingding.mid.flowlong.core.enums.TaskType;
import com.dingding.mid.flowlong.entity.FlwHisTaskActor;
import com.dingding.mid.flowlong.entity.FlwTask;
import com.dingding.mid.flowlong.entity.FlwTaskActor;
import com.dingding.mid.flowlong.model.NodeModel;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 任务业务类接口
 *
 * <p>
 * 尊重知识产权，CV 请保留版权，爱组搭 http://aizuda.com 出品，不允许非法使用，后果自负
 * </p>
 *
 * @author hubin
 * @since 1.0
 */
public interface TaskService {

    /**
     * 根据任务ID，创建人ID完成任务
     *
     * @param taskId      任务ID
     * @param flowCreator 任务完成者
     * @param args        任务参数
     * @return Task 任务对象
     */
    FlwTask complete(Long taskId, FlowCreator flowCreator, Map<String, Object> args);

    default FlwTask complete(Long taskId, FlowCreator flowCreator) {
        return this.complete(taskId, flowCreator, null);
    }

    /**
     * 更新任务对象
     *
     * @param flwTask 任务对象
     */
    void updateTaskById(FlwTask flwTask);

    /**
     * 查看任务设置为已阅状态
     *
     * @param taskId    任务ID
     * @param taskActor 任务参与者
     * @return
     */
    boolean viewTask(Long taskId, FlwTaskActor taskActor);

    /**
     * 任务设置超时
     *
     * @param taskId 任务ID
     */
    boolean taskTimeout(Long taskId);

    /**
     * 根据 任务ID 认领任务，删除其它任务参与者
     *
     * @param taskId          任务ID
     * @param flwHisTaskActor 任务参与者
     * @return Task 任务对象
     */
    FlwTask claim(Long taskId, FlwHisTaskActor flwHisTaskActor);

    /**
     * 根据 任务ID 转办任务
     *
     * @param taskId            任务ID
     * @param taskActor         任务参与者
     * @param assigneeTaskActor 任务办理人
     * @return
     */
    default boolean transferTask(Long taskId, FlwTaskActor taskActor, FlwTaskActor assigneeTaskActor) {
        return this.assigneeTask(taskId, TaskType.transfer, taskActor, assigneeTaskActor);
    }

    /**
     * 根据 任务ID 委派任务、代理人办理完任务该任务重新归还给原处理人
     *
     * @param taskId                  任务ID
     * @param flwHisTaskActor         任务参与者
     * @param assigneeFlwHisTaskActor 任务办理人
     * @return
     */
    default boolean delegateTask(Long taskId, FlwHisTaskActor flwHisTaskActor, FlwHisTaskActor assigneeFlwHisTaskActor) {
        return this.assigneeTask(taskId, TaskType.delegate, flwHisTaskActor, assigneeFlwHisTaskActor);
    }

    /**
     * 根据 任务ID 分配任务给指定办理人、重置任务类型
     *
     * @param taskId            任务ID
     * @param taskType          任务类型
     * @param taskActor         任务参与者
     * @param assigneeTaskActor 指定办理人
     * @return
     */
    boolean assigneeTask(Long taskId, TaskType taskType, FlwTaskActor taskActor, FlwTaskActor assigneeTaskActor);

    /**
     * 拿回任务、在当前办理人尚未处理文件前，允许上一节点提交人员执行拿回
     *
     * @param taskId      任务ID
     * @param flowCreator 任务创建者
     * @return
     */
    Optional<FlwTask> reclaimTask(Long taskId, FlowCreator flowCreator);

    /**
     * 唤醒历史任务
     * <p>
     * 该方法会导致流程状态不可控，请慎用
     * </p>
     *
     * @param taskId          历史任务ID
     * @param flwHisTaskActor 任务参与者
     * @return {@link FlwTask} 唤醒后的任务对象
     */
    FlwTask resume(Long taskId, FlwHisTaskActor flwHisTaskActor);

    /**
     * 根据任务ID、创建人撤回任务
     *
     * @param taskId      待撤回历史任务ID
     * @param flowCreator 任务创建者
     * @return Task 任务对象
     */
    Optional<FlwTask> withdrawTask(Long taskId, FlowCreator flowCreator);

    /**
     * 根据当前任务对象驳回至上一步处理
     *
     * @param currentFlwTask 当前任务对象
     * @param flowCreator    任务创建者
     * @param args           任务参数
     * @return Task 任务对象
     */
    Optional<FlwTask> rejectTask(FlwTask currentFlwTask, FlowCreator flowCreator, Map<String, Object> args);

    default Optional<FlwTask> rejectTask(FlwTask currentFlwTask, FlowCreator flowCreator) {
        return rejectTask(currentFlwTask, flowCreator, null);
    }

    /**
     * 根据 taskId、createBy 判断创建人createBy是否允许执行任务
     *
     * @param flwTask 任务对象
     * @param userId  用户ID
     * @return boolean 是否允许操作
     */
    boolean isAllowed(FlwTask flwTask, String userId);

    /**
     * 根据任务模型、执行对象创建新的任务
     *
     * @param taskModel 任务模型
     * @param execution 执行对象
     * @return List<Task> 创建任务集合
     */
    List<FlwTask> createTask(NodeModel taskModel, Execution execution);

    /**
     * 根据已有任务ID、任务类型、参与者创建新的任务
     *
     * @param taskId     主办任务ID
     * @param taskType   任务类型 {@link TaskType}
     * @param taskActors 参与者集合
     * @return List<Task> 创建任务集合
     */
    List<FlwTask> createNewTask(Long taskId, TaskType taskType, List<FlwTaskActor> taskActors);

    default List<FlwTask> createNewTask(Long taskId, TaskType taskType, FlwTaskActor taskActor) {
        return this.createNewTask(taskId, taskType, Arrays.asList(taskActor));
    }

    /**
     * 获取超时或者提醒的任务
     *
     * @return List<Task> 任务列表
     */
    List<FlwTask> getTimeoutOrRemindTasks();

    /**
     * 根据任务ID获取任务模型
     *
     * @param taskId 任务ID
     * @return
     */
    NodeModel getTaskModel(Long taskId);

    /**
     * 向指定的任务ID添加参与者【加签】
     *
     * @param taskId           任务ID
     * @param performType      参与类型 {@link PerformType}
     * @param flwHisTaskActors 参与者列表
     */
    boolean addTaskActor(Long taskId, PerformType performType, List<FlwHisTaskActor> flwHisTaskActors);

    default boolean addTaskActor(Long taskId, PerformType performType, FlwHisTaskActor flwHisTaskActor) {
        return this.addTaskActor(taskId, performType, Arrays.asList(flwHisTaskActor));
    }

    /**
     * 对指定的任务ID删除参与者【减签】
     *
     * @param taskId   任务ID
     * @param actorIds 参与者ID列表
     */
    boolean removeTaskActor(Long taskId, List<String> actorIds);

    default boolean removeTaskActor(Long taskId, String actorId) {
        return removeTaskActor(taskId, Arrays.asList(actorId));
    }

    /**
     * 级联删除 flw_his_task, flw_his_task_actor, flw_task, flw_task_actor
     *
     * @param instanceId 流程实例ID
     */
    void cascadeRemoveByInstanceId(Long instanceId);
}
