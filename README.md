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


WatchedEvent state:SyncConnected type:None path:null
[zk: localhost:2181(CONNECTED) 0] ls /
[cluster, controller_epoch, controller, storm, brokers, zookeeper, admin, isr_change_notification, consumers, log_dir_event_notification, latest_producer_id_block, config]
```

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

打包： mvn clean package

运行： 编写好 CounterTopology 打包 放到 storm 的bin下

```
./storm  jar KafkaStormOptr.jar com.ks.topology.CounterTopology 
```
