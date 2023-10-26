package com.dingding.mid.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dingding.mid.common.Result;
import com.dingding.mid.dto.*;
import com.dingding.mid.dto.json.ChildNode;
import com.dingding.mid.dto.json.FormOperates;
import com.dingding.mid.dto.json.SettingsInfo;
import com.dingding.mid.dto.json.UserInfo;
import com.dingding.mid.entity.*;
import com.dingding.mid.enums.RefuseEnums;
import com.dingding.mid.exception.WorkFlowException;
import com.dingding.mid.flowlong.*;
import com.dingding.mid.flowlong.core.FlowCreator;
import com.dingding.mid.flowlong.core.enums.PerformType;
import com.dingding.mid.flowlong.entity.*;
import com.dingding.mid.flowlong.mapper.*;
import com.dingding.mid.flowlong.model.ProcessModel;
import com.dingding.mid.service.*;
import com.dingding.mid.utils.SpringContextHolder;
import com.dingding.mid.vo.*;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSort;
import com.sun.xml.internal.ws.api.message.Attachment;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.xml.stream.events.Comment;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.dingding.mid.common.CommonConstants.*;
import static com.dingding.mid.common.WorkFlowConstants.*;

/**
 * @author : willian fu
 * @version : 1.0
 */
@RestController
@RequestMapping("/workspace")
@Api(tags = {"Vue2版本 的数据列表,待办,已办,我发起等接口"})
@ApiSort(3)
public class WorkspaceProcessController {

    @Resource
    private ProcessTemplateService processTemplateService;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private TaskService taskService;
    @Resource
    private UserService userService;
    @Resource
    private CcService ccService;
    @Resource
    private ProcessService processService;
    @Resource
    private FlowLongEngine flowLongEngine;
    @Resource
    private FlwHisInstanceMapper flwHisInstanceMapper;
    @Resource
    private FlwTaskMapper flwTaskMapper;
    @Resource
    private FlwHisTaskMapper flwHisTaskMapper;
    @Resource
    private QueryService queryService;
    @Resource
    private FlwInstanceMapper flwInstanceMapper;
    @Resource
    private ProcessCommentsService processCommentsService;
    @Resource
    private ProcessAttachmentsService processAttachmentsService;
    @Resource
    private FlwTaskCcMapper flwTaskCcMapper;

    @ApiOperation("通过模板id查看流程信息 会附带流程定义id")
    @ApiOperationSupport(order = 1)
    @ApiParam(required = true,name = "模板id",value = "该值从form/groupps接口 里面去取")
    @GetMapping("process/detail")
    public Result<ProcessTemplates> detail(@RequestParam("templateId") String templateId){
        ProcessTemplates processTemplates = processTemplateService.getById(templateId);
        processTemplates.setLogo(processTemplates.getIcon());
        processTemplates.setFormId(processTemplates.getTemplateId());
        processTemplates.setFormName(processTemplates.getTemplateName());
        FlwProcess processById = processService.getProcessById(processTemplates.getFlowLongId());
        if(processById==null){
            throw  new WorkFlowException("该流程暂未接入FlowLong,请重试");
        }
        processTemplates.setProcessDefinitionId(processById.getId()+"");
        return Result.OK(processTemplates);
    }


    @ApiOperationSupport(order = 2)
    @ApiOperation("通过流程定义id启动流程")
    @PostMapping("process/start")
    public Result<Object> start(@RequestBody StartProcessInstanceDTO startProcessInstanceDTO){
        try{
            JSONObject formData = startProcessInstanceDTO.getFormData();
            UserInfo startUserInfo = startProcessInstanceDTO.getStartUserInfo();
            FlowCreator flowCreator = FlowCreator.of(startUserInfo.getId(), startUserInfo.getName());
            LambdaQueryWrapper<ProcessTemplates> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(ProcessTemplates::getFlowLongId,startProcessInstanceDTO.getProcessDefinitionId());
            lambdaQueryWrapper.select(ProcessTemplates::getTemplateName);
            ProcessTemplates processTemplates = processTemplateService.getOne(lambdaQueryWrapper);
            Map<String,Object> processVariables= new HashMap<>();
            processVariables.put(PROCESS_NAME,processTemplates.getTemplateName());
            processVariables.put(FORM_VAR,formData);
            processVariables.put(PROCESS_STATUS,BUSINESS_STATUS_1);
            processVariables.put(START_USER_INFO,JSONObject.toJSONString(startUserInfo));
            ArrayList<UserInfo> userInfos = CollUtil.newArrayList(startUserInfo);
            processVariables.put("root",JSONObject.toJSONString(userInfos));
            Map<String, List<UserInfo>> processUsers = startProcessInstanceDTO.getProcessUsers();
            if(CollUtil.isNotEmpty(processUsers)){
                Set<String> strings = processUsers.keySet();
                for (String string : strings) {
                    List<UserInfo> selectUserInfo = processUsers.get(string);
                    List<String> users=new ArrayList<>();
                    for (UserInfo userInfo : selectUserInfo) {
                        users.add(userInfo.getId());
                    }
                    processVariables.put(string,users);
                }
            }

            Map formValue = JSONObject.parseObject(formData.toJSONString(), new TypeReference<Map>() {
            });
            processVariables.putAll(formValue);

            Optional<FlwInstance> flwInstance = flowLongEngine.startInstanceById(Long.valueOf(startProcessInstanceDTO.getProcessDefinitionId()), flowCreator, processVariables);

            this.executeActiveTasks(flwInstance.get().getId(), flowCreator);

            //手动完成第一个任务
//            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
//            if(task!=null){
//                taskService.complete(task.getId());
//            }
            return Result.OK(flwInstance.get().getId());
        }
        catch (Exception e){
            Throwable cause = e.getCause();
            if(cause instanceof WorkFlowException){
                WorkFlowException workFlowException=(WorkFlowException)cause;
                return Result.error(workFlowException.getMessage());
            }
            e.printStackTrace();
            return Result.error("启动流程失败");
        }
    }

    public void executeActiveTasks(Long instanceId, FlowCreator testCreator) {
        this.executeActiveTasks(instanceId, t -> this.flowLongEngine.executeTask(t.getId(), testCreator));
    }

    public void executeActiveTasks(Long instanceId, Consumer<FlwTask> taskConsumer) {
        this.flowLongEngine.queryService().getActiveTasksByInstanceId(instanceId)
                .ifPresent(tasks -> tasks.forEach(t -> taskConsumer.accept(t)));
    }

    @ApiOperation("查看我发起的流程")
    @ApiOperationSupport(order = 3)
    @PostMapping("process/applyList")
    public Result< Page<HistoryProcessInstanceVO>> applyList(@RequestBody ApplyDTO applyDTO){

        LambdaQueryWrapper<FlwHisInstance> flwHisInstanceLambdaQueryWrapper = new LambdaQueryWrapper<>();
        flwHisInstanceLambdaQueryWrapper.eq(FlwHisInstance::getCreateId,applyDTO.getCurrentUserInfo().getId());
        flwHisInstanceLambdaQueryWrapper.orderByDesc(FlwHisInstance::getCreateTime);
        List<FlwHisInstance> flwHisInstances = flwHisInstanceMapper.selectList(flwHisInstanceLambdaQueryWrapper);
        Long count = flwHisInstanceMapper.selectCount(flwHisInstanceLambdaQueryWrapper);
        List<String> applyUserIds= new ArrayList<>();


        for (FlwHisInstance historicProcessInstance : flwHisInstances) {
            Map<String, Object> processVariables = historicProcessInstance.getVariableMap();
            String id = JSONObject.parseObject(MapUtil.getStr(processVariables, START_USER_INFO), new TypeReference<UserInfo>() {
            }).getId();
            applyUserIds.add(id);
        }
        Map<Long, Users> collect=new HashMap<>();
        if(CollUtil.isNotEmpty(applyUserIds)){
            LambdaQueryWrapper<Users> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.in(Users::getUserId,applyUserIds);
            List<Users> list = userService.list(lambdaQueryWrapper);
            collect = list.stream().collect(Collectors.toMap(Users::getUserId, Function.identity()));
        }

        List<HistoryProcessInstanceVO> historyProcessInstanceVOS= new ArrayList<>();
        Page<HistoryProcessInstanceVO> page=new Page<>();
        for (FlwHisInstance historicProcessInstance : flwHisInstances) {
            Map<String, Object> processVariables = historicProcessInstance.getVariableMap();
            HistoryProcessInstanceVO historyProcessInstanceVO=new HistoryProcessInstanceVO();
            historyProcessInstanceVO.setProcessInstanceId(historicProcessInstance.getId()+"");
            //todo column
            historyProcessInstanceVO.setProcessDefinitionName(MapUtil.getStr(processVariables,PROCESS_NAME));
            historyProcessInstanceVO.setStartUser(JSONObject.parseObject(MapUtil.getStr(processVariables,START_USER_INFO),new TypeReference<UserInfo>(){}));
            historyProcessInstanceVO.setUsers(collect.get(Long.valueOf(historyProcessInstanceVO.getStartUser().getId())));
            historyProcessInstanceVO.setStartTime(historicProcessInstance.getCreateTime());
            historyProcessInstanceVO.setEndTime(historicProcessInstance.getEndTime());
            Boolean flag= historicProcessInstance.getEndTime() != null;
            historyProcessInstanceVO.setCurrentActivityName(getCurrentName(historicProcessInstance.getId()+"",flag));
            historyProcessInstanceVO.setBusinessStatus(MapUtil.getStr(processVariables,PROCESS_STATUS));


            long totalTimes = historicProcessInstance.getEndTime()==null?
                    (Calendar.getInstance().getTimeInMillis()-historicProcessInstance.getCreateTime().getTime()):
                    (historicProcessInstance.getEndTime().getTime()-historicProcessInstance.getCreateTime().getTime());
            long dayCount = totalTimes /(1000*60*60*24);//计算天
            long restTimes = totalTimes %(1000*60*60*24);//剩下的时间用于计于小时
            long hourCount = restTimes/(1000*60*60);//小时
            restTimes = restTimes % (1000*60*60);
            long minuteCount = restTimes / (1000*60);

            String spendTimes = dayCount+"天"+hourCount+"小时"+minuteCount+"分";
            historyProcessInstanceVO.setDuration(spendTimes);
            historyProcessInstanceVOS.add(historyProcessInstanceVO);
        }
        page.setRecords(historyProcessInstanceVOS);
        page.setCurrent(applyDTO.getPageNo());
        page.setSize(applyDTO.getPageSize());
        page.setTotal(count);
        return Result.OK(page);
    }

    private   String getCurrentName(String processInstanceId,Boolean flag){
        if(flag){
            return "流程已结束";
        }
        LambdaQueryWrapper<FlwTask> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(FlwTask::getInstanceId,processInstanceId);
        lambdaQueryWrapper.select(FlwTask::getTaskName);
        List<FlwTask> flwTasks = flwTaskMapper.selectList(lambdaQueryWrapper);
        if(CollUtil.isEmpty(flwTasks)){
            return "";
        }
        else{
            String taskName = flwTasks.get(0).getTaskName();
            return taskName;
        }
    }

