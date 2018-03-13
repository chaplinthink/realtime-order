# realtime-order

## 目的

本项目主要针对具体的业务如订单信息，使用实时计算技术统计相关指标。

## 环境

- storm  1.1.1
- zk   3.4.10
- kafka 2.11-1.0.0
- memcached  1.5.4
- mysql  5.7.20
- jdk 1.8.0_144

**实时计算技术架构：**

strom + kafka+ memcached/redis + mysql + zk 锁

**启动相关服务：**

- zk 启动

在准备好相应的配置之后，可以直接通过zkServer.sh 这个脚本进行服务的相关操作

1. 启动ZK服务:       sh bin/zkServer.sh start
2. 查看ZK服务状态:    sh bin/zkServer.sh status
3. 停止ZK服务:       sh bin/zkServer.sh stop
4. 重启ZK服务:       sh bin/zkServer.sh restart

查看zk的数据结构：

```
./zkCli.sh  -server localhost:2181
```
>WatchedEvent state:SyncConnected type:None path:null
[zk: localhost:2181(CONNECTED) 0] ls /
[cluster, controller_epoch, controller, storm, brokers, zookeeper, admin, isr_change_notification, consumers, log_dir_event_notification, latest_producer_id_block, config]
[zk: localhost:2181(CONNECTED) 1] 

查看kafka brokers 的节点有几个：

ls /brokers/ids

[0]

- kafka　启动
```
nohup bin/kafka-server-start.sh config/server.properties &
```
创建topic

```
bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test
```

查看kafka 日志：

/tmp/kafka-logs

查看当前运行的topic:

```
bin/kafka-topics.sh --list --zookeeper localhost:2181
```

往topic:  test里写日志：

```
bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test
```

消费数据,会将数据打印在控制台上：

```
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test --from-beginning
```

from-beginning从topic最开始的数据，就是所有的数据，偏移量为0开始，所有数据输出来

看当前的数据把from-beginning去掉就ok 了。

- storm 启动

nohup bin/storm nimbus >> /dev/null &

nohup bin/storm supervisor >> /dev/null &

nohup bin/storm ui  >> /dev/null &

上面命令的意思是丢弃输出信息并且放到后台执行，稍微等一下，执行jps查看nimbus进程是否启动

**正常执行：**

nohup bin/storm nimbus   &

nohup bin/storm supervisor   &

nohup bin/storm ui  &

- memcached 启动 

做排重 用memcached.

```
nohup /usr/local/bin/memcached -d -m 2048  -u root  -l 127.0.0.1 -p 12121 -c 1024 -P /tmp/memcached.pid &
```

-m 内存 |  -p  端口号 | -c 连接数 | -P  pid的文件路径

## 业务

```
select
count(id)  # 有效订单量，
sum(totalPrice) #优惠前金额，
sum(totalPrize-youhui) #优惠后金额，
count(distinct memberid)  # 下单用户数，
case  when substring(sendpay,9,1) = '1' then 1 when substring(sendpay,9,1)='2' then 2 else -1 end  #  手机客户端下单标记
from  
realtime_orders
where
       createdate >='2014-04-19'
group by   
case  when substring(sendpay,9,1) = '1' then 1 when substring(sendpay,9,1)='2' then 2 else -1 end
```

## 整体数据处理流程

1.web 发送数据到kafka    ----order
       
2.storm 从kafka获取数据，进行分析

    a.首先storm 和kafka整合  ----引入插件
    
    b. 简单需要一个topology ，读数据，打印数据
    
    c. 通过业务，编写代码：

Map的key  = sendpay

value = count(distinct memberid)===>去重复===》key/value memcached (sendpay、date)===>sendpay

where 条件是**判断订单是否有效的东西** （**第一个bolt  check订单是否有效，根据message里面的日期字段进行判定 或者  ，message  字段是否缺失进行判断**）

group by  (**第二个bolt，进行数据sendpay的数据纠正**)

**第三个 bolt 数据分析，存储** 

count(id)  # 有效订单量，

sum(totalPrice) #优惠前金额，

sum(totalPrize-youhui) #优惠后金额，

count(distinct memberid)  # 下单用户数

3.将数据入库

message:

|订单号 |    用户id|    	原金额 |	优惠价|	标示字段|		下单时间|
|---|---|---|---|---|---|
|id |	memberid|  totalprice | youhui  |  sendpay | createdate|


## 程序打包与运行

打包：

运行：

编写好 CounterTopology 打包 放到 storm 的bin下

```
./storm  jar KafkaStormOptr.jar com.ks.topology.CounterTopology 
```

## 参考资料

1.Storm集群的安装配置

https://www.cnblogs.com/freeweb/p/5179410.html

2.Zookeeper常用命令
http://blog.csdn.net/xiaolang85/article/details/13021339/

3.storm基础概念
http://blog.csdn.net/wust__wangfan/article/details/50412695

4.Topology的并行度
http://blog.csdn.net/wust__wangfan/article/details/50417801

5.消息的可靠处理
http://blog.csdn.net/wust__wangfan/article/details/50419871

6.异或运算
https://baike.baidu.com/item/%E5%BC%82%E6%88%96/10993677?fr=aladdin

7.Kafka的基本结构和概念
http://blog.csdn.net/zuoanyinxiang/article/details/50890322

8.Kafka+Storm+HDFS整合实践
http://shiyanjun.cn/archives/934.html

9.Maven依赖中的scope详解
http://blog.csdn.net/kimylrong/article/details/50353161

10.linux awk命令详解
https://www.cnblogs.com/jacob-tian/p/6110606.html

11.storm集群启动/停止脚本
http://blog.csdn.net/zz657114506/article/details/54429965

12.linux标准输入输出2>&1
https://www.cnblogs.com/jacob-tian/p/6110606.html

13.storm 集成kafka时遇见的问题
http://blog.csdn.net/fenghuibian/article/details/54380263

14.JStorm/Storm的调试：本地运行模式
http://blog.csdn.net/u010010428/article/details/51746712

15.Linux下的Memcache安装
http://kimi.it/257.html

16.使用Xmemcached客户端操作Memcached缓存系统
http://blog.csdn.net/l569590478/article/details/51332322

17.memcached 常用命令及使用说明
https://www.cnblogs.com/wayne173/p/5652034.html

18.java之XMemcached使用及源码详解
http://blog.csdn.net/tang9140/article/details/43445511

XMemcached:
https://github.com/killme2008/xmemcached/wiki/%E5%BF%AB%E9%80%9F%E5%85%A5%E9%97%A8

19.将下载的jar安装到本地仓库 ，然后在项目pom.xml 里直接引用
http://blog.csdn.net/l1028386804/article/details/48440723

20.Ubuntu下安装MySQL及简单操作
http://www.linuxidc.com/Linux/2016-07/133128.htm

21.conn.setAutoCommit(true)和(false)的区别
http://blog.csdn.net/qq_26847293/article/details/48489999

22.mysql的安装：
http://www.linuxidc.com/Linux/2017-05/143864.

23.IntelliJ IDEA常用快捷键汇总
http://blog.csdn.net/wei83523408/article/details/60472168
