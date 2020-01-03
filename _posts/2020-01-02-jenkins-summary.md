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

&emsp;&emsp;通过Cygwin在Windows环境中安装并配置SSH的具体操作请[点击这里](https://my.oschina.net/u/658505/blog/616079)。

## 配置Linux服务器免密登录
&emsp;&emsp;实现系统的自动化部署需要配置Linux服务器的免密登录，`方便远程执行启动脚本、上传发布包和拉取备份包等。`配置免密登录的时候Jenkins的主从节点共用一个私钥，所有的部署主机共用一个公钥。

&emsp;&emsp;配置Linux服务器的免密登录的具体操作请[点击这里](https://www.cnblogs.com/leafinwind/p/10629547.html)。

# 环境搭建

## Jenkins安装及使用简介
&emsp;&emsp;Jenkins是一个开源的持续集成工具，网上资料很多也很全面。安装的方式也有很多，我这里展示的是直接运行war包的方式，其它安装方式请[点击这里](https://jenkins.io/zh/doc/book/getting-started/)参考官方文档。

### Jenkins安装与配置

#### 初始化Jenkins和管理员用户


## 配置Jenkins从节点

### 配置Linux从节点

### 配置Windows从节点

## Jenkins集成SonarQube

## Jenkins权限管理

## SonarQube权限管理

# 自动化部署实现

## 总体方案

### 流程图

### CI/CD过程