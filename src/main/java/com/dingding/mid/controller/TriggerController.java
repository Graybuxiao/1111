package com.dingding.mid.controller;

import com.dingding.mid.common.Result;
import com.dingding.mid.entity.ProcessTemplates;
import com.dingding.mid.exception.WorkFlowException;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.xiaoymin.knife4j.annotations.ApiSort;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static com.dingding.mid.common.WorkFlowConstants.PROCESS_PREFIX;

/**
 * @author Doctor4JavaEE
 * @since 2023/6/5
 */
@RestController
@RequestMapping("/trigger")
@Api(tags = {"用来演示触发器节点的被调用"})
@ApiSort(4)
@Slf4j
public class TriggerController {

    @ApiOperation("Get请求调用")
    @ApiOperationSupport(order = 1)
    @GetMapping("getRequest")
    public Result getRequest(HttpServletRequest request) throws IOException {
        Map<String, String> headerMap = new HashMap<>();
        Enumeration<String> enumeration = request.getHeaderNames();
        while (enumeration.hasMoreElements()) {
            String name	= enumeration.nextElement();
            String value = request.getHeader(name);
            headerMap.put(name, value);
        }
        log.error("请求的Headers:为{}",headerMap);
        Map<String, String> parameterMap = new HashMap<>();
        BufferedReader br = request.getReader();

        String str, wholeStr = "";
        while((str = br.readLine()) != null){
            wholeStr += str;
        }

        log.error("请求的Body:为{}",wholeStr);

        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name	= parameterNames.nextElement();
            String value = request.getParameter(name);
            parameterMap.put(name, value);
        }
        log.error("请求的path参数为:为{}",parameterMap);

        return Result.OK();
    }

    @ApiOperation("Post请求调用")
    @ApiOperationSupport(order = 1)
    @PostMapping("postRequest")
    public Result postRequest(HttpServletRequest request) throws IOException {
        Map<String, String> headerMap = new HashMap<>();
        Enumeration<String> enumeration = request.getHeaderNames();
        while (enumeration.hasMoreElements()) {
            String name	= enumeration.nextElement();
            String value = request.getHeader(name);
            headerMap.put(name, value);
        }
        log.error("请求的Headers:为{}",headerMap);

        BufferedReader br = request.getReader();

        String str, wholeStr = "";
        while((str = br.readLine()) != null){
            wholeStr += str;
        }

        log.error("请求的Body:为{}",wholeStr);


        return Result.OK();
    }
}
