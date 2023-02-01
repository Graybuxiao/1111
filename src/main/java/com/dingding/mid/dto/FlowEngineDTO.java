package com.dingding.mid.dto;

import lombok.Data;

/**
 * @Author:LoveMyOrange
 * @Description:
 * @Date:Created in 2022/10/9 15:51
 */
@Data
public class FlowEngineDTO {
  /**
   * 表单ID
   */
  private String formId;
  /**
   * 表单内容列表（页面）
   */
  private String formItems;
  /**
   * 表单名称
   */
  private String formName;
  /**
   * 表单所属分组ID
   */
  private Integer groupId;
  /**
   * logo对象字符串: {'icon':'','background':''}
   */
  private String logo;
  /**
   * 审批流程json内容
   */
  private String process;
  /**
   * 备注
   */
  private String remark;
  /**
   * 设置：消息推送...
   */
  private String settings;
}
