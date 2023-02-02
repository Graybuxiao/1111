package com.dingding.mid;

import cn.hutool.core.io.FileUtil;
import com.dingding.mid.utils.SpringContextHolder;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import java.io.File;


/**
 */
@SpringBootApplication(scanBasePackages = "com")
public class DingDingAdminApplication extends SpringBootServletInitializer implements
    CommandLineRunner {

    public static void main(String[] args)  {
        SpringApplication springApplication = new SpringApplication(DingDingAdminApplication.class);
        springApplication.run(args);
        System.out.println("DingDing启动完成");


        RepositoryService repositoryService = SpringContextHolder.getBean(RepositoryService.class);
        byte[] bytes = FileUtil.readBytes(new File("D:\\05_CodeWorkSpace\\04_OpenWorkSpace\\01_DingDingMid\\dingding-mid-business-java\\src\\main\\resources\\LeaveProcess.bpmn20.xml"));
        Deployment deploy = repositoryService.createDeployment().addBytes("测试.bpmn", bytes).deploy();
        System.err.println(deploy);
    }


    @Override
    public void run(String... args) throws Exception {

    }



}
