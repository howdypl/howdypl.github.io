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