    @ApiOperation("通过流程实例id查看详情")
    @PostMapping("process/instanceInfo")
    public Result<HandleDataVO> instanceInfo(@RequestBody HandleDataDTO HandleDataDTO){
        String processInstanceId = HandleDataDTO.getProcessInstanceId();
        FlwHisInstance historicProcessInstance = flwHisInstanceMapper.selectById(processInstanceId);
        Long processId = historicProcessInstance.getProcessId();

        FlwProcess flwProcess = processService.getProcessById(processId);
        ProcessTemplates processTemplates = new ProcessTemplates();
        processTemplates.setLogo(flwProcess.getProcessIcon());
        processTemplates.setFormId(flwProcess.getId()+"");
        processTemplates.setFormName(flwProcess.getProcessName());

        processTemplates.setProcessDefinitionId(processId+"");
        String modelContent = flwProcess.getModelContent();
        DeployDTO deployDTO = JSONObject.parseObject(modelContent, new TypeReference<DeployDTO>() {
        });
        processTemplates.setProcess(deployDTO.getProcessJson());
        processTemplates.setFormItems(deployDTO.getFormJson());

        ProcessModel processModel = flwProcess.getProcessModel();
        //发起节点
        ChildNode childNode = processModel.getNodeConfig().getWChildNode();
        processTemplates.setFormItems(childNode.getFormJson());
        HandleDataVO handleDataVO =new HandleDataVO();
        Map<String, Object> processVariables = historicProcessInstance.getVariableMap();

        handleDataVO.setProcessInstanceId(historicProcessInstance.getId()+"");
        Map map = (Map) processVariables.get(FORM_VAR);
        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(map, SerializerFeature.WriteMapNullValue), new TypeReference<JSONObject>() {
        });
        handleDataVO.setFormData(jsonObject);
        String settingJson = childNode.getSettingJson();
        SettingsInfo settingsInfo = JSONObject.parseObject(settingJson, new TypeReference<SettingsInfo>() {});
        Boolean sign = settingsInfo.getSign();
        ChildNode currentNode=null;

        if(StringUtils.isNotBlank(HandleDataDTO.getTaskId())){
            FlwTask historicTaskInstance = flwTaskMapper.selectById(Long.valueOf(HandleDataDTO.getTaskId()));
            if(historicTaskInstance==null){
                historicTaskInstance = flwHisTaskMapper.getCheckById(Long.valueOf(HandleDataDTO.getTaskId()));
            }



            currentNode = getChildNodeByNodeId(processId + "", historicTaskInstance.getTaskNodeId());


            List<FormOperates> formPerms = currentNode.getProps().getFormPerms();
            if(CollUtil.isNotEmpty(formPerms)){
                Iterator<FormOperates> iterator = formPerms.iterator();
                while (iterator.hasNext()){
                    FormOperates next = iterator.next();
                    if("H".equals(next.getPerm())){
                        iterator.remove();
                        if(jsonObject!=null){
                            jsonObject.remove(next.getId());
                        }
                    }
                }
            }
            handleDataVO.setCurrentNode(currentNode);
            handleDataVO.setTaskId(HandleDataDTO.getTaskId());
        }
        else{
            currentNode = processModel.getNodeConfig().getWChildNode();
            processTemplates.setFormItems(currentNode.getFormJson());
            handleDataVO.setCurrentNode(currentNode);
        }
        if(sign){
            handleDataVO.setSignFlag(true);
        }
        else{
            if(StringUtils.isNotBlank(HandleDataDTO.getTaskId())){
                if(currentNode!=null){
                    Boolean sign1 = currentNode.getProps().getSign();
                    if(sign1!=null && sign1){
                        handleDataVO.setSignFlag(true);
                    }
                    else{
                        handleDataVO.setSignFlag(false);
                    }
                }
            }
            else {
                handleDataVO.setSignFlag(false);
            }
        }




        LambdaQueryWrapper<FlwHisTask>  taskLambdaQueryWrapper = new LambdaQueryWrapper<>();
        taskLambdaQueryWrapper.eq(FlwHisTask::getInstanceId,HandleDataDTO.getProcessInstanceId());
        List<FlwHisTask> list = flwHisTaskMapper.selectList(taskLambdaQueryWrapper);
        Map<String,List<FlwHisTask>> historicActivityInstanceMap =new HashMap<>();
        for (FlwHisTask historicActivityInstance : list) {
            List<FlwHisTask> historicActivityInstances = historicActivityInstanceMap.get(historicActivityInstance.getTaskNodeId());
            if(historicActivityInstances==null){
                historicActivityInstances =new ArrayList<>();
                historicActivityInstances.add(historicActivityInstance);
                historicActivityInstanceMap.put(historicActivityInstance.getTaskNodeId(),historicActivityInstances);
            }
            else{
                historicActivityInstances.add(historicActivityInstance);
                historicActivityInstanceMap.put(historicActivityInstance.getTaskNodeId(),historicActivityInstances);
            }
        }


//
//        Process mainProcess = repositoryService.getBpmnModel(historicProcessInstance.getProcessDefinitionId()).getMainProcess();
//        Collection<FlowElement> flowElements = mainProcess.getFlowElements();
//


        List<String> runningList= new ArrayList<>();

        Optional<List<FlwTask>> activeTasksByInstanceId = queryService.getActiveTasksByInstanceId(historicProcessInstance.getId());
        List<FlwTask> flwTaskList = activeTasksByInstanceId.get();
        if(CollUtil.isNotEmpty(flwTaskList)){
            for (FlwTask task : flwTaskList) {
                runningList.add(task.getTaskNodeId());
            }
        }

        handleDataVO.setRunningList(runningList);
        List<String> endList=new ArrayList<>();
        handleDataVO.setEndList(endList);
        List<String> noTakeList=new ArrayList<>();
        handleDataVO.setNoTakeList(noTakeList);



        Map<String, ChildNode> childNodeReturnMap = getChildNodeReturnMap(childNode);
        Set<String> mapKeys = childNodeReturnMap.keySet();
        mapKeys.removeAll(runningList);
        mapKeys.removeAll(endList);
        handleDataVO.setNoTakeList(CollUtil.newArrayList(mapKeys));
        //具体操作
        Set<String> strings = historicActivityInstanceMap.keySet();
        //
        for (String string : strings) {
            //
            List<FlwHisTask> flwHisTasks = historicActivityInstanceMap.get(string);
            for (FlwHisTask flwHisTask : flwHisTasks) {
                if(flwHisTask.getFinishTime()==null){
                    runningList.add(flwHisTask.getTaskNodeId());
                    break;
                }
                else{
                    endList.add(flwHisTask.getTaskNodeId());
                }
            }


        }


