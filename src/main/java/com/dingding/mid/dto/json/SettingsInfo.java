package com.dingding.mid.dto.json;

import com.alibaba.fastjson.JSONObject;
import java.util.List;
import lombok.Data;

/**
 * @Author:LoveMyOrange
 * @Description:
 * @Date:Created in 2022/10/9 16:06
 */
@Data
public class SettingsInfo {
  /**
   * ？？？
   */
  private List<String> commiter;
  /**
   * 谁可以管理
   */
  private List<UserInfo> admin;
  /**
   * 同意时签字
   */
  private Boolean sign;
  /**
   * 推送配置
   */
  private JSONObject notify;
}
