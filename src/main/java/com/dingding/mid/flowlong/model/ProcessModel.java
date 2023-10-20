/*
 * 爱组搭 http://aizuda.com 低代码组件化开发平台
 * ------------------------------------------
 * 受知识产权保护，请勿删除版权申明
 */
package com.dingding.mid.flowlong.model;


import com.dingding.mid.flowlong.assist.Assert;
import com.dingding.mid.flowlong.cache.FlowCache;
import com.dingding.mid.flowlong.core.FlowLongContext;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 爱组搭 http://aizuda.com
 * ----------------------------------------
 * JSON BPM 模型
 *
 * @author 青苗
 * @since 2023-03-17
 */
@Getter
@Setter
public class ProcessModel {
    /**
     * 节点名称
     */
    private String name;
    /**
     * 实例地址
     */
    private String instanceUrl;
    /**
     * 节点信息
     */
    private NodeModel nodeConfig;

    /**
     * 获取process定义的指定节点名称的节点模型
     *
     * @param nodeName 节点名称
     * @return {@link NodeModel}
     */
    public NodeModel getNode(String nodeName) {
        return null == nodeConfig ? null : nodeConfig.getNode(nodeName);
    }

    /**
     * 流程文件字节码解析为流程模型
     *
     * @param content   流程定义内容
     * @param processId 流程定义ID
     */
    public static ProcessModel parse(String content, Long processId) {
        // 缓存解析逻辑
        if (null != processId) {
            final String cacheKey = "flwProcessModel#" + processId;
            FlowCache flowCache = FlowLongContext.FLOW_CACHE;
            ProcessModel processModel = flowCache.get(cacheKey);
            if (null == processModel) {
                processModel = parseProcessModel(content);
                flowCache.put(cacheKey, processModel);
            }
            return processModel;
        }

        // 未缓存解析逻辑
        return parseProcessModel(content);
    }

    private static ProcessModel parseProcessModel(String content) {
        ProcessModel processModel = FlowLongContext.fromJson(content, ProcessModel.class);
        Assert.isNull(processModel, "process model json parser error");
        processModel.buildParentNode(processModel.getNodeConfig());
        return processModel;
    }

    /**
     * 构建父节点
     *
     * @param rootNode 根节点
     */
    protected void buildParentNode(NodeModel rootNode) {
        List<ConditionNode> conditionNodes = rootNode.getConditionNodes();
        if (null != conditionNodes) {
            for (ConditionNode conditionNode : conditionNodes) {
                NodeModel conditionChildNode = conditionNode.getChildNode();
                if (null != conditionChildNode) {
                    conditionChildNode.setParentNode(rootNode);
                    this.buildParentNode(conditionChildNode);
                }
            }
        }
        NodeModel childNode = rootNode.getChildNode();
        if (null != childNode) {
            childNode.setParentNode(rootNode);
            this.buildParentNode(childNode);
        }
    }
}
