package com.dingding.mid.enums;

import com.google.common.collect.Maps;

import java.util.Map;

public enum TypesEnums {
    /**
     * 并行事件
     */
    CONCURRENTS("CONCURRENTS"),
    CONCURRENT("CONCURRENT"/*, SequenceFlow.class*/),
    /**
     * 排他事件
     */
    CONDITION("CONDITION"/*, ExclusiveGateway.class*/),
    CONDITIONS("CONDITIONS"/*, ExclusiveGateway.class*/),
    /**
     * 任务
     */
    USER_TASK("APPROVAL"/*, UserTask.class*/),
    EMPTY("EMPTY"/*, Object.class*/),
    ROOT("ROOT"/*, UserTask.class*/),
    CC("CC"/*, ServiceTask.class*/),
    TRIGGER("TRIGGER"/*, ServiceTask.class*/),
    DELAY("DELAY"/*, IntermediateCatchEvent.class*/);
    private String type;


    TypesEnums(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
