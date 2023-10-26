package com.dingding.mid.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dingding.mid.entity.ProcessAttachments;
import com.dingding.mid.entity.ProcessComments;
import com.dingding.mid.mapper.ProcessAttachmentsMapper;
import com.dingding.mid.mapper.ProcessCommentsMapper;
import com.dingding.mid.service.ProcessAttachmentsService;
import com.dingding.mid.service.ProcessCommentsService;
import org.springframework.stereotype.Service;

/**
 * @author : willian fu
 * @version : 1.0
 */
@Service
public class ProcessAttachmentServiceImpl extends ServiceImpl<ProcessAttachmentsMapper, ProcessAttachments>  implements ProcessAttachmentsService {

}
