package com.dingding.mid.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author Doctor4JavaEE
 * @since 2023/10/25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "process_comments")
public class ProcessComments {
    @TableId(type = IdType.ASSIGN_ID)
    /**
     *
     */
    private String id;


    private String types;
    private Date createTime;
    private String userId;
    private Long taskId;
    private Long instanceId;
    private String message;

}
