package com.dingding.mid.listener;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

/**
 * Trigger监听器
 *
 * @author Liuxd
 * @create 2023-03-30
 */
@Component
@Slf4j
public class TriggerListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        log.info("触发器被触发: {}", delegateTask.getEventName());
    }
}
