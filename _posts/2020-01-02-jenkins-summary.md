---
layout: post
title:  "Jenkins持续集成环境搭建"
date:  2020-01-02 13:34:00
categories: Jenkins
tags: [Jenkins]
---
# 前言
&emsp;&emsp;由于项目需求，需要搭建一个持续集成平台，主要用于项目的自动化部署，因此在网上调研了一下持续集成工具，经过调研发现，Jenkins这个工具是比较适合的。Jenkins是一个开源的、可扩展的持续集成、交付、部署（软件/代码的编译、打包、部署）的基于web界面的平台。允许持续集成和持续交付项目，无论用的是什么平台，可以处理任何类型的构建或持续集成。

&emsp;&emsp;本文主要讲解如何利用持续集成工具——Jenkins来搭建一套持续集成环境。主要使用的工具有：
- Jenkins版本：2.176.1
- SonarQube版本：7.7
- Jdk版本：1.8
<!-- more -->

# 平台架构
![平台架构](https://howdypl.github.io/img/jenkins/jenkins-framework.png "平台架构图"){:height="100%" width="100%"}

# 前期准备
&emsp;&emsp;主要介绍为了完成Jenkins持续集成环境的搭建，并实现节点的高可用，前期需要做哪些准备。

## JDK安装
&emsp;&emsp;JDK主要提供Jenkins运行所需的环境，需要注意的一点是`在配置从节点的时候从节点的jdk版本要和主节点的保持一致。`

&emsp;&emsp;JDK安装及环境配置的具体操作请[点击这里](https://blog.csdn.net/pang_ping/article/details/80570011 "JDK安装及环境配置的具体操作")。

## NFS挂载目录
&emsp;&emsp;进行挂载的主要目的是`保证各节点之间数据的一致性。`只需要挂载四个目录即可，分别是：
1. maven本地仓库的目录；
2. 备份包存放的目录；
3. 环境准备所需文件存放的目录；
4. Jenkins工作空间所在的目录；

&emsp;&emsp;Centos7安装配置NFS服务和挂载的具体操作请[点击这里](https://www.cnblogs.com/lixiuran/p/7117000.html "Centos7安装配置NFS服务和挂载")。

## CYGWIN安装`（针对Windows系统的部署主机）`
&emsp;&emsp;Cygwin是在Windows操作系统上仿真Linux操作系统，是一个在Windows平台上运行的Linux模拟环境，它使用动态链接库*.dll来实现，简单来说Cywin是一个Windows的软件。

&emsp;&emsp;通过Cygwin在Windows环境中安装并配置SSH的具体操作请[点击这里](https://my.oschina.net/u/658505/blog/616079 "通过Cygwin在Windows环境中安装并配置SSH")。

## 配置Linux服务器免密登录
&emsp;&emsp;实现系统的自动化部署需要配置Linux服务器的免密登录，`方便远程执行启动脚本、上传发布包和拉取备份包等。`配置免密登录的时候Jenkins的主从节点共用一个私钥，所有的部署主机共用一个公钥。

&emsp;&emsp;配置Linux服务器的免密登录的具体操作请[点击这里](https://www.cnblogs.com/leafinwind/p/10629547.html "配置Linux服务器的免密登录")。

# 环境搭建

## Jenkins安装及使用简介	
&emsp;&emsp;主要介绍Jenkins的安装过程和使用的简介
	
&emsp;&emsp;Jenkins是基于Java开发的一种持续集成工具，用于监控持续重复的工作，下载地址请[点击这里](https://jenkins.io/zh/download/ "Jenkins下载地址")。Jenkins提供了Windows、Linux和OS X平台的安装包。最简便的还是使用提供的war包直接启动，但是此时必须保证系统中已经安装了jdk，最好是 jdk1.5以上。

### 安装Jenkins
&emsp;&emsp;我这里展示的是直接运行war包的方式，其它安装方式请[点击这里](https://jenkins.io/zh/doc/book/getting-started/ "官方参考文档")参考官方文档。

#### 1、启动Jenkins
&emsp;&emsp;下载好jenkins.war包之后，切换到下载目录，然后执行如下命令：

>&emsp;`在启动之前要确保jdk环境已经准备完成。`默认情况下端口是8080，如果要使用其他端口启动，可以通过命令行"java -jar jenkins.war --ajp13Port=-1 --httpPort=8081 > log_jenkins.log 2>&1 &"的方式修改

```
  java -jar jenkins.war > log_jenkins.log 2>&1 &
```
&emsp;&emsp;Jenkins的工作目录，默认是：/用户名/.jenkins。
也可以自定义Jenkins的工作目录，设置JENKINS_HOME环境变量，启动 jenkins.war后将被解压到JENKINS_HOME目录下，同时所有Jenkins的 plugins和配置文件等也将被写入到JENKINS_HOME所设置的目录下。

&emsp;&emsp;启动以后通过查看日志输出，可以发现生成的有随机口令

![随机口令](https://howdypl.github.io/img/jenkins/jenkins-1.png "随机口令"){:height="100%" width="100%"}

#### 2、网页打开后进行配置

- **第一次启动Jenkins时，出于安全考虑，Jenkins会自动生成一个随机的安装口令。**
注意控制台输出的口令，复制下来。在浏览器中输入地址：`http://localhost:8080/`

![解锁Jenkins](https://howdypl.github.io/img/jenkins/jenkins-2.png "解锁Jenkins"){:height="100%" width="100%"}

- **选择需要安装的插件**

&emsp;&emsp;选择默认推荐即可，会安装通用的社区插件，剩下的可以在使用的时候再进行安装。

![安装插件](https://howdypl.github.io/img/jenkins/jenkins-3.png "安装插件"){:height="100%" width="100%"}

- **开始安装，由于网络原因，有一些插件可能会安装失败。**

![安装插件过程中](https://howdypl.github.io/img/jenkins/jenkins-4.png "安装插件过程中"){:height="100%" width="100%"}

- **设置Admin用户和密码**

![初始化管理员](https://howdypl.github.io/img/jenkins/jenkins-5.png "初始化管理员"){:height="100%" width="100%"}

- **安装完成**

![安装完成](https://howdypl.github.io/img/jenkins/jenkins-6.png "安装完成"){:height="100%" width="100%"}

- **登录Jenkins**

![登录Jenkins](https://howdypl.github.io/img/jenkins/jenkins-7.png "登录Jenkins"){:height="100%" width="100%"}

### Jenkins使用简介

Jenkins的使用简介具体请[点击这里](https://www.cnblogs.com/along21/p/9724036.html#auto_id_9 "Jenkins使用简介")

## 配置Jenkins从节点
&emsp;&emsp;Jenkins的分布式构建，在Jenkins的配置中叫做节点，分布式构建能够让同一套代码或项目在不同的环境(如：Windows和Linux系统)中编译、部署等。节点服务器不需要安装jenkins，只需要运行一个slave节点服务，构建事件的分发由master端（jenkins主服务）来执行。

配置Jenkins从节点的具体操作请[点击这里](https://www.jianshu.com/p/e93229012ac0 "配置Jenkins从节点")，`再次强调各节点之间的JDK版本要保持一致`

## Jenkins集成SonarQube
&emsp;&emsp;SonarQube是一款用于代码质量管理的开源工具，它主要用于源代码质量的管理。通过插件形式，可以支持众多计算机语言，通过代码规则检测工具来检测代码，帮助开发人员发现代码的漏洞，Bug，异味等信息。

&emsp;&emsp;`安装SonarQube的具体操作`请[点击这里](https://www.jianshu.com/p/349b4a0ac5b8)查看

&emsp;&emsp;`SonarQube的扫描器有很多种`，具体请[点击这里](https://docs.sonarqube.org/latest/analysis/overview/)查看

&emsp;&emsp;`Jenkins集成SonarScanner for Maven`的具体操作请[点击这里](https://blog.csdn.net/jin_hongxia/article/details/80844322 "Jenkins集成SonarScanner for Maven")

## Jenkins权限管理
&emsp;&emsp;Jenkins粒度比较细的权限管理需要借助插件`Role-based Authorization Strategy`，具体操作请[点击这里](https://www.cnblogs.com/sker/p/9255338.html "Jenkins权限管理")查看

## SonarQube权限管理
&emsp;&emsp;SonarQube权限管理的具体操作请[点击这里](https://blog.csdn.net/danielchan2518/article/details/72792897 "SonarQube权限管理")查看

# 自动化部署实现

## 总体方案

### 流程图
![方案流程图](https://howdypl.github.io/img/jenkins/jenkins-8.png "方案流程图"){:height="100%" width="100%"}

### CI/CD过程

#### 环境准备

&emsp;&emsp;此步骤将检查目标服务器系统运行所需要的环境（比如：JDK、Tomcat等）；检查是否有备份目录和临时目录，无则创建

#### 拉取代码
&emsp;&emsp;此步骤将清空上一次构建遗留在工作区的文件，避免缓存，避免磁盘占用浪费，并从git仓库或者svn拉取代码。

#### 质量检查
&emsp;&emsp;此步骤将采用SonarQube进行代码质量检查，并推送检查报告到SonarQube服务器。

#### 单元测试
&emsp;&emsp;此步骤将执行maven单元测试，如果项目目录下存在测试用例。

#### 系统构建
&emsp;&emsp;此步骤将执行maven构建。

#### 传输war包到临时目录
&emsp;&emsp;此步骤将从jenkins服务器构建好的war包传输的目标服务器的临时目录：
这样做相当于是在构建和发布之间做了一个抽离。因为有的项目构建好的war包比较大，服务器之间的传输时间很长，而tomcat自带热部署功能，会对发布过程产生影响。
1. 判断目标服务器是否有临时目录
2. 如果没有，创建临时目录
3. 将jenkins构建好的war包传送到目标服务器临时目录。

#### 系统备份
&emsp;&emsp;此步骤将从目标服务器备份上一次发布的部署包：
1. 创建jenkins服务器备份目录
4. 检查备份目录下是否有备份文件
4. 检查备份目录下是否有备份文件
5. 如果没有
6. 创建本地备份临时目录
7. 拉取目标服务器上的部署war包到临时目录
8. 发送到jenkins备份目录
9. 删除临时目录
10. 将部署服务器的war包备份到部署服务器指定的备份目录

#### 系统发布
&emsp;&emsp;此步骤将把构建好的部署包，发布到目标服务器项目目录上。此步骤在开始前，需要用户介入，确认并输入yes后继续。
1. 判断远程服务状态
2. 如果状态是启动，则停止
3. 删除远程服务器war包
4. 将远程服务器临时目录的war包移动到远程服务器部署目录
5. 重启服务

#### 系统回退
&emsp;&emsp;此步骤将把用于选择的部署包，发到目标服务器项目目录上，此步骤在开始前，需要用户介入，确认并输入yes后继续。
1. 判断远程服务状态
2. 如果状态是启动，则停止
3. 删除远程服务器war包
4. 将远程服务器临时目录的war包移动到远程服务器部署目录
5. 重启服务

#### 应用重启
&emsp;&emsp;此步骤将重启目标服务器tomcat。该步骤仅选择“Restart”才会执行。




