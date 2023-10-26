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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.dingding.mid.flowlong.FlowLongEngine;
import com.dingding.mid.flowlong.TaskService;
import com.dingding.mid.flowlong.core.FlowLongContext;
import com.dingding.mid.utils.MinioUploadUtil;
import com.dingding.mid.utils.SpringContextHolder;
import lombok.Cleanup;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;


/**
 */
@SpringBootApplication(scanBasePackages = "com")
public class DingDingAdminApplication extends SpringBootServletInitializer implements
    CommandLineRunner {

    public static void main(String[] args)  {
        SpringApplication springApplication = new SpringApplication(DingDingAdminApplication.class);
        springApplication.run(args);
        System.out.println("DingDing启动完成");
        //
        System.err.println("----验证源码集成FlowLong------------------");
        FlowLongEngine flowLongEngine = SpringContextHolder.getBean(FlowLongEngine.class);
        FlowLongContext flowLongEngineContext = flowLongEngine.getContext();

        ProcessService processService = flowLongEngineContext.getProcessService();
        QueryService queryService = flowLongEngineContext.getQueryService();
        RuntimeService runtimeService = flowLongEngineContext.getRuntimeService();
        TaskService taskService = flowLongEngineContext.getTaskService();
        Expression expression = flowLongEngineContext.getExpression();
        List<FlowLongInterceptor> interceptors = flowLongEngineContext.getInterceptors();
        TaskAccessStrategy taskAccessStrategy = flowLongEngineContext.getTaskAccessStrategy();
        System.err.println("----验证源码集成FlowLong成功------------------");
        ExpressionParser parser = new SpelExpressionParser();
        EvaluationContext context = new StandardEvaluationContext();
//        for (Map.Entry<String, Object> entry : args.entr) {
//            context.setVariable(entry.getKey(), entry.getValue());
//        }
        context.setVariable("rootEx","张三");

        String expr="T(com.dingding.mid.utils.ExUtils).strEqualsMethod(#rootEx,'张三')";
        Boolean value = parser.parseExpression(expr).getValue(context, Boolean.class);
        System.err.println(value);
    }


    @Override
    public void run(String... args) throws Exception {

    }



}
