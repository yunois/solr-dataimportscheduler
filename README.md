# solr-dataimportscheduler
solr dataimport scheduler

1、data-config.xml

增加propertyWriter配置

<propertyWriter dateFormat="yyyy-MM-dd HH:mm:ss" type="org.apache.solr.handler.dataimport.ZKPropertiesForLastWriter" directory="data" filename="dataimport.properties" />


ZKPropertiesForLastWriter 功能：

解决根据原生增量导入可能丢失数据问题，可以每次增量的时候，往前推一定的时间。

如：

<dataConfig>
   <dataSource type="JdbcDataSource" jndiName="java:comp/env/jdbc/FEDERATEDDS"/>
  <propertyWriter dateFormat="yyyy-MM-dd HH:mm:ss" type="org.apache.solr.handler.dataimport.ZKPropertiesForLastWriter" directory="data" filename="dataimport.properties" />
  <document name="test01">
      <!--  field for news search, from member table-->
      <entity name="test01"
                  query="SELECT id, code,name from test01"
                  deltaImportQuery="SELECT id, code,name from test01 where id='${dih.delta.id}'"  
              deltaQuery="SELECT id from test01 WHERE update_date_time > '${dih.bcchannel.last_index_time}'"
     >
          <field column="id" name="id" />
          <field column="code" name="code" />
          <field column="name" name="name" />
     </entity>
    </document>
</dataConfig>

2、zk上指定core的dataimport.properties

before_second:往前推迟的数量，单位秒
last_index_time：最后更新时间，系统默认添加，每次系统自己记录

3、在solr cloud的conf目录，添加增量导入配置

conf/dataimport.properties 

#################################################
#       dataimport scheduler properties         #
#                                               #
#################################################
  
#  to sync or not to sync
#  1 - active; anything else - inactive
# 这里的配置不用修改
syncEnabled=1
  
#  which cores to schedule
#  in a multi-core environment you can decide which cores you want syncronized
#  leave empty or comment it out if using single-core deployment
#  修改成你所使用的core，我这里是我自定义的core：onlinepayorder
syncCores=test01_shard1_replica2
  
#  solr server name or IP address
#  [defaults to localhost if empty]
#  这个一般都是localhost不会变
server=172.1.1.2
  
#  solr server port
#  [defaults to 80 if empty]
#  安装solr的tomcat端口，如果你使用的是默认的端口，就不用改了，否则改成自己的端口就好了
port=8116
  
#  application name/context
#  [defaults to current ServletContextListener's context (app) name]
#  这里默认不改
webapp=/
  
#  URL params [mandatory]
#  remainder of URL
#  这里改成下面的形式，solr同步数据时请求的链接
params=/dataimport?command=delta-import&clean=false&commit=true
#  schedule interval
#  number of minutes between two runs
#  [defaults to 30 if empty]
#  这里是设置定时任务的，单位是秒，也就是多长时间你检测一次数据同步，根据项目需求修改
#  开始测试的时候为了方便看到效果，时间可以设置短一点
interval=30
  
#  重做索引的时间间隔，单位分钟，默认7200，即5天; 
#  为空,为0,或者注释掉:表示永不重做索引
#reBuildIndexInterval=7200
  
#  重做索引的参数
#reBuildIndexParams=/select?qt=/dataimport&command=full-import&clean=true&commit=true
  
#  重做索引时间间隔的计时开始时间，第一次真正执行的时间=reBuildIndexBeginTime+reBuildIndexInterval*60*1000；
#  两种格式：2012-04-11 03:10:00 或者  03:10:00，后一种会自动补全日期部分为服务启动时的日期
#reBuildIndexBeginTime=03:10:00
#  延迟执行时间 默认是30分钟
initialDelay=1
#  执行线程数 默认是10
threadPoolCount=10
