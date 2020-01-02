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
<p><img src="https://howdypl.github.io/img/jenkins-framework.png" width = 100% height = 100% alt="平台架构图" title="平台架构图"></p>

# 前期准备
&emsp;&emsp;主要介绍为了完成Jenkins持续集成环境的搭建，并实现节点的高可用，前期需要做哪些准备。

## jdk安装
&emsp;&emsp;jdk主要提供Jenkins运行所需的环境，需要注意的一点是`在配置从节点的时候从节点的jdk版本要和主节点的保持一致。`

&emsp;&emsp;jdk安装及环境配置的具体操作请[点击这里](https://blog.csdn.net/pang_ping/article/details/80570011 "jdk安装及环境配置的具体操作")

## NFS挂载目录
&emsp;&emsp;进行挂载的主要目的是`保证各节点之间数据的一致性。`只需要挂载四个目录即可，分别是：
1. maven本地仓库的目录
2. 备份包存放的目录
3. 环境准备所需文件存放的目录
4. Jenkins工作空间所在的目录

&emsp;&emsp;Centos7安装配置NFS服务和挂载的具体操作请[点击这里](https://www.cnblogs.com/lixiuran/p/7117000.html "Centos7安装配置NFS服务和挂载")

## CYGWIN安装（针对Windows系统的部署主机）

# 环境搭建








# 自动化部署实现