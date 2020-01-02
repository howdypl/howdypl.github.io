# 日志搜索模块配置
 >只需要在application.yml更改elasticsearch集群的ip地址即可
~~~
elasticsearch:
  ip: xxxxxx:9200,xxxxxx:9200,xxxxxx:9200
~~~
# 日志搜索模块的操作步骤
1. 选择索引（必须）
2. 选择搜索的时间段（可选）
3. 输入搜索的语句（可选）(e.g. status:200 AND extension:PHP)
4. 选择表格需要显示的字段（可选）
