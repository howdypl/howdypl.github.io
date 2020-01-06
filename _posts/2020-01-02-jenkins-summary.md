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

&emsp;&emsp;自动化部署的实现是`采用流水线的方式`，主要分为三个部分，分别是`总体方案的设计`、`参数化构建的配置`和`groovy脚本的编写`。

## 总体方案的设计

&emsp;&emsp;普通用户登录Jenkins，点击所需要构建的项目进行构建：
1. 用户操作主要有三个，分别为`Restart（重启项目）`，`Deploy（发布项目）`，`RollBack（回退项目）`；

2. 如果选择Deploy，并选择需要发布的节点（默认选择已配置的所有节点）则`会执行系统回退和重启外的所有步骤`；

3. 如果选择RollBack，则需要选择回退的部署包，并选择需要回退的节点（默认选择已配置的所有节点）然后`会执行环境准备以及系统回退步骤`；

4. 当选择Restart时，并选择需要重启的节点（默认选择已配置的所有节点）然后就`会执行系统重启步骤`。

### 流程图
![方案流程图](https://howdypl.github.io/img/jenkins/jenkins-8.png "方案流程图"){:height="100%" width="100%"}

### CI/CD过程

#### 环境准备

&emsp;&emsp;此步骤将检查目标服务器系统运行所需要的环境（比如：JDK、Tomcat等），如果没有则通过脚本进行自动安装；检查是否有备份目录和临时目录，无则创建

#### 拉取代码
&emsp;&emsp;此步骤将清空上一次构建遗留在工作空间的文件，避免缓存，避免磁盘占用浪费，并从git仓库或者svn拉取代码。

#### 质量检查
&emsp;&emsp;此步骤将采用SonarQube进行代码质量检查，并推送检查报告到SonarQube服务器。

#### 单元测试
&emsp;&emsp;此步骤将执行maven单元测试，如果项目目录下存在测试用例。

#### 系统构建
&emsp;&emsp;此步骤将执行maven构建。

#### 传输发布包到临时目录
&emsp;&emsp;此步骤将从jenkins服务器上通过maven构建好的发布包传输到目标服务器的临时目录：
这样做相当于是在构建和发布之间做了一个抽离。因为有的项目构建好的发布包比较大，服务器之间的传输时间很长，而tomcat自带热部署功能，会对发布过程产生影响。
1. 判断目标服务器是否有临时目录
2. 如果没有，创建临时目录
3. 将Jenkins服务器上通过maven构建好的发布包传送到目标服务器的临时目录。

#### 系统备份
&emsp;&emsp;此步骤将从目标服务器上备份上一次发布的发布包：
1. 在Jenkins服务器上创建备份目录
2. 检查备份目录下是否有备份文件，如果没有，就创建本地备份临时目录
3. 拉取目标服务器上的上一次发布的发布包到临时目录
4. 判断临时目录中是否拉取的文件，没有则删除临时目录，并输出提示语句
5. 将服务器上一次发布的发布包备份到Jenkins服务器指定的备份目录

#### 系统发布
&emsp;&emsp;此步骤将把构建好的发布包，发布到目标服务器项目目录上。`此步骤在开始前，需要用户介入，确认并输入yes后继续。`
1. 判断远程服务状态
2. 如果状态是启动，则停止
3. 删除远程服务器上的发布包
4. 将远程服务器临时目录的发布包移动到远程服务器部署目录
5. 重启服务

#### 系统回退
&emsp;&emsp;此步骤将把用于选择的回退包，发到目标服务器项目目录上，`此步骤在开始前，需要用户介入，确认并输入yes后继续。`
1. 判断远程服务状态
2. 如果状态是启动，则停止
3. 删除远程服务器发布包
4. 将远程服务器临时目录的发布包移动到远程服务器部署目录
5. 重启服务

#### 应用重启
&emsp;&emsp;此步骤将重启目标服务器上的项目。该步骤仅选择“Restart”才会执行。

## 参数化构建配置
&emsp;&emsp;由于在总体方案设计的时候提供了`发布项目、回退项目和重启项目`这三个功能，还需要动态的选择需要发布的节点，因此采用参数化构建的方式。（可能需要安装插件）

&emsp;&emsp;参数化构建的简单操作请[点击这里](https://zhuanlan.zhihu.com/p/42139069 "参数化构建的简单操作")查看。

### 添加选项参数
&emsp;&emsp;此步骤主要用于参数化构建时生成一个下拉框,可以选择执行的不同功能。

&emsp;&emsp;**`具体步骤：选中“参数化构建过程”——>选中“添加参数”——>选中“选项参数”`**

![添加选项参数1](https://howdypl.github.io/img/jenkins/jenkins-9-1.png "添加选项参数1"){:height="100%" width="100%"}

```
名称：action
选项：
Deploy
RollBack
Restart
描述：
<div style="color:#ff3300;font-weight:1600;font-weight:bold;">Deploy         【发布应用】 <br>
  RollBack  【回滚应用】<br>   Restart  【重启应用】<br></div>
```
![添加选项参数2](https://howdypl.github.io/img/jenkins/jenkins-9-2.png "添加选项参数2"){:height="100%" width="100%"}

### 添加动态选项参数
&emsp;&emsp;**`具体步骤：选中“参数化构建过程”——>选中“添加参数”——>选中“Active Choices Reactive Parameter”`**

&emsp;&emsp;当选择Rollback时，会在param_rollback_pkg处，以下拉框的形式显示出所有的备份包`（默认选中最近一次备份的备份包）`

![添加动态选项参数1](https://howdypl.github.io/img/jenkins/jenkins-10-1.png "添加动态选项参数1"){:height="100%" width="100%"}

```
Name：param_rollback_pkg
Groovy Script:
path = param_backup_path+"/essapi-dev"
rollback=['bash', '-c', "ls -t1 ${path} "].execute().text.readLines()

if (action.equals("RollBack") && rollback) {
return rollback
}else {
	return ["NONE"]
}
Description：
<div style="color:#ff3300;font-weight:1600;font-weight:bold;">【请谨慎选择回退版本，NONE - 无可回退的版本】</div>

```
![添加动态选项参数2](https://howdypl.github.io/img/jenkins/jenkins-10-2.png "添加动态选项参数2"){:height="100%" width="100%"}

### 添加扩展选项参数
&emsp;&emsp;在构建时，生成多选框，选择需要部署的服务器节点。

&emsp;&emsp;**`具体步骤：选中“参数化构建过程”——>选中“添加参数”——>选中“Extended Choice Parameter”`**

![添加扩展选项参数1](https://howdypl.github.io/img/jenkins/jenkins-11-1.png "添加扩展选项参数1"){:height="100%" width="100%"}

&emsp;&emsp;注意：Choose Source for Value和Choose Source for Default Value中需点击value,然后按照 `远程服务器名1:远程服务器ip1:端口1,远程服务器名2:远程服务器ip2:端口2 `的格式填写。例如 `C1:192.168.1.1:22,C1:192.168.1.66:22`

![添加扩展选项参数2](https://howdypl.github.io/img/jenkins/jenkins-11-2.png "添加扩展选项参数2"){:height="100%" width="100%"}

### 最终效果
![最终效果](https://howdypl.github.io/img/jenkins/jenkins-12.png "最终效果"){:height="100%" width="100%"}

## groovy脚本编写
&emsp;&emsp;groovy脚本的编写采用的是Jenkins流水线的声明式语法，具体请[点击这里](https://jenkins.io/zh/doc/book/pipeline/syntax/ "Jenkins流水线的声明式语法")查看

```
import java.text.SimpleDateFormat

def getHost() {
    def targetStr = param_host.tokenize(',')
    def servers = []
    for (item in targetStr) {
        def remote = [:]
        def target = item.tokenize(':')
        remote.name = target[0]
        remote.host = target[1]
        remote.port = target[2].toInteger()
        servers.add(remote)
    }
    return servers
}


pipeline {
    agent { label 'linux' }
    tools {
        maven 'maven-3.6.1'
        jdk 'jdk1.8.0_211'
    }
    environment {
        env_ssh_key_id = "ssh-private-key"
        env_finalName = "lmsbaseservice-prod"
        env_projectName = "lms-base-service"
        env_deployType = "jar"
        env_prepareProjectPath = "/lc/app"
        env_projectLog = "lc/logs"
        env_param = "prod"

        env_prepareEnvFiles = "/devops/jenkins/serverFiles/lc/env-files"
        env_JDKFileName = "openjdk1.8.tar.gz"
        env_targetJDKPath = "/usr/lib/jvm"

        env_prepareProjectFiles = "/devops/jenkins/serverFiles/lc/project-files"
        env_preparepRojectFileName = "start-files.tar.gz"
        env_configName = "jenkinstart.sh"

        env_version = VersionNumber('_${BUILD_DATE_FORMATTED, \"yyyyMMdd\"}.${BUILDS_TODAY}')
        env_Date = VersionNumber('_${BUILD_DATE_FORMATTED, \"yyyyMMdd\"}')
        env_Hour = VersionNumber('_${BUILD_DATE_FORMATTED, \"HH\"}')

    }

    stages {
        stage('Prepare Environment') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: env_ssh_key_id, keyFileVariable: 'identity', passphraseVariable: '', usernameVariable: 'userName')]) {
                    script {
                        def servers = getHost()
                        for (server in servers) {
                            def remote = [:]
                            def javaCheck
                            remote.host = server.host
                            remote.port = server.port
                            remote.name = server.name
                            remote.user = userName
                            remote.identityFile = identity
                            remote.allowAnyHosts = true

                            //检查是否有OPENJDK 原始安装的路径
                            writeFile file: 'checkProjectPath.sh', text: "if [ ! -d ${env_targetJDKPath} ];then echo 'false';fi;"
                            def resultPath = sshScript remote: remote, script: "checkProjectPath.sh"
                            echo "================= checkProjectPath " + resultPath
                            if (resultPath == 'false') {

                                sshCommand remote: remote, command: "mkdir -p ${env_targetJDKPath}"
                                echo "================= Did not found OPENJDK on ${server.name}, start to install OPENJDK"
                                def jdk = env_prepareEnvFiles + "/" + env_JDKFileName
                                sshPut remote: remote, from: jdk, into: env_targetJDKPath
                                sshCommand remote: remote, command: "cd ${env_targetJDKPath}; tar -zxvf ${env_targetJDKPath}/${env_JDKFileName} ;rm -rf ${env_targetJDKPath}/${env_JDKFileName}"

                            } else {
                                javaCheck = sshCommand remote: remote, command: "find ${env_targetJDKPath} -type d -name 'java-1.8.0-openjdk-1.8.0*' "
                                //检查OPENJDK
                                if (javaCheck) {
                                    echo "================= OPENJDK ready"
                                } else {
                                    echo "================= Did not found OPENJDK on ${server.name}, start to install OPENJDK"
                                    def jdk = env_prepareEnvFiles + "/" + env_JDKFileName
                                    sshPut remote: remote, from: jdk, into: env_targetJDKPath
                                    sshCommand remote: remote, command: "cd ${env_targetJDKPath} ;tar -zxvf ${env_targetJDKPath}/${env_JDKFileName} ;rm -rf ${env_targetJDKPath}/${env_JDKFileName}"
                                }
                            }
                            //检查项目目录
                            def shellPath = env_prepareProjectPath+"/${env_configName}"
                            writeFile file: 'checkProjectPath.sh', text: "if [ ! -d ${env_prepareProjectPath} -o ! -f ${shellPath} ];then echo 'false';fi;"
                            def checkProjectPath = sshScript remote: remote, script: "checkProjectPath.sh"
                            echo "================= checkProjectPath " + checkProjectPath
                            if (checkProjectPath == 'false') {
                                sshCommand remote: remote, command: "mkdir -p ${env_prepareProjectPath} ; mkdir -p ${env_projectLog}"
                                def projectFilesPath = env_prepareProjectFiles + "/${env_preparepRojectFileName}"
                                sshPut remote: remote, from: projectFilesPath, into: env_prepareProjectPath
                                sshCommand remote: remote, command: "cd ${env_prepareProjectPath} ; tar -zxf ${env_preparepRojectFileName} ;chmod +x ${env_configName} ; rm -rf ${env_preparepRojectFileName}"
                            }
                        }
                    }
                }
                echo '============== Environment Ready'
            }
        }

        stage('Checkout') {
            when { expression { "${action}" == "Deploy" } }
            steps {
                script {
                    echo '============== Clean WorkSpace'
                    cleanWs()
                    echo '============== checkout code from svn'
                    if(param_svn_branch.trim() ==""){
                        echo "param_svn_branch is empty ,please input importantly"
                        sh"exit -1"
                    }
                    def addr = lc_uat_svn_address + "/" + env_projectName + "/branches/"+ param_svn_branch
                    checkout([$class: 'SubversionSCM', additionalCredentials: [], excludedCommitMessages: '', excludedRegions: '', excludedRevprop: '', excludedUsers: '', filterChangelog: false, ignoreDirPropChanges: false, includedRegions: '', locations: [[cancelProcessOnExternalsFail: true, credentialsId: lms_svn_cid, depthOption: 'infinity', ignoreExternalsOption: true, local: '.', remote: addr]], quietOperation: true, workspaceUpdater: [$class: 'UpdateUpdater']])
                }
            }
        }

        /*stage('Build') {
            when { expression { "${action}" == "Deploy" } }
            steps {
                echo '============== Build Project'
                withSonarQubeEnv('SonarQubeServer') {
                    sh 'mvn clean install sonar:sonar -Dmaven.test.skip=true'
                }
            }
        }*/

        stage("build && SonarQube analysis") {
            when { expression { "${action}" == "Deploy" } }
            steps {
                echo '============== 开始执行项目构建并进行sonar扫描'
                withSonarQubeEnv('SonarQubeServer') {
                    sh 'mvn clean install sonar:sonar -Dmaven.test.skip=true'
                }
            }

        }

        stage("Quality Gate"){
            when { expression { "${action}" == "Deploy" } }
            steps {
                wrap([$class: 'BuildUser']) {
                    script {
                        def BUILD_USER_ID = env.BUILD_USER_ID
                        if (BUILD_USER_ID == 'aux_admin') {
                            echo "此项目是由管理员进行构建，跳过sonar扫描结果判断步骤"
                        }else {
                            echo '============== 判断sonar扫描结果'
                            timeout(time: 5, unit: 'MINUTES') {
                                def qg = waitForQualityGate()
                                echo "============sonar扫描结果为：${qg.status}"
                                if (qg.status != 'OK') {
                                    error "由于sonar扫描的结果没有达到规定的指标，脚本执行中止"
                                }else {
                                    echo "由于sonar扫描的结果达到了规定的指标，脚本继续执行"
                                }
                            }

                        }
                    }
                }
            }
        }

        stage('BackupPackage') {
            when { expression { "${action}" == "Deploy" } }
            steps {
                script {
                    echo "================= Backup"
                    def servers = getHost()
                    def host = servers[0].host
                    def port = servers[0].port
                    def tmp = 'tmp' + env_version.trim()
                    //版本号备份
                    def packageNme = env_projectName +"*.jar"
                    def reBackJar = sh(script: "ssh -f -p ${port} root@${host}  find ${env_prepareProjectPath} -name ${packageNme} ", returnStdout: true).trim()
                    if(reBackJar){
                        def backName = reBackJar.split("/")[-1]-".jar"
                        def backupName = backName + "." + env_deployType + env_Date + env_Hour
                        sh "mkdir -p ${param_backup_path}/${env_finalName}"
                        exits = sh(script: "find ${param_backup_path}/${env_finalName} -name ${backupName}", returnStdout: true).trim()
                        if (exits) {
                            echo "================= ${param_backup_path}/${env_finalName} 备份路径下已存在备份文件：${backupName}"
                        } else {
//                            sh "mkdir -p ${param_backup_path}/${tmp}"

                            //创建备份临时目录
                            def tmpPath = param_backup_path+"/"+env_finalName+"/"+tmp
                            sh "mkdir -p ${tmpPath}"

                            sh "scp -BCrq -P ${port} root@${host}:${reBackJar} ${tmpPath}"
                            files = sh(script: "ls -l ${tmpPath}|grep '^-'|wc -l", returnStdout: true).trim()
                            if (files > 0) {
                                sh "tar -cvf ${param_backup_path}/${env_finalName}/${backupName} ${tmpPath}/.;rm -rf ${tmpPath}"
                            } else {
                                echo "================= 检查远程服务器${host}:${env_prepareProjectPath}是否存在jar包且不为空！"
                                sh "rm -rf ${tmpPath}"
                            }
                        }
                    }else {
                        echo "================= 检查远程服务器${host}:${env_prepareProjectPath}是否存在jar包且不为空！"
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                beforeInput true
                expression { "${action}" == "Deploy" }
            }
            input {
                message "请确认发布?"
                ok "是"
                parameters {
                    string(name: 'confirm', defaultValue: '', description: '如果确认发布，请在上框中输入 yes ')
                }
            }
            steps {
                script {
                    if ("${confirm}" != "yes") {
                        echo "================= 取消发布"
                        sh 'exit 0'
                    } else {
                        echo '================= deploy to remote server'
                        withCredentials([sshUserPrivateKey(credentialsId: env_ssh_key_id, keyFileVariable: 'identity', passphraseVariable: '', usernameVariable: 'userName')]) {
                            script {
                                def packageName = env_projectName + '*.' + env_deployType
                                //定位
                                def fullPath = sh(script: "find ${WORKSPACE} -name ${packageName}", returnStdout: true).trim()
                                def jarName = fullPath.split("/")
                                def jar = jarName[-1] - ".jar"
                                if (fullPath) {
                                    def servers = getHost()
                                    for (server in servers) {
                                        def remote = [:]
                                        remote.host = server.host
                                        remote.name = server.name
                                        remote.user = userName
                                        remote.port = server.port
                                        remote.identityFile = identity
                                        remote.allowAnyHosts = true

                                        //查看jar包是否在运行，如果在运行就kill掉
                                        echo "======== 查看jar包是否在运行"
                                        def bakStr = sshCommand remote: remote, command: "ps -ef | grep -w ${env_projectName} | grep -v grep | awk '{print \$2}'"
                                        if(bakStr){
                                            echo "======== jar包正在运行，开始kill掉！"
                                            def pids = bakStr.tokenize("\n")
                                            for(def pid in pids){
                                                echo "======pid: ${pid}"
                                                sh "ssh -f -p ${server.port} root@${server.host} kill -9 ${pid}"
                                            }
                                        }else {
                                            echo "======== jar包没有在运行！"
                                        }
                                        echo "======== 删除已经备份的jar包"
                                        sshCommand remote: remote, command: "rm -rf ${env_prepareProjectPath}/${packageName}"

                                        echo "======== 发布jar包${jar}向服务器 ${server.name}"
                                        sshPut remote: remote, from: fullPath, into: env_prepareProjectPath
                                        sh "ssh -f -p ${server.port} root@${server.host} sh ${env_prepareProjectPath}/jenkinstart.sh start ${jar} ${env_param}"
                                        sleep 10
                                    }
                                } else {
                                    echo "================= No ${packageName} package found ."
                                    sh 'exit -1'
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('RollBack') {
            when {
                beforeInput true
                expression { "${action}" == "RollBack" }
            }
            input {
                message "确认回退?"
                ok "是"

                parameters {
                    string(name: 'confirm', defaultValue: '', description: '如果确认回退，请在上框中输入 yes ')
                }
            }
            steps {
                script {
                    if ("${confirm}" != "yes") {
                        echo "================= 取消回退"
                        sh 'exit 0'
                    } else {

                        withCredentials([sshUserPrivateKey(credentialsId: env_ssh_key_id, keyFileVariable: 'identity', passphraseVariable: '', usernameVariable: 'userName')]) {
                            script {
                                echo "================= RollBack from remote server,the package is :" + param_rollback_pkg
                                def servers = getHost()
                                def packageName = env_projectName + '*.' + env_deployType
                                def fullPath = sh(script: "find ${param_backup_path} -name ${param_rollback_pkg}", returnStdout: true).trim()
                                if (fullPath) {

                                    for (server in servers) {

                                        def remote = [:]
                                        remote.host = server.host
                                        remote.port = server.port
                                        remote.name = server.name
                                        remote.user = userName
                                        remote.identityFile = identity
                                        remote.allowAnyHosts = true

                                        /*def packageName = env_projectName + '*.' + env_deployType
                                        def path = sh(script: "ssh -f -p ${server.port} root@${server.host} find ${env_prepareProjectPath} -name ${packageName}", returnStdout: true).trim()
                                        def jarName = path.split("/")
                                        def jarPrefix = jarName[-1] - ".jar"
                                        echo "======当前的jar包===${jarPrefix}===${env_param}"
                                        def bakStr = sshCommand remote: remote, command: "ps -ef | grep -w ${jarPrefix} | grep -v grep | awk '{print \$2}'"
                                        if(bakStr){
                                            def pids = bakStr.tokenize("\n")
                                            for(def pid in pids){
                                                echo "======pid: ${pid}"
                                                sh "ssh -f -p ${server.port} root@${server.host} kill -9 ${pid}"
                                            }
                                            echo "======== 删除原来存在的jar包"
                                            sshCommand remote: remote, command: "rm -rf ${path}"
                                        }

                                        //发送jar包
                                        def fileName = param_backup_path + "/" + env_finalName + "/" + param_rollback_pkg
                                        sshPut remote: remote, from: fileName, into: env_prepareProjectPath
                                        sshCommand remote: remote, command: "cd ${env_prepareProjectPath};tar -xvf ${param_rollback_pkg} --strip-components 5;rm -rf ${env_prepareProjectPath}/${param_rollback_pkg}"

                                        packageName = env_projectName + '*.' + env_deployType
                                        path = sh(script: "ssh -f -p ${server.port} root@${server.host} find ${env_prepareProjectPath} -name ${packageName}", returnStdout: true).trim()
                                        jarName = path.split("/")
                                        jarPrefix = jarName[-1] - ".jar"

                                        sh "ssh -f -p ${server.port} root@${server.host} sh ${env_prepareProjectPath}/jenkinstart.sh start ${jarPrefix} ${env_param}"*/

                                        //查看jar包是否在运行，如果在运行就kill掉
                                        echo "======== 查看jar包是否在运行"
                                        def bakStr = sshCommand remote: remote, command: "ps -ef | grep -w ${env_projectName} | grep -v grep | awk '{print \$2}'"
                                        if(bakStr){
                                            echo "======== jar包正在运行，开始kill掉！"
                                            def pids = bakStr.tokenize("\n")
                                            for(def pid in pids){
                                                echo "======pid: ${pid}"
                                                sh "ssh -f -p ${server.port} root@${server.host} kill -9 ${pid}"
                                            }
                                        }else {
                                            echo "======== jar包没有在运行！"
                                        }
                                        echo "======== 删除已经备份的jar包"
                                        sshCommand remote: remote, command: "rm -rf ${env_prepareProjectPath}/${packageName}"

                                        //发送jar包
                                        def fileName = param_backup_path + "/" + env_finalName + "/" + param_rollback_pkg
                                        sshPut remote: remote, from: fileName, into: env_prepareProjectPath
                                        sshCommand remote: remote, command: "cd ${env_prepareProjectPath};tar -xvf ${param_rollback_pkg} --strip-components 5;rm -rf ${env_prepareProjectPath}/${param_rollback_pkg}"
                                        sleep 5
                                        //得到回退jar包的名称，然后启动jar包
                                        def path = sh(script: "ssh -f -p ${server.port} root@${server.host} find ${env_prepareProjectPath} -name ${packageName}", returnStdout: true).trim()
                                        if (path){
                                            def jarName = path.split("/")
                                            def jarPrefix = jarName[-1] - ".jar"

                                            sh "ssh -f -p ${server.port} root@${server.host} sh ${env_prepareProjectPath}/jenkinstart.sh start ${jarPrefix} ${env_param}"
                                            sleep 5
                                        }else {
                                            error "================回退失败，没有找到回退包！"
                                        }
                                    }
                                } else {
                                    echo "============== 回退失败，${param_rollback_pkg}在${param_backup_path}/${env_finalName}路径下不存在。"
                                    sh 'exit 1'
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('ReStartApplication') {
            when {
                beforeInput true
                expression { "${action}" == "Restart" }
            }
            input {
                message "确认重启应用?"
                ok "是"
                parameters {
                    string(name: 'confirm', defaultValue: '', description: '如果确认重启应用，请在上框中输入 yes ')
                }
            }
            steps {
                script {
                    if ("${confirm}" != "yes") {
                        echo "================= 取消启动"
                        sh 'exit 0'
                    } else {

                        withCredentials([sshUserPrivateKey(credentialsId: env_ssh_key_id, keyFileVariable: 'identity', passphraseVariable: '', usernameVariable: 'userName')]) {

                            script {
                                echo "================= 重新启动应用"
                                def servers = getHost()
                                for (server in servers) {
                                    def remote = [:]
                                    remote.host = server.host
                                    remote.port = server.port
                                    remote.name = server.name
                                    remote.user = userName
                                    remote.identityFile = identity
                                    remote.allowAnyHosts = true

                                    def packageName = env_projectName + '*.' + env_deployType


                                    def bakStr = sshCommand remote: remote, command: "ps -ef | grep -w ${env_projectName} | grep -v grep | awk '{print \$2}'"

                                    if(bakStr){
                                        def pids = bakStr.tokenize("\n")
                                        for(def pid in pids){
                                            echo "======pid: ${pid}"
                                            sh "ssh -f -p ${server.port} root@${server.host} kill -9 ${pid}"
                                        }
                                    }
                                    //得到需要重启的jar包名称，然后重启
                                    def jarPath = sh(script: "ssh -f -p ${server.port} root@${server.host} find ${env_prepareProjectPath} -name ${packageName}", returnStdout: true).trim()
                                    def jarPrefix = jarPath.split("/")[-1] - ".jar"
                                    echo "============== 重新启动${server.name}上的应用"
                                    sh "ssh -f -p ${server.port} root@${server.host} sh ${env_prepareProjectPath}/jenkinstart.sh start ${jarPrefix} ${env_param}"
                                    sleep 5
                                }
                            }

                        }

                    }
                }
            }
        }
    }

    //此处是job执行完成以后进行短信通知，具体是使用http request插件调用短信发送接口
    post {
        success {
            script{
                def by = currentBuild.getBuildCauses()
                def second = currentBuild.duration/1000
                def startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(currentBuild.startTimeInMillis)
                def endTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date())
                def body = '{"userNumber":"'+"${lms_admin_phone}"+'", "messageContent":"'+"Jenkins持续交付平台通知：\\n构建结果：成功\\n构建环境：正式环境\\n项目名称：lms项目\\n模块名称：${currentBuild.projectName}\\n触发原因：${by[0].shortDescription}\\n开始构建时间：${startTime}\\n结束构建时间：${endTime}\\n持续构建时间：${second}s\\n详细信息：${BUILD_URL}"+'", "systemName":"Jenkins"}'
                def response = httpRequest acceptType: 'APPLICATION_JSON_UTF8', contentType: 'APPLICATION_JSON_UTF8', httpMode: 'POST', requestBody: body, url: "${phone_api_address}"

            }
        }
        failure {
            script{
                def by = currentBuild.getBuildCauses()
                def second = currentBuild.duration/1000
                def startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(currentBuild.startTimeInMillis)
                def endTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date())
                def body = '{"userNumber":"'+"${lms_admin_phone}"+'", "messageContent":"'+"Jenkins持续交付平台通知：\\n构建结果：失败\\n构建环境：正式环境\\n项目名称：lms项目\\n模块名称：${currentBuild.projectName}\\n触发原因：${by[0].shortDescription}\\n开始构建时间：${startTime}\\n结束构建时间：${endTime}\\n持续构建时间：${second}s\\n详细信息：${BUILD_URL}"+'", "systemName":"Jenkins"}'
                def response = httpRequest acceptType: 'APPLICATION_JSON_UTF8', contentType: 'APPLICATION_JSON_UTF8', httpMode: 'POST', requestBody: body, url: "${phone_api_address}"

            }
        }
        unstable {
            script{
                def by = currentBuild.getBuildCauses()
                def second = currentBuild.duration/1000
                def startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(currentBuild.startTimeInMillis)
                def endTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date())
                def body = '{"userNumber":"'+"${lms_admin_phone}"+'", "messageContent":"'+"Jenkins持续交付平台通知：\\n构建结果：失败（由于测试失败,代码违规等造成）\\n构建环境：正式环境\\n项目名称：lms项目\\n模块名称：${currentBuild.projectName}\\n触发原因：${by[0].shortDescription}\\n开始构建时间：${startTime}\\n结束构建时间：${endTime}\\n持续构建时间：${second}s\\n详细信息：${BUILD_URL}"+'", "systemName":"Jenkins"}'
                def response = httpRequest acceptType: 'APPLICATION_JSON_UTF8', contentType: 'APPLICATION_JSON_UTF8', httpMode: 'POST', requestBody: body, url: "${phone_api_address}"

            }
        }
        aborted {
            script{
                def by = currentBuild.getBuildCauses()
                def second = currentBuild.duration/1000
                def startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(currentBuild.startTimeInMillis)
                def endTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date())
                def body = '{"userNumber":"'+"${lms_admin_phone}"+'", "messageContent":"'+"Jenkins持续交付平台通知：\\n构建结果：中止\\n构建环境：正式环境\\n项目名称：lms项目\\n模块名称：${currentBuild.projectName}\\n触发原因：${by[0].shortDescription}\\n开始构建时间：${startTime}\\n结束构建时间：${endTime}\\n持续构建时间：${second}s\\n详细信息：${BUILD_URL}"+'", "systemName":"Jenkins"}'
                def response = httpRequest acceptType: 'APPLICATION_JSON_UTF8', contentType: 'APPLICATION_JSON_UTF8', httpMode: 'POST', requestBody: body, url: "${phone_api_address}"

            }
        }
    }
}
```




