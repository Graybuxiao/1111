package com.dingding.mid.listener;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class Vue3Listener implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        System.err.println("vue3监听器触发了");
    }
}
