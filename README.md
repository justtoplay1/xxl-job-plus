# xxl-job-plus

## 特别鸣谢

**[许雪里/xxl-job](https://www.xuxueli.com/xxl-job/)**

## 前言

项目从分布式升级为微服务后，针对服务运行状态的管控就显得尤为重要。spring cloud中的注册中心，不管是nacos
还是eureka都可以监测和管理服务上下线的状态。有时候可能临时需要再上新服务节点，这时候再去挨个修改现有服务的调用ip配置就显得不合时宜了。xxl-job作为一款分布式定时任务组件，极大的方便了各服务模块的任务管理，但是使用之前，必须配置好xxl-job-admin的地址列表，同时如果我们在注册中心临时下线了某个服务，这个时候xxl-job-admin的执行器列表中这个服务还是在线状态，管理就不是很统一。为了解决这种配置问题和服务上下线问题，xxl-job-plus应运而生，更好的服务于springcloud服务运维。

## 介绍

xxl-job-plus是xxl-job的增强包，在不对xxl-job-core源码修改的情况下，提供对接注册中心能力。支持监测注册中心xxl-job-admin服务上下线，executor服务上下线，实现executor向xxl-job-admin自动启动、刷新、停止，完美兼容xxl-job-admin。同时不再需要为executor单独配置ip和port（ip和port为注册中心中注册的ip和port，原有netty端口监听不再使用，改用spring-web
controller 实现），也不需要配置admin地址（动态从注册中心拉取）。也无需配置XxlJobExecutor，由本包自动注册。现已支持nacos注册中心，eureka将在下个版本推出。

## 软件架构

[![架构.png](https://s1.ax1x.com/2022/07/04/jJq4Ds.md.png)](https://imgtu.com/i/jJq4Ds)

Xxl-job-plus采用接口扩展和动态反射技术，对原有xxl-job-core配置做修改和注入，对xxl-job-core版本依赖性强，建议使用对应的版本

## 环境要求

jdk 1.8+

nacos-discovery-spring-boot-starter:0.2.8+ ｜ spring-cloud-starter-alibaba-nacos-discovery:2.2.5.RELEASE +

xxl-job-core:2.3.1+

## 下载

```java
<dependency>
<groupId>com.justtoplay</groupId>
<artifactId>xxl-job-plus</artifactId>
<version>${对应的xxl-job-core版本}</version>
</dependency>
```

## 快速入门

### 服务端xxl-job-admin

#### 方式一：原工程构建

此部分除nacos接入外，其它步骤均以xxl-job的文档为准

1. 下载xxl-job源码： http://gitee.com/xuxueli0323/xxl-job

2. 刷数据库： /xxl-job/doc/db/tables_xxl_job.sql

3. xxl-job-admin工程pom添加引入，nacos-client版本请匹配nacos server版本

   ```
   <dependency>
      <groupId>com.alibaba.boot</groupId>
      <artifactId>nacos-discovery-spring-boot-starter</artifactId>
      <version>${版本号}</version>
   </dependency>
   ```

4. 修改 xxl-job-admin 工程的application.properties配置文件

   1. 修改数据库配置

      ```
      spring.datasource.url=jdbc:mysql://127.0.0.1:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai
      spring.datasource.username=root
      spring.datasource.password=123456
      ```

   2. 添加注册中心相关配置

      ```
      spring.application.name=xxl-job-admin
      nacos.discovery.server-addr=127.0.0.1:8848
      nacos.discovery.register.ip=127.0.0.1
      nacos.discovery.register.port=${server.port}
      nacos.discovery.namespace=
      nacos.discovery.auto-register=true
      ```

5. 修改xxl-job-admin 工程的logback文件

   ```
   <property name="log.path" value="./logs/xxl-job/xxl-job-admin.log"/>
   ```

6. 启动工程

7. 查看注册中心服务列表中是否注册成功

8. 访问调度中心

   * http://localhost:8080/xxl-job-admin
   * 用户名/密码 ：admin/123456

#### 方式二：docker镜像

1. 下载镜像（建议指定版本号）

   ```
   docker pull justtoplay/xxl-job-admin:{指定版本}
   ```

2. 创建容器并运行

   ```
   docker run -e PARAMS="--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai --spring.application.name=xxl-job-admin --nacos.discovery.server-addr=127.0.0.1:8848 --nacos.discovery.register.ip=127.0.0.1 --nacos.discovery.register.port=${server.port} --nacos.discovery.namespace= --nacos.discovery.auto-register=true" -p 8080:8080 -v /tmp:/data/applogs --name xxl-job-admin  -d justtoplay/xxl-job-admin:{指定版本}
   ```

3. 查看是否注册成功，能否正常访问调度中心

### 执行器端

1. 业务工程引入pom依赖

   ```
   <dependency>
       <groupId>com.xuxueli</groupId>
       <artifactId>xxl-job-core</artifactId>
       <version>${xxl-job-core版本号}</version>
   </dependency>
   
   <dependency>
       <groupId>com.justtoplay</groupId>
       <artifactId>xxl-job-plus</artifactId>
       <version>${xxl-job-core版本号}</version>
   </dependency>
   
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-web</artifactId>
   </dependency>
   ```

2. 引入nacos-discovery

   * spring boot 工程

      1. 修改pom

         ```
         <dependency>
             <groupId>com.alibaba.boot</groupId>
             <artifactId>nacos-discovery-spring-boot-starter</artifactId>
             <version>${和nacos server匹配的client版本号}</version>
         </dependency>
         ```

      2. 修改 业务工程的application.properties配置文件

         ```
         spring.application.name=executor-sample
         nacos.discovery.server-addr=127.0.0.1:8848
         nacos.discovery.namespace=
         nacos.discovery.auto-register=true
         ```

   * spring cloud 工程

      1. 修改pom

         ```
         <dependency>
             <groupId>com.alibaba.cloud</groupId>
             <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
             <version>${和nacos server匹配的client版本号}</version>
         </dependency>
         ```

      2. 修改 业务工程的application.properties配置文件

         ```
         spring.application.name=executor-sample
         spring.cloud.nacos.discovery.server-addr=127.0.0.1:8848
         spring.cloud.nacos.discovery.namespace=
         ```

3. 修改业务工程的application.properties配置文件

   ```
   # xxl-job-admin注册到注册中心的名称
   xxl.job.plus.admin.service-name=xxl-job-admin
   
   # executor 注册到xxl-job-admin executor列表的名称，默认同spring.application.name，可不用配置
   xxl.job.plus.executor.service-name=
   
   # 使用默认的就好，可以不用配置
   xxl.job.plus.admin.access-token=
   xxl.job.plus.admin.context-path=
   xxl.job.plus.executor.log-path=
   xxl.job.plus.executor.log-retention-days=
   ```

4. 启动工程

5. 查看注册中心服务是否注册上

6. 进入xxl-job-admin，手动新增执行器，输入执行器名，选择自动注册，保存。在列表中就能看到online机器出现

7.
注册中心上下线xxl-job-admin服务，executor服务会自动刷新本地缓存，不再对下线的xxl-job-admin发送心跳，有新xxl-job-admin服务上线，会自动发送心跳，由于nacos的状态订阅延时和xxl-job-core心跳延时，xxl-job-admin中executor服务online状态会存在一定的延时

8.
注册中心上下线executor服务，executor服务台会自动启动或停止executor在xxl-job-admin中的状态。由于nacos的状态订阅延时和xxl-job-core心跳延时，xxl-job-admin中executor服务online状态会存在一定的延时

## 参与贡献

欢迎参与项目贡献！比如提交PR修复一个bug，或者新建 Issue 讨论新特性或者变更。

## 赞助

开源不易，如果你享受本开源产品带来的便利，那就请作者喝杯奶茶吧

