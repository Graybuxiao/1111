# 新官网地址:  http://101.42.224.59:35003/

# 本人在B站有录制各种工作流实操视频,大家请多多支持!

| 课程名  |课程链接   | 完结状态  |
|---|---|---|
| Camunda中国式流程课程B站全网首发 讲解Camunda项目实施中的重难点,如有需要:   | https://www.bilibili.com/cheese/play/ss30347?csource=private_space_class_null&spm_id_from=333.999.0.0  | 正在更新中  |
| Flowable中国式流程课程B站全网首发 讲解Flowable项目实施中的重难点,如有需要:   | 正在录制中  | 正在更新中  |
| Activiti中国式流程课程B站全网首发 讲解Activiti项目实施中的重难点,如有需要:   | 正在录制中  | 正在更新中  |
| 一种支持超高TPS的流程引擎性能架构以及流程引擎调优   | 正在录制中  | 正在更新中  |









# dingding-mid     中国式传统流程引擎开源标杆(Activiti 567 Flowable 56,Camunda7 )







### 大家可扫码加入交流群， 如果二维码失效了，可以加我微信 Doctor4JavaEE  备注 "异世相遇,尽享美味" 拉你入群  会附赠 "一套Flowable视频和Camunda视频供学习哦!"
| <img src="https://jeecgdev.oss-cn-beijing.aliyuncs.com/upload/%E5%BE%AE%E4%BF%A1_1665560718233.png" alt="作者微信" style="zoom:20%;" /> |
|-------------------------------------------------------------------------------------------------------------------------------------|




| 史上最全工作流文档  | http://101.42.224.59:35003/                           |
|------------|-------------------------------------------------------|
| 演示地址:      | 暂无                                |
| Vue3 演示地址: | 暂无                           |
| 超强商用版演示地址 | http://101.42.224.59:35000/  admin 123456 |
| 超高TPS-QPS性能工作流 | http://101.42.224.59:35003/blogs/tps/00_tps-qps.html |
| 单台普通PC服务器日支撑千万级业务流 | http://101.42.224.59:35003/blogs/tps/00_tps-qps.html |














  




### 使用到了如下开源项目
前端项目  
> - 1>https://gitee.com/willianfu/jw-workflow-engine 提供了前端源码 保留版权信息 Apache2.0协议(此是Vue2版本的)  
> - 2>https://gitee.com/crowncloud/smart-flow-design 提供了前端源码 保留版权信息 Apache2.0协议(此是AntDV2版本的)  
> - 3>https://github.com/StavinLi/Workflow-Vue3 提供了前端源码 保留版权信息 MIT协议(此是Vue3版本的)  
> - 4>https://github.com/cedrusweng/workflow-react 提供了前端源码 保留版权信息 无LICENSE文件,未对其进行二次开发,仅引用 保留版权信息 (此是React版本的)  
> - 5>https://github.com/wangzhenggui/dingding-approval-flow 提供了前端源码 保留版权信息 无LICENSE文件,未对其进行二次开发,仅引用 保留版权信息 (此是React版本的)
> - 6>https://github.com/miantj/workflow_vue3.git 提供了前端源码 保留版权信息 无LICENSE文件,未对其进行二次开发,仅引用 保留版权信息 (此是Vue3版本的)
后台项目  
> - 1>https://gitee.com/willianfu/jw-workflow-engine-server 提供了几个用户,部门表结构  Apache2.0协议  
个人  
> - 1>感谢如下个人    
    李 銍 lzgabel  lz19960321lz@gmail.com  (已给)
    于胜灵 yushengling@zhihuisystem.com  (已给)
    Flowable群小白菜 18877811997  (已给)
    
以上人员可以找我领取一套 本人珍藏的Flowable\Activiti\Camunda视频教程 感谢你们的付出!  





  
# 👀本项目商业计划(本项目是2022年初先发布的商业版,目前也发了开源版)
>  商业版地址   http://101.42.224.59:35000/  admin 123456
> - 0> 开源版和商业版不是一套东西 商业版用的也是仿钉钉流程设计器,功能更加强大,建议企业来采购,个人别来购买,用开源版就行,开源版也会持续更新滴!!!
> - 1> 开源是因为发现很多个人开发者需要这样的系统,以及国内暂无可用的后台接Flowable的,开源版也是有其商业价值的!!!!  


  




## 最后, 给学习流程引擎框架(Activiti567,Flowable56,Camunda7,Zeebe8)的Java开发人员几个建议
> - 1> 一定一定不要认为Activiti 没有提供对应的表查询的API ,那么就无法完成了   
      我们可以 把他的这些表 Mapper 都写出来, 自己查就可以了 ,但是要注意的是, 需要先看好Activiti的索引是怎么建的,  
      防止自己写导致索引失效,导致查询效率慢 (自己不要写更新SQL, 可以通过CMD 更新, 因为 他有乐观锁版本列  ,自己写的Mapper仅仅用于查询 推荐)  
> - 2> 一定一定要把他的执行SQL 打印出来, 这样才会知道该如何优化 ,因为工作流本身 比如很小的一个功能,     
      就至少会 操作 十几张表 比如start 工作流 ,所以一定要注意它的性能优化,对于SQL不强的人,可以在了解的Activiti的表结构之后 看一下他是如何进行多表联查的  
> - 3> 一定不要觉得,从数据库 中查询出数据 就代表 工作流 进阶了 , 像类似这种, 从数据库查询到的对应的数据,   
      只是Activiti的 入门, 根本满足不了 产品的需求的, 比如一个Activiti 稍微难一点(中国式流程)的功能, 流程任意跳转,  
      功能, 比如需要完成这个功能, 是相当有难度的, 要完成这个功能 , 其实大部分的操作 都不是表, 而是 一些高级的类中,   
      只不过数据最后流转完了之后到了数据库中     













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


