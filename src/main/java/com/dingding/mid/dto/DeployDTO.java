package com.dingding.mid.dto;

import com.dingding.mid.entity.ProcessTemplates;
import lombok.Data;

/**
 * @author Doctor4JavaEE
 * @since 2023/10/25
 */
@Data
public class DeployDTO {
    private String processJson;
    private String formJson;
    private ProcessTemplates processTemplates;
}
