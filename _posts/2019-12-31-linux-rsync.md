---
layout: post
title:  "Linux通过Rsync+sersync实现数据实时同步"
date:  2019-12-31 14:34:00
categories: Linux
tags: [Linx, Rsync]
---
# 简介
由于项目需求需要对数据进行实时备份，因此在网上调研了一下实现数据实时备份的解决方案，在调研中发现主要的方案有三种：
1. Rsync+sersync实现数据实时同步
2. Rsync+inotify-tools实现数据实时同步
3. NFS挂载目录（适用于内网环境）


<!-- more -->