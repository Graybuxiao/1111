package com.dingding.mid.flowlong.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BaseEx {
    private String nodeId;
    private NodeModel childNode;
    private List<ConditionNode> conditionNodes;
}
