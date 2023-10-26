# dingding-mid     中国式传统流程引擎开源标杆(Activiti 567 Flowable 56,Camunda7 )

### 大家可扫码加入交流群， 如果二维码失效了，可以加我微信 Doctor4JavaEE  备注 钉钉 拉你入群
<table>
  <tr>
    <td><img src="https://pro.cxygzl.com/wj//2023-09-18/36a3f20a346646cc87b32abc3c44d63d-918.jpg" alt="微信群" style="zoom:20%;" /></td>
    <td><img src="https://jeecgdev.oss-cn-beijing.aliyuncs.com/upload/%E5%BE%AE%E4%BF%A1_1665560718233.png" alt="作者微信" style="zoom:20%;" /></td>
  </tr>
</table> 

### 史上最全工作流社区文档 http://123.249.74.161:1024/
### 演示地址: http://123.249.74.161/ 







## 👀开源版界面一览

###  **工作区面板** 

<img src="https://images.gitee.com/uploads/images/2020/1005/140253_39e3f2d5_4928216.png" alt="输入图片说明" title="屏幕截图.png" style="zoom: 50%;" />

<img src="https://images.gitee.com/uploads/images/2020/1005/140329_89cd5aac_4928216.png" alt="输入图片说明" title="屏幕截图.png" style="zoom:50%;" />



### 表单管理

 **工作流表单管理，支持分组和单组表单拖拽排序** 

<img src="https://images.gitee.com/uploads/images/2020/1005/140358_17fc6838_4928216.png" alt="输入图片说明" title="屏幕截图.png" style="zoom:50%;" />

<img src="https://images.gitee.com/uploads/images/2020/1005/140502_bdc2ea04_4928216.png" alt="输入图片说明" title="屏幕截图.png" style="zoom:50%;" />


---------

####  **表单基本设置** 

<img src="https://images.gitee.com/uploads/images/2020/1005/140559_5c51a89b_4928216.png" alt="输入图片说明" title="屏幕截图.png" style="zoom: 50%;" />




--------

####  **表单设计器**

>  支持分栏布局、明细表格、以及多种基础组件，支持自定义开发组件

![image-20220724220114724](https://pic.rmb.bdstatic.com/bjh/b0f1ed22d61ea86b4222b89dbea6ecd1.png)

![image-20220724221040780](https://pic.rmb.bdstatic.com/bjh/73e71e1323812a57802a76beffe27906.png)






---------

 #### 流程设计器

> 任意条件层级审批流程设计， 审批节点支持多种业务类型设置，支持流程校验

![image-20220711111351476](https://pic.rmb.bdstatic.com/bjh/3300dbc60218a8376b45ed6ed46e8162.png)



**自定义审批条件**

![image-20220722182318650](https://pic.rmb.bdstatic.com/bjh/4599e414142004f3b0445e478018b8be.png)


---------

**自定义复杂流转条件**

> 可视化流程逻辑分支条件

![image-20220722182622661](https://pic.rmb.bdstatic.com/bjh/299989bb8b256beae152a29ea611b790.png)

---------



 **支持多种类型业务节点，支持配置校验，灵活配置** 

<img src="https://pic.rmb.bdstatic.com/bjh/e35d8375eae56b4b9bbace88ee2a00fd.png" alt="image-20220722182427315" style="zoom:50%;" />

**支持无限层级嵌套**

![image-20220711111911427](https://pic.rmb.bdstatic.com/bjh/02cd8936e081bdd053bfa695826817ba.png)

**自动校验设置项，列出所有错误提示**

<img src="https://pic.rmb.bdstatic.com/bjh/ddd20cd54d9502f8eec59565864dfb2a.png" alt="image-20220731215022817" style="zoom:50%;" />

**条件节点优先级动态拖拽，实时刷新**

<img src="https://images.gitee.com/uploads/images/2021/0416/200127_a59216a1_4928216.png" alt="输入图片说明" title="屏幕截图.png" style="zoom:50%;" />



## ✍开发










> 特别说明：源码、JDK、MySQL、Redis等存放路径禁止包含中文、空格、特殊字符等

## 环境要求

> 官方建议： JDK版本不低于 `1.8.0_281`版本，可使用`OpenJDK 8`、`Alibaba Dragonwell 8`、`BiShengJDK 8`

项目  | 推荐版本                              | 说明
-----|-----------------------------------| -------------
JDK  | 1.8.0_281            | JAVA环境依赖(需配置环境变量)
Maven  | 3.6.3                             | 项目构建(需配置环境变量)
Redis  | 3.2.100(Windows)/6.0.x(Linux,Mac) |
MySQL  | 5.7.x+                            | 数据库任选一(默认)
SQLServer  | 2012+                             | 数据库任选一
Oracle  | 11g+                              | 数据库任选一
PostgreSQL  | 12+                               | 数据库任选一

## 工具推荐
> 为防止无法正常下载Maven以来，请使用以下IDE版本

IDEA版本  | Maven版本
-----|-------- | 
IDEA2020及以上版本  | Maven 3.6.3及以上版本 |

## IDEA插件

- `Lombok`
- `Alibaba Java Coding Guidelines`
- `MybatisX`


## 环境配置
- 打开`dingding-mid\src\main\resources\application.yml`

> 环境变量
> - dev  开发环境
> - test  测试环境
> - preview 预发布环境
> - pro 生产环境

``` yml
  #环境 dev|test|pro|preview
  profiles:
  active: dev
```

- 打开`application-x.yml`(`x`表示环境变量),需配置以下
  - 服务端口(`port`)
  - 数据库连接
  - Redis

## 启动项目
- `dingding-mid\src\main\java\com\dingding\mid\DingDingAdminApplication.java`，右击运行即可。

### 项目发布

- 在`IDEA`右侧`Maven`-`dingding-mid(root)`-`Lifecycle`中双击`clean`清理下项目
- 双击`package`打包项目
- 打开项目目录，依次打开`dingding-mid\target`，将`dingding-mid-{version}-RELEASE.jar`上传至服务器

### knife4j接口文档
- `http://ip:端口/doc.html


### 启动前端(因为是前后端分离的项目) (B站有配套视频讲解了 各个版本的前端以及后端如何启动)

- `Vue2 版本对应视频 :  https://www.bilibili.com/video/BV18D4y1C7qE/?spm_id_from=333.999.0.0`
- `Vue3 版本对应视频 :  https://www.bilibili.com/video/BV1aG4y1t7JQ/?spm_id_from=333.788&vd_source=fb655b691713324522e551b8acef3630`
- `AntDesignVue 版本对应视频 :  https://www.bilibili.com/video/BV1X84y1C7AD/?spm_id_from=333.788&vd_source=fb655b691713324522e551b8acef3630`
- `React 版本对应视频 :  https://www.bilibili.com/video/BV13Y411d7g6/?spm_id_from=333.788&vd_source=fb655b691713324522e551b8acef3630`
- `RuoYI-Vue-Camunda 版本对应视频 :  https://www.bilibili.com/video/BV1rv4y1L7wD/?spm_id_from=333.788&vd_source=fb655b691713324522e551b8acef3630`


