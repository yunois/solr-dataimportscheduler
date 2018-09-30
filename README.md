# solr-dataimportscheduler
solr dataimport scheduler

1、data-config.xml

增加propertyWriter配置

<propertyWriter dateFormat="yyyy-MM-dd HH:mm:ss" type="org.apache.solr.handler.dataimport.ZKPropertiesForLastWriter" directory="data" filename="dataimport.properties" />


ZKPropertiesForLastWriter 功能：

解决根据原生增量导入可能丢失数据问题，可以每次增量的时候，往前推一定的时间。

如：
<pre>
&lt;dataConfig&gt;
   &lt;dataSource type=&quot;JdbcDataSource&quot; jndiName=&quot;java:comp/env/jdbc/FEDERATEDDS&quot;/&gt;
  &lt;propertyWriter dateFormat=&quot;yyyy-MM-dd HH:mm:ss&quot; type=&quot;org.apache.solr.handler.dataimport.ZKPropertiesForLastWriter&quot; directory=&quot;data&quot; filename=&quot;dataimport.properties&quot; /&gt;
  &lt;document name=&quot;test01&quot;&gt;
      &lt;!--  field for news search, from member table--&gt;
      &lt;entity name=&quot;test01&quot;
                  query=&quot;SELECT id, code,name from test01&quot;
                  deltaImportQuery=&quot;SELECT id, code,name from test01 where id=&#x27;${dih.delta.id}&#x27;&quot;  
              deltaQuery=&quot;SELECT id from test01 WHERE update_date_time &gt; &#x27;${dih.bcchannel.last_index_time}&#x27;&quot;
     &gt;
          &lt;field column=&quot;id&quot; name=&quot;id&quot; /&gt;
          &lt;field column=&quot;code&quot; name=&quot;code&quot; /&gt;
          &lt;field column=&quot;name&quot; name=&quot;name&quot; /&gt;
     &lt;/entity&gt;
    &lt;/document&gt;
&lt;/dataConfig&gt;
</pre>

2、zk上指定core的dataimport.properties
<pre>
before_second:往前推迟的数量，单位秒
last_index_time：最后更新时间，系统默认添加，每次系统自己记录
</pre>

3、在solr cloud的conf目录，添加增量导入配置

conf/dataimport.properties 

<pre>


#################################################
#       dataimport scheduler properties         #
#                                               #
#################################################
#  to sync or not to sync
#  1 - active; anything else - inactive
syncEnabled=1
#  which cores to schedule
#  in a multi-core environment you can decide which cores you want syncronized
#  leave empty or comment it out if using single-core deployment
#  修改成你所使用的core
syncCores=test01
#  solr server name or IP address
#  [defaults to localhost if empty]
#  这个一般都是localhost不会变
server=172.1.1.2
#  solr server port
#  [defaults to 80 if empty]
#  安装solr的tomcat端口，如果你使用的是默认的端口，就不用改了，否则改成自己的端口就好了
port=8116
#  application name/context
#  [defaults to current ServletContextListener&#x27;s context (app) name]
#  这里默认不改
webapp=/
  
#  URL params [mandatory]
#  remainder of URL
#  这里改成下面的形式，solr同步数据时请求的链接
params=/dataimport?command=delta-import&amp;clean=false&amp;commit=true
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
#reBuildIndexParams=/select?qt=/dataimport&amp;command=full-import&amp;clean=true&amp;commit=true
  
#  重做索引时间间隔的计时开始时间，第一次真正执行的时间=reBuildIndexBeginTime+reBuildIndexInterval*60*1000；
#  两种格式：2012-04-11 03:10:00 或者  03:10:00，后一种会自动补全日期部分为服务启动时的日期
#reBuildIndexBeginTime=03:10:00
#  延迟执行时间 默认是30分钟
initialDelay=1
#  执行线程数 默认是10
threadPoolCount=10

</pre>


4、web.xml

<pre>
&lt;listener&gt;
        &lt;listener-class>org.apache.solr.handler.dataimport.scheduler.ApplicationListener&lt;/listener-class&gt;
&lt;/listener&gt;
</pre>