        handleDataVO.setProcessTemplates(processTemplates);
        handleDataVO.setDetailVOList(new HashMap<>());
        return Result.OK(handleDataVO);
    }

    private  static  void getChildNode(ChildNode childNode,Map<String,ChildNode> childNodeMap){
        childNodeMap.put(childNode.getId(),childNode);
        List<ChildNode> branchs = childNode.getBranchs();
        ChildNode children = childNode.getChildren();
        if(branchs!=null && branchs.size()>0){
            for (ChildNode branch : branchs) {
                if(StringUtils.isNotBlank(branch.getId())){
                    childNodeMap.put(branch.getId(),branch);
                    getChildNode(branch,childNodeMap);
                }
            }
        }

        if(children!=null ){
            childNodeMap.put(children.getId(),children);
            getChildNode(children,childNodeMap);
        }

    }
    public static ChildNode getChildNodeByNodeId(String processDefinitionId,String currentActivityId){
        ProcessService service = SpringContextHolder.getBean(ProcessService.class);
        ProcessModel processModel = service.getProcessById(Long.valueOf(processDefinitionId)).getProcessModel();

        //发起节点
        ChildNode childNode = processModel.getNodeConfig().getWChildNode();
        return getChildNode(childNode, currentActivityId);
    }

    public static   Map<String,ChildNode> getChildNodeReturnMap(ChildNode childNode){
        Map<String,ChildNode> childNodeMap =new HashMap<>();
        if(StringUtils.isNotBlank(childNode.getId())){
            getChildNode(childNode,childNodeMap);
        }
        return childNodeMap;
    }


    public static  ChildNode getChildNode(ChildNode childNode,String nodeId){
        Map<String,ChildNode> childNodeMap =new HashMap<>();
        if(StringUtils.isNotBlank(childNode.getId())){
            getChildNode(childNode,childNodeMap);
        }

        Set<String> set = childNodeMap.keySet();
        for (String s : set) {
            if(StringUtils.isNotBlank(s)){
                if(s.equals(nodeId)){
                    return childNodeMap.get(s);
                }
            }
        }
        return null;
    }
    @ApiOperation("查看我的待领任务")
    @PostMapping("process/claimList")
    public Result<Page<TaskVO>> claimList(@RequestBody TaskDTO taskDTO){
        List<FlwTask> flwTasks = flwTaskMapper.todoTask(taskDTO.getCurrentUserInfo().getId());
//        LambdaQueryWrapper<FlwTask> flwTaskLambdaQueryWrapper= new LambdaQueryWrapper<>();
//        flwTaskLambdaQueryWrapper.eq(FlwTask::getAssignorId,taskDTO.getCurrentUserInfo().getId());

//        List<FlwTask> flwTasks = flwTaskMapper.selectList(flwTaskLambdaQueryWrapper);
        long count = flwTasks.size();
        List<TaskVO> taskVOS= new ArrayList<>();
        Page<TaskVO> page =new Page<>();



        List<String> taskIds= new ArrayList<>();
        List<Long> instanceIds=new ArrayList<>();
        Map<Long,FlwInstance> flwInstanceMap = new HashMap<>();
        for (FlwTask flwTask : flwTasks) {
            instanceIds.add(flwTask.getInstanceId());
        }
        LambdaQueryWrapper<FlwInstance> flwInstanceLambdaQueryWrapper= new LambdaQueryWrapper<>();
        flwInstanceLambdaQueryWrapper.in(FlwInstance::getId,instanceIds);
        List<FlwInstance> flwInstances = flwInstanceMapper.selectList(flwInstanceLambdaQueryWrapper);
        for (FlwInstance flwInstance : flwInstances) {
            flwInstanceMap.put(flwInstance.getId(),flwInstance);
        }
        for (FlwTask task : flwTasks) {
            Long instanceId = task.getInstanceId();
            FlwInstance flwInstance = flwInstanceMap.get(instanceId);
            Map<String, Object> processVariables = flwInstance.getVariableMap();
            String id = JSONObject.parseObject(MapUtil.getStr(processVariables, START_USER_INFO), new TypeReference<UserInfo>() {
            }).getId();
            taskIds.add(id);
        }


        Map<Long, Users> collect=new HashMap<>();
        if(CollUtil.isNotEmpty(taskIds)){
            LambdaQueryWrapper<Users> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.in(Users::getUserId,taskIds);
            List<Users> list = userService.list(lambdaQueryWrapper);
            collect = list.stream().collect(Collectors.toMap(Users::getUserId, Function.identity()));
        }

        for (FlwTask task : flwTasks) {
            FlwInstance processInstance = flwInstanceMap.get(task.getInstanceId());
            Map<String, Object> processVariables = processInstance.getVariableMap();
            TaskVO taskVO=new TaskVO();
            taskVO.setTaskId(task.getId()+"");
            taskVO.setProcessInstanceId(task.getInstanceId()+"");
            String name = processService.getProcessById(processInstance.getProcessId()).getProcessModel().getName();
            taskVO.setProcessDefinitionName(name);
            taskVO.setStartUser(JSONObject.parseObject(MapUtil.getStr(processVariables,START_USER_INFO),new TypeReference<UserInfo>(){}));
            taskVO.setUsers(collect.get(Long.valueOf(taskVO.getStartUser().getId())));
            taskVO.setStartTime(processInstance.getCreateTime());

            taskVO.setCurrentActivityName(getCurrentName(processInstance.getId()+"",false));

            taskVO.setBusinessStatus(MapUtil.getStr(processVariables,PROCESS_STATUS));
            taskVO.setTaskCreatedTime(task.getCreateTime());
//            DelegationState delegationState = task.getDelegationState();
//            if(delegationState!=null){
//                taskVO.setDelegationState(delegationState);
//            }
            taskVOS.add(taskVO);

        }
        page.setRecords(taskVOS);
        page.setCurrent(taskDTO.getPageNo());
        page.setSize(taskDTO.getPageSize());
        page.setTotal(count);
        return Result.OK(page);
    }

    @ApiOperation("认领任务")
    @PostMapping("/claimTask")
    public Result claimTask(@RequestBody HandleDataDTO handleDataDTO){
        UserInfo currentUserInfo = handleDataDTO.getCurrentUserInfo();
        List<AttachmentDTO> attachments = handleDataDTO.getAttachments();
        String comments = handleDataDTO.getComments();
        JSONObject formData = handleDataDTO.getFormData();
        String taskId = handleDataDTO.getTaskId();
        FlwTask task = flwTaskMapper.getCheckById(Long.valueOf(taskId));
        Map<String,Object> map=new HashMap<>();
        if(formData!=null &&formData.size()>0){
            Map formValue = JSONObject.parseObject(formData.toJSONString(), new TypeReference<Map>() {
            });
            map.putAll(formValue);
            map.put(FORM_VAR,formData);
        }
        runtimeService.addVariable(task.getInstanceId(),map);
        List<ProcessComments> commentsList =new ArrayList<>();
        List<ProcessAttachments> attachmentsList = new ArrayList<>();
        if(StringUtils.isNotBlank(comments)){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(OPINION_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(comments);
            commentsList.add(processComments);

        }
        if(attachments!=null && attachments.size()>0){
            for (AttachmentDTO attachment : attachments) {
                ProcessAttachments processAttachments =new ProcessAttachments();
                processAttachments.setUserId(currentUserInfo.getId());
                processAttachments.setFileName(attachment.getName());
                processAttachments.setFileDesc(attachment.getName());
                processAttachments.setTypes(OPTION_COMMENT);
                processAttachments.setCreateTime(new Date());
                processAttachments.setTaskId(task.getId());
                processAttachments.setInstanceId(task.getInstanceId());
                processAttachments.setUrl(attachment.getUrl());
                attachmentsList.add(processAttachments);
            }
        }

        if(StringUtils.isNotBlank(handleDataDTO.getSignInfo())){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(SIGN_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(handleDataDTO.getSignInfo());
            commentsList.add(processComments);
        }
        if(CollUtil.isNotEmpty(attachmentsList)){
            processAttachmentsService.saveBatch(attachmentsList);
        }
        if(CollUtil.isNotEmpty(commentsList)){
            processCommentsService.saveBatch(commentsList);
        }


        UserInfo delegateUserInfo = handleDataDTO.getDelegateUserInfo();
        FlwHisTaskActor taskActor = (FlwHisTaskActor) FlwHisTaskActor.ofUser(delegateUserInfo.getId(), delegateUserInfo.getName());
        taskService.claim(task.getId(),taskActor);
        return Result.OK();
    }
    //todo 按照flowlong设计,没有办法做到反认领,因为他把actor都删了
    @ApiOperation("反认领任务")
//    @PostMapping("/unClaimTask")
    public Result unClaimTask(@RequestBody HandleDataDTO handleDataDTO){
        UserInfo currentUserInfo = handleDataDTO.getCurrentUserInfo();
        List<AttachmentDTO> attachments = handleDataDTO.getAttachments();
        String comments = handleDataDTO.getComments();
        JSONObject formData = handleDataDTO.getFormData();
        String taskId = handleDataDTO.getTaskId();
        FlwTask task = flwTaskMapper.getCheckById(Long.valueOf(taskId));
        Map<String,Object> map=new HashMap<>();
        if(formData!=null &&formData.size()>0){
            Map formValue = JSONObject.parseObject(formData.toJSONString(), new TypeReference<Map>() {
            });
            map.putAll(formValue);
            map.put(FORM_VAR,formData);
        }
        runtimeService.addVariable(task.getInstanceId(),map);
        List<ProcessComments> commentsList =new ArrayList<>();
        List<ProcessAttachments> attachmentsList = new ArrayList<>();
        if(StringUtils.isNotBlank(comments)){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(OPINION_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(comments);
            commentsList.add(processComments);

        }
        if(attachments!=null && attachments.size()>0){
            for (AttachmentDTO attachment : attachments) {
                ProcessAttachments processAttachments =new ProcessAttachments();
                processAttachments.setUserId(currentUserInfo.getId());
                processAttachments.setFileName(attachment.getName());
                processAttachments.setFileDesc(attachment.getName());
                processAttachments.setTypes(OPTION_COMMENT);
                processAttachments.setCreateTime(new Date());
                processAttachments.setTaskId(task.getId());
                processAttachments.setInstanceId(task.getInstanceId());
                processAttachments.setUrl(attachment.getUrl());
                attachmentsList.add(processAttachments);
            }
        }

        if(StringUtils.isNotBlank(handleDataDTO.getSignInfo())){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(SIGN_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(handleDataDTO.getSignInfo());
            commentsList.add(processComments);
        }
        if(CollUtil.isNotEmpty(attachmentsList)){
            processAttachmentsService.saveBatch(attachmentsList);
        }
        if(CollUtil.isNotEmpty(commentsList)){
            processCommentsService.saveBatch(commentsList);
        }


        UserInfo delegateUserInfo = handleDataDTO.getDelegateUserInfo();
        FlwTaskActor flwTaskActor = FlwTaskActor.ofUser(delegateUserInfo.getId(), delegateUserInfo.getName());
//        taskService.(task.getId(),flwTaskActor);
        return Result.OK();
    }


    @ApiOperation("查看我的待办")
    @PostMapping("process/toDoList")
    public Result<Page<TaskVO>> toDoList(@RequestBody TaskDTO taskDTO){
        LambdaQueryWrapper<FlwTask> flwTaskLambdaQueryWrapper= new LambdaQueryWrapper<>();
        flwTaskLambdaQueryWrapper.eq(FlwTask::getAssignorId,taskDTO.getCurrentUserInfo().getId());

        List<FlwTask> flwTasks = flwTaskMapper.selectList(flwTaskLambdaQueryWrapper);
        long count = flwTasks.size();
        List<TaskVO> taskVOS= new ArrayList<>();
        Page<TaskVO> page =new Page<>();
        if(CollUtil.isNotEmpty(flwTasks)){
            List<String> taskIds= new ArrayList<>();
            List<Long> instanceIds=new ArrayList<>();
            Map<Long,FlwInstance> flwInstanceMap = new HashMap<>();
            for (FlwTask flwTask : flwTasks) {
                instanceIds.add(flwTask.getInstanceId());
            }
            LambdaQueryWrapper<FlwInstance> flwInstanceLambdaQueryWrapper= new LambdaQueryWrapper<>();
            flwInstanceLambdaQueryWrapper.in(FlwInstance::getId,instanceIds);
            List<FlwInstance> flwInstances = flwInstanceMapper.selectList(flwInstanceLambdaQueryWrapper);
            for (FlwInstance flwInstance : flwInstances) {
                flwInstanceMap.put(flwInstance.getId(),flwInstance);
            }
            for (FlwTask task : flwTasks) {
                Long instanceId = task.getInstanceId();
                FlwInstance flwInstance = flwInstanceMap.get(instanceId);
                Map<String, Object> processVariables = flwInstance.getVariableMap();
                String id = JSONObject.parseObject(MapUtil.getStr(processVariables, START_USER_INFO), new TypeReference<UserInfo>() {
                }).getId();
                taskIds.add(id);
            }


            Map<Long, Users> collect=new HashMap<>();
            if(CollUtil.isNotEmpty(taskIds)){
                LambdaQueryWrapper<Users> lambdaQueryWrapper = new LambdaQueryWrapper<>();
                lambdaQueryWrapper.in(Users::getUserId,taskIds);
                List<Users> list = userService.list(lambdaQueryWrapper);
                collect = list.stream().collect(Collectors.toMap(Users::getUserId, Function.identity()));
            }

            for (FlwTask task : flwTasks) {
                FlwInstance processInstance = flwInstanceMap.get(task.getInstanceId());
                Map<String, Object> processVariables = processInstance.getVariableMap();
                TaskVO taskVO=new TaskVO();
                taskVO.setTaskId(task.getId()+"");
                taskVO.setProcessInstanceId(task.getInstanceId()+"");
                String name = processService.getProcessById(processInstance.getProcessId()).getProcessModel().getName();
                taskVO.setProcessDefinitionName(name);
                taskVO.setStartUser(JSONObject.parseObject(MapUtil.getStr(processVariables,START_USER_INFO),new TypeReference<UserInfo>(){}));
                taskVO.setUsers(collect.get(Long.valueOf(taskVO.getStartUser().getId())));
                taskVO.setStartTime(processInstance.getCreateTime());

                taskVO.setCurrentActivityName(getCurrentName(processInstance.getId()+"",false));

                taskVO.setBusinessStatus(MapUtil.getStr(processVariables,PROCESS_STATUS));
                taskVO.setTaskCreatedTime(task.getCreateTime());
//            DelegationState delegationState = task.getDelegationState();
//            if(delegationState!=null){
//                taskVO.setDelegationState(delegationState);
//            }
                taskVOS.add(taskVO);

            }
            page.setRecords(taskVOS);
        }



        page.setCurrent(taskDTO.getPageNo());
        page.setSize(taskDTO.getPageSize());
        page.setTotal(count);
        return Result.OK(page);
    }

    @ApiOperation("查看我的已办")
    @PostMapping("process/doneList")
    public Result<Page<TaskVO>> doneList(@RequestBody TaskDTO taskDTO){
        LambdaQueryWrapper<FlwHisTask> flwHisTaskLambdaQueryWrapper  = new LambdaQueryWrapper<>();
        flwHisTaskLambdaQueryWrapper.eq(FlwHisTask::getAssignorId,taskDTO.getCurrentUserInfo().getId())
                .isNotNull(FlwHisTask::getFinishTime);
        List<FlwHisTask> flwHisTasks = flwHisTaskMapper.selectList(flwHisTaskLambdaQueryWrapper);
        Page<TaskVO> page=new Page<>();
        if(CollUtil.isNotEmpty(flwHisTasks)){
            List<TaskVO> taskVOS= new ArrayList<>();
            Map<Long, Users> collect=new HashMap<>();
            List<String> taskIds= new ArrayList<>();
            List<Long> instanceIds=new ArrayList<>();
            Map<Long,FlwHisInstance> flwInstanceMap = new HashMap<>();
            for (FlwTask flwTask : flwHisTasks) {
                instanceIds.add(flwTask.getInstanceId());
            }
            LambdaQueryWrapper<FlwHisInstance> flwInstanceLambdaQueryWrapper= new LambdaQueryWrapper<>();
            flwInstanceLambdaQueryWrapper.in(FlwHisInstance::getId,instanceIds);
            List<FlwHisInstance> flwInstances = flwHisInstanceMapper.selectList(flwInstanceLambdaQueryWrapper);
            for (FlwHisInstance flwInstance : flwInstances) {
                flwInstanceMap.put(flwInstance.getId(),flwInstance);
            }
            for (FlwTask task : flwHisTasks) {
                Long instanceId = task.getInstanceId();
                FlwInstance flwInstance = flwInstanceMap.get(instanceId);
                Map<String, Object> processVariables = flwInstance.getVariableMap();
                String id = JSONObject.parseObject(MapUtil.getStr(processVariables, START_USER_INFO), new TypeReference<UserInfo>() {
                }).getId();
                taskIds.add(id);
            }


            for (FlwHisTask task : flwHisTasks) {
                FlwHisInstance processInstance = flwInstanceMap.get(task.getInstanceId());
                Map<String, Object> processVariables = processInstance.getVariableMap();
                TaskVO taskVO=new TaskVO();
                taskVO.setTaskId(task.getId()+"");
                taskVO.setTaskName(task.getTaskName());
                String name = processService.getProcessById(processInstance.getProcessId()).getProcessModel().getName();
                taskVO.setProcessDefinitionName(name);
                taskVO.setStartUser(JSONObject.parseObject(MapUtil.getStr(processVariables,START_USER_INFO),new TypeReference<UserInfo>(){}));
                taskVO.setUsers(collect.get(Long.valueOf(taskVO.getStartUser().getId())));
                taskVO.setStartTime(processInstance.getCreateTime());
                Boolean flag= processInstance.getEndTime() != null;
                taskVO.setCurrentActivityName(getCurrentName(processInstance.getId()+"",flag));
                taskVO.setBusinessStatus(MapUtil.getStr(processVariables,PROCESS_STATUS));
                taskVO.setEndTime(task.getFinishTime());

                long totalTimes = task.getFinishTime()==null?
                        (Calendar.getInstance().getTimeInMillis()-task.getCreateTime().getTime()):
                        (task.getFinishTime().getTime()-task.getCreateTime().getTime());
                long dayCount = totalTimes /(1000*60*60*24);//计算天
                long restTimes = totalTimes %(1000*60*60*24);//剩下的时间用于计于小时
                long hourCount = restTimes/(1000*60*60);//小时
                restTimes = restTimes % (1000*60*60);
                long minuteCount = restTimes / (1000*60);
                String spendTimes = dayCount+"天"+hourCount+"小时"+minuteCount+"分";
                taskVO.setDuration(spendTimes);
                taskVOS.add(taskVO);
            }
            page.setRecords(taskVOS);
        }



        page.setCurrent(taskDTO.getPageNo());
        page.setSize(taskDTO.getPageSize());
        page.setTotal(flwHisTasks.size());
        return Result.OK(page);
    }
    @ApiOperation("同意按钮")
    @PostMapping("/agree")
    public Result agree(@RequestBody HandleDataDTO handleDataDTO){
        UserInfo currentUserInfo = handleDataDTO.getCurrentUserInfo();
        List<AttachmentDTO> attachments = handleDataDTO.getAttachments();
        String comments = handleDataDTO.getComments();
        JSONObject formData = handleDataDTO.getFormData();
        String taskId = handleDataDTO.getTaskId();
        FlwTask task = flwTaskMapper.getCheckById(Long.valueOf(taskId));
//        if(DelegationState.PENDING.equals(task.getDelegationState())){
//            return Result.error("委派人不可以点击同意按钮,而应该点击 委派人完成按钮");
//        }
        Map<String,Object> map=new HashMap<>();
        if(formData!=null &&formData.size()>0){
            Map formValue = JSONObject.parseObject(formData.toJSONString(), new TypeReference<Map>() {
            });
            map.putAll(formValue);
            map.put(FORM_VAR,formData);
        }

        runtimeService.addVariable(task.getInstanceId(),map);

        List<ProcessComments> commentsList =new ArrayList<>();
        List<ProcessAttachments> attachmentsList = new ArrayList<>();
        if(StringUtils.isNotBlank(comments)){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(OPINION_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(comments);
            commentsList.add(processComments);

        }
        if(attachments!=null && attachments.size()>0){
            for (AttachmentDTO attachment : attachments) {
                ProcessAttachments processAttachments =new ProcessAttachments();
                processAttachments.setUserId(currentUserInfo.getId());
                processAttachments.setFileName(attachment.getName());
                processAttachments.setFileDesc(attachment.getName());
                processAttachments.setTypes(OPTION_COMMENT);
                processAttachments.setCreateTime(new Date());
                processAttachments.setTaskId(task.getId());
                processAttachments.setInstanceId(task.getInstanceId());
                processAttachments.setUrl(attachment.getUrl());
                attachmentsList.add(processAttachments);
            }
        }

        if(StringUtils.isNotBlank(handleDataDTO.getSignInfo())){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(SIGN_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(handleDataDTO.getSignInfo());
            commentsList.add(processComments);
        }
        if(CollUtil.isNotEmpty(attachmentsList)){
            processAttachmentsService.saveBatch(attachmentsList);
        }
        if(CollUtil.isNotEmpty(commentsList)){
            processCommentsService.saveBatch(commentsList);
        }

        FlowCreator flowCreator = FlowCreator.of(currentUserInfo.getId(), currentUserInfo.getName());
//        taskService.complete(task.getId(),flowCreator);
        flowLongEngine.executeTask(task.getId(),flowCreator);
//        flowLongEngine.executeAndJumpTask();
        return Result.OK();
    }
    @ApiOperation("委派按钮")
    @PostMapping("/delegateTask")
    public Result delegateTask(@RequestBody HandleDataDTO handleDataDTO){
        UserInfo currentUserInfo = handleDataDTO.getCurrentUserInfo();
        List<AttachmentDTO> attachments = handleDataDTO.getAttachments();
        String comments = handleDataDTO.getComments();
        JSONObject formData = handleDataDTO.getFormData();
        String taskId = handleDataDTO.getTaskId();
        FlwTask task = flwTaskMapper.getCheckById(Long.valueOf(taskId));
        Map<String,Object> map=new HashMap<>();
        if(formData!=null &&formData.size()>0){
            Map formValue = JSONObject.parseObject(formData.toJSONString(), new TypeReference<Map>() {
            });
            map.putAll(formValue);
            map.put(FORM_VAR,formData);
        }

        runtimeService.addVariable(task.getInstanceId(),map);
        List<ProcessComments> commentsList =new ArrayList<>();
        List<ProcessAttachments> attachmentsList = new ArrayList<>();
        if(StringUtils.isNotBlank(comments)){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(OPINION_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(comments);
            commentsList.add(processComments);

        }
        if(attachments!=null && attachments.size()>0){
            for (AttachmentDTO attachment : attachments) {
                ProcessAttachments processAttachments =new ProcessAttachments();
                processAttachments.setUserId(currentUserInfo.getId());
                processAttachments.setFileName(attachment.getName());
                processAttachments.setFileDesc(attachment.getName());
                processAttachments.setTypes(OPTION_COMMENT);
                processAttachments.setCreateTime(new Date());
                processAttachments.setTaskId(task.getId());
                processAttachments.setInstanceId(task.getInstanceId());
                processAttachments.setUrl(attachment.getUrl());
                attachmentsList.add(processAttachments);
            }
        }

        if(StringUtils.isNotBlank(handleDataDTO.getSignInfo())){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(SIGN_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(handleDataDTO.getSignInfo());
            commentsList.add(processComments);
        }
        if(CollUtil.isNotEmpty(attachmentsList)){
            processAttachmentsService.saveBatch(attachmentsList);
        }
        if(CollUtil.isNotEmpty(commentsList)){
            processCommentsService.saveBatch(commentsList);
        }

        UserInfo delegateUserInfo = handleDataDTO.getDelegateUserInfo();

        FlwTaskActor flwTaskActor1 = FlwHisTaskActor.ofUser(currentUserInfo.getId(), currentUserInfo.getName());
        flwTaskActor1.setTaskId(task.getId());
        flwTaskActor1.setInstanceId(task.getInstanceId());
        flwTaskActor1.setActorType(0);
        FlwHisTaskActor currentActor = FlwHisTaskActor.of(flwTaskActor1);
        FlwTaskActor flwTaskActor = FlwHisTaskActor.ofUser(delegateUserInfo.getId(), delegateUserInfo.getName());
        flwTaskActor.setTaskId(task.getId());
        flwTaskActor.setInstanceId(task.getInstanceId());
        flwTaskActor.setActorType(0);
        //todo 委派代码实现有问题, 另议
        FlwHisTaskActor deletegateActor = FlwHisTaskActor.of(flwTaskActor);
        taskService.delegateTask(task.getId(),currentActor,deletegateActor);
        return Result.OK();
    }

    @ApiOperation("委派人完成的按钮")
//    @PostMapping("/resolveTask")
    public Result resolveTask(@RequestBody HandleDataDTO handleDataDTO){
        UserInfo currentUserInfo = handleDataDTO.getCurrentUserInfo();
        List<AttachmentDTO> attachments = handleDataDTO.getAttachments();
        String comments = handleDataDTO.getComments();
        JSONObject formData = handleDataDTO.getFormData();
        String taskId = handleDataDTO.getTaskId();
        FlwTask task = flwTaskMapper.getCheckById(Long.valueOf(taskId));
        Map<String,Object> map=new HashMap<>();
        if(formData!=null &&formData.size()>0){
            Map formValue = JSONObject.parseObject(formData.toJSONString(), new TypeReference<Map>() {
            });
            map.putAll(formValue);
            map.put(FORM_VAR,formData);
        }

        runtimeService.addVariable(task.getInstanceId(),map);
        List<ProcessComments> commentsList =new ArrayList<>();
        List<ProcessAttachments> attachmentsList = new ArrayList<>();
        if(StringUtils.isNotBlank(comments)){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(OPINION_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(comments);
            commentsList.add(processComments);

        }
        if(attachments!=null && attachments.size()>0){
            for (AttachmentDTO attachment : attachments) {
                ProcessAttachments processAttachments =new ProcessAttachments();
                processAttachments.setUserId(currentUserInfo.getId());
                processAttachments.setFileName(attachment.getName());
                processAttachments.setFileDesc(attachment.getName());
                processAttachments.setTypes(OPTION_COMMENT);
                processAttachments.setCreateTime(new Date());
                processAttachments.setTaskId(task.getId());
                processAttachments.setInstanceId(task.getInstanceId());
                processAttachments.setUrl(attachment.getUrl());
                attachmentsList.add(processAttachments);
            }
        }

        if(StringUtils.isNotBlank(handleDataDTO.getSignInfo())){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(SIGN_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(handleDataDTO.getSignInfo());
            commentsList.add(processComments);
        }
        if(CollUtil.isNotEmpty(attachmentsList)){
            processAttachmentsService.saveBatch(attachmentsList);
        }
        if(CollUtil.isNotEmpty(commentsList)){
            processCommentsService.saveBatch(commentsList);
        }

        FlowCreator flowCreator = FlowCreator.of(currentUserInfo.getId(), currentUserInfo.getName());
        taskService.complete(task.getId(),flowCreator);
        return Result.OK();
    }

    @ApiOperation("拒绝按钮")
    @PostMapping("/refuse")
    public Result refuse(@RequestBody HandleDataDTO handleDataDTO){
        UserInfo currentUserInfo = handleDataDTO.getCurrentUserInfo();
        List<AttachmentDTO> attachments = handleDataDTO.getAttachments();
        String comments = handleDataDTO.getComments();
        JSONObject formData = handleDataDTO.getFormData();
        String taskId = handleDataDTO.getTaskId();
        FlwTask task = flwTaskMapper.getCheckById(Long.valueOf(taskId));
        Map<String,Object> map=new HashMap<>();
        if(formData!=null &&formData.size()>0){
            Map formValue = JSONObject.parseObject(formData.toJSONString(), new TypeReference<Map>() {
            });
            map.putAll(formValue);
            map.put(FORM_VAR,formData);
        }

        runtimeService.addVariable(task.getInstanceId(),map);
        List<ProcessComments> commentsList =new ArrayList<>();
        List<ProcessAttachments> attachmentsList = new ArrayList<>();
        if(StringUtils.isNotBlank(comments)){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(OPINION_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(comments);
            commentsList.add(processComments);

        }
        if(attachments!=null && attachments.size()>0){
            for (AttachmentDTO attachment : attachments) {
                ProcessAttachments processAttachments =new ProcessAttachments();
                processAttachments.setUserId(currentUserInfo.getId());
                processAttachments.setFileName(attachment.getName());
                processAttachments.setFileDesc(attachment.getName());
                processAttachments.setTypes(OPTION_COMMENT);
                processAttachments.setCreateTime(new Date());
                processAttachments.setTaskId(task.getId());
                processAttachments.setInstanceId(task.getInstanceId());
                processAttachments.setUrl(attachment.getUrl());
                attachmentsList.add(processAttachments);
            }
        }

        if(StringUtils.isNotBlank(handleDataDTO.getSignInfo())){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(SIGN_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(handleDataDTO.getSignInfo());
            commentsList.add(processComments);
        }
        if(CollUtil.isNotEmpty(attachmentsList)){
            processAttachmentsService.saveBatch(attachmentsList);
        }
        if(CollUtil.isNotEmpty(commentsList)){
            processCommentsService.saveBatch(commentsList);
        }

        FlwHisInstance flwHisInstance = flwHisInstanceMapper.selectById(task.getInstanceId());

        ChildNode childNodeByNodeId = getChildNodeByNodeId(flwHisInstance.getProcessId()+"", task.getTaskNodeId());
        Map<String, Object> refuse = childNodeByNodeId.getProps().getRefuse();
        String type = MapUtil.getStr(refuse, "type");
        if(RefuseEnums.TO_END.getTypeName().equals(type)){
            FlowCreator of = FlowCreator.of(task.getAssignorId(), task.getAssignor());
            runtimeService.terminate(task.getInstanceId(),of);
        }
        else if(RefuseEnums.TO_BEFORE.getTypeName().equals(type)){
            taskService.rejectTask(task,FlowCreator.of(currentUserInfo.getId(),currentUserInfo.getName()));
//            throw new WorkFlowException("他没写,我也不写,嘿嘿");
        }
        //TODO 不实现复杂退回模式,他没写我也不写
        else if(RefuseEnums.TO_NODE.getTypeName().equals(type)){
            String target = MapUtil.getStr(refuse, "target");
            flowLongEngine.executeAndJumpPointIdTask(task.getId(),target,FlowCreator.of(currentUserInfo.getId(),currentUserInfo.getName()));
        }

        return Result.OK();
    }


    @ApiOperation("撤销按钮")
    @PostMapping("/revoke")
    public Result revoke(@RequestBody HandleDataDTO handleDataDTO){
        UserInfo currentUserInfo = handleDataDTO.getCurrentUserInfo();
        List<AttachmentDTO> attachments = handleDataDTO.getAttachments();
        String comments = handleDataDTO.getComments();
        JSONObject formData = handleDataDTO.getFormData();
        String taskId = handleDataDTO.getTaskId();
        FlwTask task=null;
        if(null == taskId){
            //通过流程实例id找最新的taskId
            Optional<List<FlwTask>> flwTaskList = queryService.getActiveTasksByInstanceId(Long.valueOf(handleDataDTO.getProcessInstanceId()));
            if(CollUtil.isNotEmpty(flwTaskList.get())){
                task = flwTaskList.get().get(0);
            }
        }else {
            task = flwTaskMapper.getCheckById(Long.valueOf(handleDataDTO.getTaskId()));
        }
        if(null == task){
            return Result.error("找不到任务");
        }
        Map<String,Object> map=new HashMap<>();
        if(formData!=null &&formData.size()>0){
            Map formValue = JSONObject.parseObject(formData.toJSONString(), new TypeReference<Map>() {
            });
            map.putAll(formValue);
            map.put(FORM_VAR,formData);
        }

        runtimeService.addVariable(task.getInstanceId(),map);
        List<ProcessComments> commentsList =new ArrayList<>();
        List<ProcessAttachments> attachmentsList = new ArrayList<>();
        if(StringUtils.isNotBlank(comments)){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(OPINION_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(comments);
            commentsList.add(processComments);

        }
        if(attachments!=null && attachments.size()>0){
            for (AttachmentDTO attachment : attachments) {
                ProcessAttachments processAttachments =new ProcessAttachments();
                processAttachments.setUserId(currentUserInfo.getId());
                processAttachments.setFileName(attachment.getName());
                processAttachments.setFileDesc(attachment.getName());
                processAttachments.setTypes(OPTION_COMMENT);
                processAttachments.setCreateTime(new Date());
                processAttachments.setTaskId(task.getId());
                processAttachments.setInstanceId(task.getInstanceId());
                processAttachments.setUrl(attachment.getUrl());
                attachmentsList.add(processAttachments);
            }
        }

        if(StringUtils.isNotBlank(handleDataDTO.getSignInfo())){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(SIGN_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(handleDataDTO.getSignInfo());
            commentsList.add(processComments);
        }
        if(CollUtil.isNotEmpty(attachmentsList)){
            processAttachmentsService.saveBatch(attachmentsList);
        }
        if(CollUtil.isNotEmpty(commentsList)){
            processCommentsService.saveBatch(commentsList);
        }

        FlowCreator of = FlowCreator.of(task.getAssignorId(), task.getAssignor());
        runtimeService.terminate(task.getInstanceId(),of);
        return Result.OK();
    }

    @ApiOperation("转办按钮")
    @PostMapping("/assignee")
    public Result assignee(@RequestBody HandleDataDTO handleDataDTO){
        UserInfo currentUserInfo = handleDataDTO.getCurrentUserInfo();

        List<AttachmentDTO> attachments = handleDataDTO.getAttachments();
        String comments = handleDataDTO.getComments();
        JSONObject formData = handleDataDTO.getFormData();
        String taskId = handleDataDTO.getTaskId();
        FlwTask task = flwTaskMapper.getCheckById(Long.valueOf(taskId));
        Map<String,Object> map=new HashMap<>();
        if(formData!=null &&formData.size()>0){
            Map formValue = JSONObject.parseObject(formData.toJSONString(), new TypeReference<Map>() {
            });
            map.putAll(formValue);
            map.put(FORM_VAR,formData);
        }

        runtimeService.addVariable(task.getInstanceId(),map);
        List<ProcessComments> commentsList =new ArrayList<>();
        List<ProcessAttachments> attachmentsList = new ArrayList<>();
        if(StringUtils.isNotBlank(comments)){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(OPINION_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(comments);
            commentsList.add(processComments);

        }
        if(attachments!=null && attachments.size()>0){
            for (AttachmentDTO attachment : attachments) {
                ProcessAttachments processAttachments =new ProcessAttachments();
                processAttachments.setUserId(currentUserInfo.getId());
                processAttachments.setFileName(attachment.getName());
                processAttachments.setFileDesc(attachment.getName());
                processAttachments.setTypes(OPTION_COMMENT);
                processAttachments.setCreateTime(new Date());
                processAttachments.setTaskId(task.getId());
                processAttachments.setInstanceId(task.getInstanceId());
                processAttachments.setUrl(attachment.getUrl());
                attachmentsList.add(processAttachments);
            }
        }

        if(StringUtils.isNotBlank(handleDataDTO.getSignInfo())){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(SIGN_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(handleDataDTO.getSignInfo());
            commentsList.add(processComments);
        }
        if(CollUtil.isNotEmpty(attachmentsList)){
            processAttachmentsService.saveBatch(attachmentsList);
        }
        if(CollUtil.isNotEmpty(commentsList)){
            processCommentsService.saveBatch(commentsList);
        }
        UserInfo transferUserInfo = handleDataDTO.getTransferUserInfo();
        FlwTaskActor flwTaskActor1 = FlwHisTaskActor.ofUser(currentUserInfo.getId(), currentUserInfo.getName());
        flwTaskActor1.setTaskId(task.getId());
        flwTaskActor1.setInstanceId(task.getInstanceId());
        flwTaskActor1.setActorType(0);
        FlwHisTaskActor currentActor = FlwHisTaskActor.of(flwTaskActor1);
        FlwTaskActor flwTaskActor = FlwHisTaskActor.ofUser(transferUserInfo.getId(), transferUserInfo.getName());
        flwTaskActor.setTaskId(task.getId());
        flwTaskActor.setInstanceId(task.getInstanceId());
        flwTaskActor.setActorType(0);
        taskService.transferTask(Long.valueOf(taskId),flwTaskActor1,flwTaskActor);
        return Result.OK();
    }
    @ApiOperation("查询可退回的节点(这个是给 下面 rollback接口作为入参用的 )")
    @PostMapping("/rollbackNodes")
    public Result rollbackNodes(@RequestBody HandleDataDTO handleDataDTO){
        LambdaQueryWrapper<FlwHisTask> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.isNotNull(FlwHisTask::getFinishTime);
        lambdaQueryWrapper.eq(FlwHisTask::getInstanceId,Long.valueOf(handleDataDTO.getProcessInstanceId()));
        List<FlwHisTask> flwHisTasks = flwHisTaskMapper.selectList(lambdaQueryWrapper);

        Map<String,String> nodes=new HashMap<>();
        for (FlwHisTask activityInstance : flwHisTasks) {
            nodes.put(activityInstance.getTaskNodeId(),activityInstance.getTaskName());
        }
        return Result.OK(nodes);
    }

    @ApiOperation("退回按钮")
    @PostMapping("/rollback")
    public Result rollback(@RequestBody HandleDataDTO handleDataDTO){
        UserInfo currentUserInfo = handleDataDTO.getCurrentUserInfo();
        List<AttachmentDTO> attachments = handleDataDTO.getAttachments();
        String comments = handleDataDTO.getComments();
        JSONObject formData = handleDataDTO.getFormData();
        String taskId = handleDataDTO.getTaskId();
        String processInstanceId = handleDataDTO.getProcessInstanceId();
        FlwTask task = flwTaskMapper.getCheckById(Long.valueOf(taskId));
        Map<String,Object> map=new HashMap<>();
        if(formData!=null &&formData.size()>0){
            Map formValue = JSONObject.parseObject(formData.toJSONString(), new TypeReference<Map>() {
            });
            map.putAll(formValue);
            map.put(FORM_VAR,formData);
        }

        runtimeService.addVariable(task.getInstanceId(),map);
        List<ProcessComments> commentsList =new ArrayList<>();
        List<ProcessAttachments> attachmentsList = new ArrayList<>();
        if(StringUtils.isNotBlank(comments)){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(OPINION_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(comments);
            commentsList.add(processComments);

        }
        if(attachments!=null && attachments.size()>0){
            for (AttachmentDTO attachment : attachments) {
                ProcessAttachments processAttachments =new ProcessAttachments();
                processAttachments.setUserId(currentUserInfo.getId());
                processAttachments.setFileName(attachment.getName());
                processAttachments.setFileDesc(attachment.getName());
                processAttachments.setTypes(OPTION_COMMENT);
                processAttachments.setCreateTime(new Date());
                processAttachments.setTaskId(task.getId());
                processAttachments.setInstanceId(task.getInstanceId());
                processAttachments.setUrl(attachment.getUrl());
                attachmentsList.add(processAttachments);
            }
        }

        if(StringUtils.isNotBlank(handleDataDTO.getSignInfo())){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(SIGN_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(handleDataDTO.getSignInfo());
            commentsList.add(processComments);
        }
        if(CollUtil.isNotEmpty(attachmentsList)){
            processAttachmentsService.saveBatch(attachmentsList);
        }
        if(CollUtil.isNotEmpty(commentsList)){
            processCommentsService.saveBatch(commentsList);
        }
        FlowCreator flowCreator = FlowCreator.of(currentUserInfo.getId(), currentUserInfo.getName());
        flowLongEngine.executeAndJumpPointIdTask(task.getId(),handleDataDTO.getRollbackId(),flowCreator);
        return Result.OK();
    }



    @ApiOperation("加签按钮")
    @PostMapping("/addMulti")
    public Result addMulti(@RequestBody HandleDataDTO handleDataDTO){
        UserInfo currentUserInfo = handleDataDTO.getCurrentUserInfo();
        List<AttachmentDTO> attachments = handleDataDTO.getAttachments();
        String comments = handleDataDTO.getComments();
        JSONObject formData = handleDataDTO.getFormData();
        String taskId = handleDataDTO.getTaskId();
        FlwTask task = flwTaskMapper.getCheckById(Long.valueOf(taskId));
        Map<String,Object> map=new HashMap<>();
        if(formData!=null &&formData.size()>0){
            Map formValue = JSONObject.parseObject(formData.toJSONString(), new TypeReference<Map>() {
            });
            map.putAll(formValue);
            map.put(FORM_VAR,formData);
        }

        runtimeService.addVariable(task.getInstanceId(),map);
        List<ProcessComments> commentsList =new ArrayList<>();
        List<ProcessAttachments> attachmentsList = new ArrayList<>();
        if(StringUtils.isNotBlank(comments)){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(OPINION_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(comments);
            commentsList.add(processComments);

        }
        if(attachments!=null && attachments.size()>0){
            for (AttachmentDTO attachment : attachments) {
                ProcessAttachments processAttachments =new ProcessAttachments();
                processAttachments.setUserId(currentUserInfo.getId());
                processAttachments.setFileName(attachment.getName());
                processAttachments.setFileDesc(attachment.getName());
                processAttachments.setTypes(OPTION_COMMENT);
                processAttachments.setCreateTime(new Date());
                processAttachments.setTaskId(task.getId());
                processAttachments.setInstanceId(task.getInstanceId());
                processAttachments.setUrl(attachment.getUrl());
                attachmentsList.add(processAttachments);
            }
        }

        if(StringUtils.isNotBlank(handleDataDTO.getSignInfo())){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(SIGN_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(handleDataDTO.getSignInfo());
            commentsList.add(processComments);
        }
        if(CollUtil.isNotEmpty(attachmentsList)){
            processAttachmentsService.saveBatch(attachmentsList);
        }
        if(CollUtil.isNotEmpty(commentsList)){
            processCommentsService.saveBatch(commentsList);
        }
        UserInfo multiAddUserInfo = handleDataDTO.getMultiAddUserInfo();
        FlwHisTaskActor flwTaskActor = (FlwHisTaskActor) FlwHisTaskActor.ofUser(multiAddUserInfo.getId(), multiAddUserInfo.getName());

        //
        Integer performType = task.getPerformType();
        taskService.addTaskActor(task.getId(), PerformType.get(performType),flwTaskActor);
        return Result.OK();
    }


    @ApiOperation("查到签上的人")
    @PostMapping("/queryMultiUsersInfo")
    public Result<List<MultiVO>> queryMultiUsersInfo(@RequestBody Map<String,Object> map){
        String taskId = MapUtil.getStr(map, "taskId");
        FlwTask task = flwTaskMapper.getCheckById(Long.valueOf(taskId));
        List<MultiVO> multiVOS = new ArrayList<>();

        List<FlwTaskActor> flwTaskActors = queryService.getTaskActorsByTaskId(task.getId());
        for (FlwTaskActor flwTaskActor : flwTaskActors) {
                MultiVO multiVO=new MultiVO();
                multiVO.setTaskId(flwTaskActor.getTaskId()+"");
                multiVO.setProcessInstanceId(task.getInstanceId()+"");
                multiVO.setExecutionId(task.getId()+"---"+flwTaskActor.getActorId());
                multiVO.setUserId(flwTaskActor.getActorId());
                multiVO.setUserName(flwTaskActor.getActorName());
            multiVOS.add(multiVO);
        }


        return Result.OK(multiVOS);
    }

    @ApiOperation("减签按钮")
    @PostMapping("/deleteMulti")
    public Result deleteMulti(@RequestBody Map<String,Object> map){
        List<String> executionIds = MapUtil.get(map, "executionIds", List.class);
        String  taskId="";
        List<String> actorIds=new ArrayList<>();
        for (String executionId : executionIds) {
            String[] split = executionId.split("---");
            taskId=split[0];
            actorIds.add(split[1]);
        }
        taskService.removeTaskActor(Long.valueOf(taskId),actorIds);
        return Result.OK();
    }


    @ApiOperation("评论按钮")
    @PostMapping("/comments")
    public Result comments(@RequestBody HandleDataDTO handleDataDTO){
        UserInfo currentUserInfo = handleDataDTO.getCurrentUserInfo();
        List<AttachmentDTO> attachments = handleDataDTO.getAttachments();
        String comments = handleDataDTO.getComments();
        JSONObject formData = handleDataDTO.getFormData();
        String taskId = handleDataDTO.getTaskId();
        FlwTask task = flwTaskMapper.getCheckById(Long.valueOf(taskId));
        Map<String,Object> map=new HashMap<>();
        if(formData!=null &&formData.size()>0){
            Map formValue = JSONObject.parseObject(formData.toJSONString(), new TypeReference<Map>() {
            });
            map.putAll(formValue);
            map.put(FORM_VAR,formData);
        }

        runtimeService.addVariable(task.getInstanceId(),map);
        List<ProcessComments> commentsList =new ArrayList<>();
        List<ProcessAttachments> attachmentsList = new ArrayList<>();
        if(StringUtils.isNotBlank(comments)){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(COMMENTS_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(comments);
            commentsList.add(processComments);

        }
        if(attachments!=null && attachments.size()>0){
            for (AttachmentDTO attachment : attachments) {
                ProcessAttachments processAttachments =new ProcessAttachments();
                processAttachments.setUserId(currentUserInfo.getId());
                processAttachments.setFileName(attachment.getName());
                processAttachments.setFileDesc(attachment.getName());
                processAttachments.setTypes(OPTION_COMMENT);
                processAttachments.setCreateTime(new Date());
                processAttachments.setTaskId(task.getId());
                processAttachments.setInstanceId(task.getInstanceId());
                processAttachments.setUrl(attachment.getUrl());
                attachmentsList.add(processAttachments);
            }
        }

        if(StringUtils.isNotBlank(handleDataDTO.getSignInfo())){
            ProcessComments processComments =new ProcessComments();
            processComments.setTypes(SIGN_COMMENT);
            processComments.setCreateTime(new Date());
            processComments.setUserId(currentUserInfo.getId());
            processComments.setTaskId(task.getId());
            processComments.setInstanceId(task.getInstanceId());
            processComments.setMessage(handleDataDTO.getSignInfo());
            commentsList.add(processComments);
        }
        if(CollUtil.isNotEmpty(attachmentsList)){
            processAttachmentsService.saveBatch(attachmentsList);
        }
        if(CollUtil.isNotEmpty(commentsList)){
            processCommentsService.saveBatch(commentsList);
        }
        return Result.OK();
    }


    @ApiOperation("通过流程实例查看审批记录")
    @PostMapping("process/record/{processInstanceId}")
    public Result<List<TaskDetailVO>> record(@PathVariable("processInstanceId") String processInstanceId){
        LambdaQueryWrapper<ProcessComments> lambdaQueryWrapper  =new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ProcessComments::getInstanceId,processInstanceId);
        List<ProcessComments> processCommentsList = processCommentsService.list(lambdaQueryWrapper);


        Map<Long, List<ProcessComments>> commentsMap = processCommentsList.stream()
                .collect(Collectors.groupingBy(ProcessComments::getTaskId));


        LambdaQueryWrapper<ProcessAttachments> processAttachmentsLambdaQueryWrapper  =new LambdaQueryWrapper<>();
        processAttachmentsLambdaQueryWrapper.eq(ProcessAttachments::getInstanceId,processInstanceId);
        List<ProcessAttachments> processAttachments = processAttachmentsService.list(processAttachmentsLambdaQueryWrapper);


        Map<Long, List<ProcessAttachments>> attachmentMap = processAttachments.stream()
                .collect(Collectors.groupingBy(ProcessAttachments::getTaskId));

        LambdaQueryWrapper<FlwHisTask> flwHisTaskLambdaQueryWrapper= new LambdaQueryWrapper<>();
        flwHisTaskLambdaQueryWrapper.eq(FlwHisTask::getInstanceId,processInstanceId);
        List<FlwHisTask> list = flwHisTaskMapper.selectList(flwHisTaskLambdaQueryWrapper);

        List<TaskDetailVO> taskDetailVOS= new ArrayList<>();
        for (FlwHisTask historicActivityInstance : list) {
            if("root".equals(historicActivityInstance.getTaskNodeId())){
                TaskDetailVO taskDetailVO= new TaskDetailVO();
                taskDetailVO.setTaskId(historicActivityInstance.getId()+"");
                taskDetailVO.setActivityId(historicActivityInstance.getTaskNodeId());
                taskDetailVO.setName("流程开始");
                taskDetailVO.setCreateTime(historicActivityInstance.getCreateTime());
                taskDetailVO.setEndTime(historicActivityInstance.getFinishTime());
                taskDetailVOS.add(taskDetailVO);

//                taskDetailVO.setSignImage();
//                taskDetailVO.setAttachmentVOList();
//                taskDetailVO.setOptionVOList();
//                taskDetailVO.setCommentVOList();
            }
//            else if("endEvent".equals(historicActivityInstance.getActivityType())){
//                TaskDetailVO taskDetailVO= new TaskDetailVO();
//                taskDetailVO.setTaskId(historicActivityInstance.getTaskId());
//                taskDetailVO.setActivityId(historicActivityInstance.getActivityId());
//                taskDetailVO.setName("流程结束");
//                taskDetailVO.setCreateTime(historicActivityInstance.getStartTime());
//                taskDetailVO.setEndTime(historicActivityInstance.getEndTime());
//                taskDetailVOS.add(taskDetailVO);
//            }
            else {
                List<ProcessComments> comments = commentsMap.get(historicActivityInstance.getId());
                if(CollUtil.isNotEmpty(comments)){
                    for (ProcessComments comment : comments) {
                        if(OPINION_COMMENT.equals(comment.getTypes())){
                            TaskDetailVO taskDetailVO= new TaskDetailVO();
                            taskDetailVO.setTaskId(historicActivityInstance.getId()+"");
                            taskDetailVO.setActivityId(historicActivityInstance.getTaskNodeId());
                            taskDetailVO.setName(historicActivityInstance.getTaskName());
                            taskDetailVO.setCreateTime(historicActivityInstance.getCreateTime());
                            taskDetailVO.setEndTime(historicActivityInstance.getFinishTime());
                            taskDetailVO.setComment(comment.getMessage());
                            List<ProcessAttachments> attachments = attachmentMap.get(historicActivityInstance.getId());
                            List<AttachmentVO> attachmentVOList = new ArrayList<>();
                            for (ProcessAttachments attachment : attachments) {
                                AttachmentVO attachmentVO = new AttachmentVO();
                                attachmentVO.setId(attachment.getId());
                                attachmentVO.setName(attachment.getFileName());
                                attachmentVO.setUrl(attachment.getUrl());
                                attachmentVOList.add(attachmentVO);
                            }

                            for (ProcessComments comment1 : comments) {
                                if(SIGN_COMMENT.equals(comment1.getTypes())){
                                    taskDetailVO.setSignImage(comment1.getMessage());
                                }
                            }

                            taskDetailVO.setAttachmentVOList(attachmentVOList);
                            taskDetailVOS.add(taskDetailVO);
                        }
                    }
                }
            }
        }
        return Result.OK(taskDetailVOS);
    }


    @ApiOperation("查看抄送")
    @PostMapping("process/ccList")
    public Result<Page<TaskVO>> ccList(@RequestBody TaskDTO taskDTO){
        LambdaQueryWrapper<FlwTaskCc> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(FlwTaskCc::getActorId,taskDTO.getCurrentUserInfo().getId());
        Page page= new Page();
        page.setCurrent(taskDTO.getPageNo());
        page.setSize(taskDTO.getPageSize());
        List<FlwTaskCc> ccList = flwTaskCcMapper.selectList(lambdaQueryWrapper);
        page.setTotal(ccList.size());
        if(CollUtil.isNotEmpty(ccList)){
            Set<String> processInstanceIds= new HashSet<>();
            for (FlwTaskCc cc : ccList) {
                processInstanceIds.add(cc.getInstanceId()+"");
            }
            List<FlwHisInstance> processInstanceList = flwHisInstanceMapper.selectBatchIds(processInstanceIds);
            Map<String,FlwHisInstance> map =new HashMap<>();
            for (FlwHisInstance historicProcessInstance : processInstanceList) {
                map.put(historicProcessInstance.getId()+"",historicProcessInstance);
            }

            List<String> applyUserIds= new ArrayList<>();
            for (FlwHisInstance historicProcessInstance : processInstanceList) {
                Map<String, Object> processVariables = historicProcessInstance.getVariableMap();
                String id = JSONObject.parseObject(MapUtil.getStr(processVariables, START_USER_INFO), new TypeReference<UserInfo>() {
                }).getId();
                applyUserIds.add(id);
            }
            Map<Long, Users> collect=new HashMap<>();
            if(CollUtil.isNotEmpty(applyUserIds)){
                LambdaQueryWrapper<Users> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
                userLambdaQueryWrapper.in(Users::getUserId,applyUserIds);
                List<Users> list = userService.list(userLambdaQueryWrapper);
                collect = list.stream().collect(Collectors.toMap(Users::getUserId, Function.identity()));
            }


            List<CCVO> ccvos=new ArrayList<>();
            for (FlwTaskCc cc : ccList) {
                CCVO ccvo = new CCVO();
                FlwHisInstance historicProcessInstance = map.get(cc.getInstanceId());
                Map<String, Object> processVariables = historicProcessInstance.getVariableMap();
                ccvo.setProcessInstanceId(historicProcessInstance.getId()+"");
                String name = processService.getProcessById(historicProcessInstance.getProcessId()).getProcessModel().getName();
                ccvo.setProcessDefinitionName(name);
                ccvo.setStartUser(JSONObject.parseObject(MapUtil.getStr(processVariables,START_USER_INFO),new TypeReference<UserInfo>(){}));
                ccvo.setUsers(collect.get(Long.valueOf(ccvo.getStartUser().getId())));
                ccvo.setStartTime(historicProcessInstance.getCreateTime());
                ccvo.setEndTime(historicProcessInstance.getEndTime());
                Boolean flag= historicProcessInstance.getEndTime() != null;
                ccvo.setCurrentActivityName(getCurrentName(historicProcessInstance.getId()+"",flag));
                ccvo.setBusinessStatus(MapUtil.getStr(processVariables,PROCESS_STATUS));


                long totalTimes = historicProcessInstance.getEndTime()==null?
                        (Calendar.getInstance().getTimeInMillis()-historicProcessInstance.getCreateTime().getTime()):
                        (historicProcessInstance.getEndTime().getTime()-historicProcessInstance.getCreateTime().getTime());
                long dayCount = totalTimes /(1000*60*60*24);//计算天
                long restTimes = totalTimes %(1000*60*60*24);//剩下的时间用于计于小时
                long hourCount = restTimes/(1000*60*60);//小时
                restTimes = restTimes % (1000*60*60);
                long minuteCount = restTimes / (1000*60);

                String spendTimes = dayCount+"天"+hourCount+"小时"+minuteCount+"分";
                ccvo.setDuration(spendTimes);
                ccvos.add(ccvo);
            }
            page.setRecords(ccvos);

        }
        return Result.OK(page);
    }

  /*






























    @ApiOperation("通过流程实例查看审批记录")
    @PostMapping("process/record/{processInstanceId}")
    public Result<List<TaskDetailVO>> record(@PathVariable("processInstanceId") String processInstanceId){
        List<Comment> processInstanceComments = taskService.getProcessInstanceComments(processInstanceId);
        Map<String, List<Comment>> commentsMap = processInstanceComments.stream()
                .collect(Collectors.groupingBy(Comment::getTaskId));
        List<Attachment> processInstanceAttachments = taskService.getProcessInstanceAttachments(processInstanceId);
        Map<String, List<Attachment>> attachmentMap = processInstanceAttachments.stream()
                .collect(Collectors.groupingBy(Attachment::getTaskId));
        List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).orderByHistoricActivityInstanceStartTime().asc().list();
        List<TaskDetailVO> taskDetailVOS= new ArrayList<>();
        for (HistoricActivityInstance historicActivityInstance : list) {
            if("startEvent".equals(historicActivityInstance.getActivityType())){
                TaskDetailVO taskDetailVO= new TaskDetailVO();
                taskDetailVO.setTaskId(historicActivityInstance.getTaskId());
                taskDetailVO.setActivityId(historicActivityInstance.getActivityId());
                taskDetailVO.setName("流程开始");
                taskDetailVO.setCreateTime(historicActivityInstance.getStartTime());
                taskDetailVO.setEndTime(historicActivityInstance.getEndTime());
                taskDetailVOS.add(taskDetailVO);

//                taskDetailVO.setSignImage();
//                taskDetailVO.setAttachmentVOList();
//                taskDetailVO.setOptionVOList();
//                taskDetailVO.setCommentVOList();
            }
            else if("endEvent".equals(historicActivityInstance.getActivityType())){
                TaskDetailVO taskDetailVO= new TaskDetailVO();
                taskDetailVO.setTaskId(historicActivityInstance.getTaskId());
                taskDetailVO.setActivityId(historicActivityInstance.getActivityId());
                taskDetailVO.setName("流程结束");
                taskDetailVO.setCreateTime(historicActivityInstance.getStartTime());
                taskDetailVO.setEndTime(historicActivityInstance.getEndTime());
                taskDetailVOS.add(taskDetailVO);
            }
            else if("userTask".equals(historicActivityInstance.getActivityType())){
                List<Comment> comments = commentsMap.get(historicActivityInstance.getTaskId());
                if(CollUtil.isNotEmpty(comments)){
                    for (Comment comment : comments) {
                        if(OPINION_COMMENT.equals(comment.getType())){
                            TaskDetailVO taskDetailVO= new TaskDetailVO();
                            taskDetailVO.setTaskId(historicActivityInstance.getTaskId());
                            taskDetailVO.setActivityId(historicActivityInstance.getActivityId());
                            taskDetailVO.setName(historicActivityInstance.getActivityName());
                            taskDetailVO.setCreateTime(historicActivityInstance.getStartTime());
                            taskDetailVO.setEndTime(historicActivityInstance.getEndTime());
                            taskDetailVO.setComment(comment.getFullMessage());
                            List<Attachment> attachments = attachmentMap.get(historicActivityInstance.getTaskId());
                            List<AttachmentVO> attachmentVOList = new ArrayList<>();
                            for (Attachment attachment : attachments) {
                                AttachmentVO attachmentVO = new AttachmentVO();
                                attachmentVO.setId(attachment.getId());
                                attachmentVO.setName(attachment.getName());
                                attachmentVO.setUrl(attachment.getUrl());
                                attachmentVOList.add(attachmentVO);
                            }

                            for (Comment comment1 : comments) {
                                if(SIGN_COMMENT.equals(comment1.getType())){
                                    taskDetailVO.setSignImage(comment1.getFullMessage());
                                }
                            }

                            taskDetailVO.setAttachmentVOList(attachmentVOList);
                            taskDetailVOS.add(taskDetailVO);
                        }
                    }
                }
            }
        }
        return Result.OK(taskDetailVOS);
    }

    @ApiOperation("通过流程实例id查看详情")
    @PostMapping("process/instanceInfo")
    public Result<HandleDataVO> instanceInfo(@RequestBody HandleDataDTO HandleDataDTO){
        String processInstanceId = HandleDataDTO.getProcessInstanceId();
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId)
                .includeProcessVariables().singleResult();
        String processDefinitionKey = historicProcessInstance.getProcessDefinitionKey();
        ProcessTemplates processTemplates = processTemplateService.getById(processDefinitionKey.replace(PROCESS_PREFIX,""));
        processTemplates.setLogo(processTemplates.getIcon());
        processTemplates.setFormId(processTemplates.getTemplateId());
        processTemplates.setFormName(processTemplates.getTemplateName());
        processTemplates.setProcessDefinitionId(historicProcessInstance.getProcessDefinitionId());

        HandleDataVO handleDataVO =new HandleDataVO();
        Map<String, Object> processVariables = historicProcessInstance.getProcessVariables();

        handleDataVO.setProcessInstanceId(historicProcessInstance.getId());
        JSONObject jsonObject = (JSONObject) processVariables.get(FORM_VAR);
        handleDataVO.setFormData(jsonObject);
        String process = processTemplates.getProcess();
        ChildNode childNode = JSONObject.parseObject(process, new TypeReference<ChildNode>(){});
        SettingsInfo settingsInfo = JSONObject.parseObject(processTemplates.getSettings(), new TypeReference<SettingsInfo>() {});
        Boolean sign = settingsInfo.getSign();
        ChildNode currentNode=null;
        if(StringUtils.isNotBlank(HandleDataDTO.getTaskId())){
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(HandleDataDTO.getTaskId()).singleResult();
            currentNode = getChildNode(childNode, historicTaskInstance.getTaskDefinitionKey());
            List<FormOperates> formPerms = currentNode.getProps().getFormPerms();
            if(CollUtil.isNotEmpty(formPerms)){
                Iterator<FormOperates> iterator = formPerms.iterator();
                while (iterator.hasNext()){
                    FormOperates next = iterator.next();
                    if("H".equals(next.getPerm())){
//                        iterator.remove();
                        if(jsonObject!=null){
                            jsonObject.remove(next.getId());
                        }
                    }
                }
            }
            handleDataVO.setCurrentNode(currentNode);
            handleDataVO.setTaskId(HandleDataDTO.getTaskId());
        }

        if(sign){
            handleDataVO.setSignFlag(true);
        }
        else{
        if(StringUtils.isNotBlank(HandleDataDTO.getTaskId())){
            if(currentNode!=null){
                if(currentNode.getProps().getSign()){
                    handleDataVO.setSignFlag(true);
                }
                else{
                    handleDataVO.setSignFlag(false);
                }
            }
        }
        else {
            handleDataVO.setSignFlag(false);
        }
        }




        List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery().processInstanceId(historicProcessInstance.getId()).list();
        Map<String,List<HistoricActivityInstance>> historicActivityInstanceMap =new HashMap<>();
        for (HistoricActivityInstance historicActivityInstance : list) {
            List<HistoricActivityInstance> historicActivityInstances = historicActivityInstanceMap.get(historicActivityInstance.getActivityId());
            if(historicActivityInstances==null){
             historicActivityInstances =new ArrayList<>();
             historicActivityInstances.add(historicActivityInstance);
             historicActivityInstanceMap.put(historicActivityInstance.getActivityId(),historicActivityInstances);
            }
            else{
                historicActivityInstances.add(historicActivityInstance);
                historicActivityInstanceMap.put(historicActivityInstance.getActivityId(),historicActivityInstances);
            }
        }

        Process mainProcess = repositoryService.getBpmnModel(historicProcessInstance.getProcessDefinitionId()).getMainProcess();
        Collection<FlowElement> flowElements = mainProcess.getFlowElements();

        List<String> runningList= new ArrayList<>();
        handleDataVO.setRunningList(runningList);
        List<String> endList=new ArrayList<>();
        handleDataVO.setEndList(endList);
        List<String> noTakeList=new ArrayList<>();
        handleDataVO.setNoTakeList(noTakeList);
        Map<String,List<TaskDetailVO>> deatailMap =new HashMap<>();
        List<Comment> processInstanceComments = taskService.getProcessInstanceComments(historicProcessInstance.getId());
        List<Attachment> processInstanceAttachments = taskService.getProcessInstanceAttachments(historicProcessInstance.getId());
        for (FlowElement flowElement : flowElements) {
            List<TaskDetailVO> detailVOList =new ArrayList<>();
            List<HistoricActivityInstance> historicActivityInstanceList = historicActivityInstanceMap.get(flowElement.getId());
            if(CollUtil.isNotEmpty(historicActivityInstanceList)){
                for (HistoricActivityInstance historicActivityInstance : historicActivityInstanceList) {
                    if(historicActivityInstance.getEndTime()!=null){
                        if("startEvent".equalsIgnoreCase(historicActivityInstance.getActivityType()) ||"endEvent".equalsIgnoreCase(historicActivityInstance.getActivityType())){
                            TaskDetailVO taskDetailVO = new TaskDetailVO();
                            taskDetailVO.setActivityId(historicActivityInstance.getActivityId());
                            taskDetailVO.setName(historicActivityInstance.getActivityName());
                            taskDetailVO.setCreateTime(historicActivityInstance.getStartTime());
                            taskDetailVO.setEndTime(historicActivityInstance.getEndTime());
                            detailVOList.add(taskDetailVO);
                            deatailMap.put(historicActivityInstance.getActivityId(),detailVOList);
                            endList.add(historicActivityInstance.getActivityId());
                        }
                        else if ("userTask".equalsIgnoreCase(historicActivityInstance.getActivityType())){
                            List<TaskDetailVO> voList = deatailMap.get(historicActivityInstance.getActivityId());
                            List<HistoricActivityInstance> activityInstanceList = list.stream().filter(h -> h.getActivityId().equals(historicActivityInstance.getActivityId()) &&h.getEndTime()!=null).collect(Collectors.toList());
                            if(voList!=null){
                                collectUserTaskInfo(processInstanceComments, processInstanceAttachments, historicActivityInstance, voList, activityInstanceList);
                            }
                            else{
                                voList=new ArrayList<>();
                                collectUserTaskInfo(processInstanceComments, processInstanceAttachments, historicActivityInstance, voList, activityInstanceList);
                            }
                            deatailMap.put(historicActivityInstance.getActivityId(),voList);
                            endList.add(historicActivityInstance.getActivityId());
                        }
                        else if("serviceTask".equalsIgnoreCase(historicActivityInstance.getActivityType())){

                        }
                    }
                    else{
                        if ("userTask".equalsIgnoreCase(historicActivityInstance.getActivityType())){
                            List<TaskDetailVO> voList = deatailMap.get(historicActivityInstance.getActivityId());
                            List<HistoricActivityInstance> activityInstanceList = list.stream().filter(h -> h.getActivityId().equals(historicActivityInstance.getActivityId()) &&h.getEndTime()==null).collect(Collectors.toList());
                            if(voList!=null){
                                collectUserTaskInfo(processInstanceComments, processInstanceAttachments, historicActivityInstance, voList, activityInstanceList);
                            }
                            else{
                                voList=new ArrayList<>();
                                collectUserTaskInfo(processInstanceComments, processInstanceAttachments, historicActivityInstance, voList, activityInstanceList);
                            }
                            deatailMap.put(historicActivityInstance.getActivityId(),voList);
                            if(endList.contains(historicActivityInstance.getActivityId())){
                                endList.remove(historicActivityInstance.getActivityId());
                                runningList.add(historicActivityInstance.getActivityId());
                            }
                            else{
                                runningList.add(historicActivityInstance.getActivityId());
                            }
                        }
                        else if("serviceTask".equalsIgnoreCase(historicActivityInstance.getActivityType())){

                        }
                    }
                }
            }
            else{
                noTakeList.add(flowElement.getId());
            }
        }
        handleDataVO.setProcessTemplates(processTemplates);
        handleDataVO.setDetailVOList(deatailMap);
        return Result.OK(handleDataVO);
    }

    private void collectUserTaskInfo(List<Comment> processInstanceComments,
                                     List<Attachment> processInstanceAttachments,
                                     HistoricActivityInstance historicActivityInstance,
                                     List<TaskDetailVO> voList,
                                     List<HistoricActivityInstance> activityInstanceList) {
        for (HistoricActivityInstance activityInstance : activityInstanceList) {
            TaskDetailVO taskDetailVO =new TaskDetailVO();
            taskDetailVO.setTaskId(activityInstance.getTaskId());
            taskDetailVO.setActivityId(activityInstance.getActivityId());
            taskDetailVO.setName(activityInstance.getActivityName());
            taskDetailVO.setCreateTime(activityInstance.getStartTime());
            taskDetailVO.setEndTime(activityInstance.getEndTime());
            Comment signComment = processInstanceComments.stream().filter(h -> h.getTaskId().equals(historicActivityInstance.getTaskId()) && h.getType().equals(SIGN_COMMENT)).findFirst().orElse(null);
            if(signComment!=null){
                taskDetailVO.setSignImage(signComment.getFullMessage());
            }
            List<Attachment> attachments = processInstanceAttachments.stream().filter(h -> h.getTaskId().equals(historicActivityInstance.getTaskId())).collect(Collectors.toList());
            if(CollUtil.isNotEmpty(attachments)){
                List<AttachmentVO> attachmentVOList = new ArrayList<>();
                for (Attachment attachment : attachments) {
                    AttachmentVO attachmentVO = new AttachmentVO();
                    attachmentVO.setId(attachment.getId());
                    attachmentVO.setName(attachment.getName());
                    attachmentVO.setUrl(attachment.getUrl());
                    attachmentVOList.add(attachmentVO);
                }
                taskDetailVO.setAttachmentVOList(attachmentVOList);
            }

            List<Comment> options = processInstanceComments.stream().filter(h -> h.getTaskId().equals(historicActivityInstance.getTaskId()) && h.getType().equals(OPINION_COMMENT)).collect(Collectors.toList());
            if(CollUtil.isNotEmpty(options)){
                List<OptionVO> optionVOList =new ArrayList<>();
                for (Comment option : options) {
                    OptionVO optionVO = new OptionVO();
                    optionVO.setComments(option.getFullMessage());
                    optionVO.setUserId(option.getUserId());
//                                        optionVO.setUserName();
                    optionVO.setCreateTime(option.getTime());
                    optionVOList.add(optionVO);
                }
                taskDetailVO.setOptionVOList(optionVOList);
            }

            List<Comment> comments = processInstanceComments.stream().filter(h -> h.getTaskId().equals(historicActivityInstance.getTaskId()) && h.getType().equals(COMMENTS_COMMENT)).collect(Collectors.toList());
            if(CollUtil.isNotEmpty(comments)){
                List<CommentVO> commentsVOList =new ArrayList<>();
                for (Comment comment : comments) {
                    CommentVO commentVO = new CommentVO();
                    commentVO.setComments(comment.getFullMessage());
                    commentVO.setUserId(comment.getUserId());
//                                        commentVO.setUserName();
                    commentVO.setCreateTime(comment.getTime());
                    commentsVOList.add(commentVO);
                }
                taskDetailVO.setCommentVOList(commentsVOList);
            }

            voList.add(taskDetailVO);



        }
    }



    @ApiOperation("上传文件")
    @PostMapping("upLoadFile")
    public Result<AttachmentVO> upLoadFile(@RequestParam("file") MultipartFile file, HttpServletRequest request){
        MinioUploadUtil minioUploadUtil = SpringContextHolder.getBean(MinioUploadUtil.class);
        MinioClient minioClient = SpringContextHolder.getBean(MinioClient.class);
        AttachmentVO attachmentVO= new AttachmentVO();
        attachmentVO.setName(file.getOriginalFilename());
        minioUploadUtil.uploadFile(file, "java", file.getOriginalFilename());
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket("java")
                            .object(file.getOriginalFilename())
                            .expiry(7, TimeUnit.DAYS) // 设置5秒的超时时间。
                            .method(Method.GET)
                            .build());
            attachmentVO.setUrl(url);
        } catch (ErrorResponseException e) {
            e.printStackTrace();
        } catch (InsufficientDataException e) {
            e.printStackTrace();
        } catch (InternalException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidResponseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (XmlParserException e) {
            e.printStackTrace();
        } catch (ServerException e) {
            e.printStackTrace();
        }
        return Result.OK(attachmentVO);
    }
    @ApiOperation("下载文件")
    @PostMapping("downLoadFile")
    public void downLoadFile(@RequestParam("name") String name){
        MinioUploadUtil minioUploadUtil = SpringContextHolder.getBean(MinioUploadUtil.class);
        minioUploadUtil.downFile(name,"java",name);
    }
*/
}
