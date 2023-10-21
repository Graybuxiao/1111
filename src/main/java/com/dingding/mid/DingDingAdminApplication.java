package com.dingding.mid;
import com.dingding.mid.flowlong.QueryService;
import java.util.List;
import com.dingding.mid.flowlong.RuntimeService;
import com.dingding.mid.flowlong.ProcessService;
import com.dingding.mid.flowlong.FlowLongInterceptor;
import com.dingding.mid.flowlong.TaskAccessStrategy;
import com.dingding.mid.flowlong.Expression;

import java.io.*;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.dingding.mid.flowlong.FlowLongEngine;
import com.dingding.mid.flowlong.TaskService;
import com.dingding.mid.flowlong.core.FlowLongContext;
import com.dingding.mid.utils.MinioUploadUtil;
import com.dingding.mid.utils.SpringContextHolder;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.Cleanup;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;


/**
 */
@SpringBootApplication(scanBasePackages = "com")
public class DingDingAdminApplication extends SpringBootServletInitializer implements
    CommandLineRunner {

    public static void main(String[] args)  {
        SpringApplication springApplication = new SpringApplication(DingDingAdminApplication.class);
        springApplication.run(args);
        System.out.println("DingDing启动完成");
        FlowLongEngine flowLongEngine = SpringContextHolder.getBean(FlowLongEngine.class);
        FlowLongContext context = flowLongEngine.getContext();

        ProcessService processService = context.getProcessService();
        QueryService queryService = context.getQueryService();
        RuntimeService runtimeService = context.getRuntimeService();
        TaskService taskService = context.getTaskService();
        Expression expression = context.getExpression();
        List<FlowLongInterceptor> interceptors = context.getInterceptors();
        TaskAccessStrategy taskAccessStrategy = context.getTaskAccessStrategy();
    }


    @Override
    public void run(String... args) throws Exception {

    }



}
