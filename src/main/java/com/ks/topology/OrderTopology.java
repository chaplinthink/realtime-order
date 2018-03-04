package com.ks.topology;

import com.google.common.collect.ImmutableList;
import com.ks.bolt.CheckOrderBolt;
import com.ks.bolt.SaveMysqlBolt;
import com.ks.bolt.TranslateBolt;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.kafka.*;
import org.apache.storm.spout.SchemeAsMultiScheme;
import org.apache.storm.topology.TopologyBuilder;

public class OrderTopology {
    public static void main(String[] args)  {

        try {

            String KafkaZookeeper = "127.0.0.1:2181";
            BrokerHosts brokerHosts = new ZkHosts(KafkaZookeeper);
            //zkRoot,id需要手动在zk 里创建
            SpoutConfig kafkaConfig = new SpoutConfig(brokerHosts, "order", "/order", "id");
            kafkaConfig.scheme = new SchemeAsMultiScheme(new StringScheme());
            kafkaConfig.zkServers = ImmutableList.of("127.0.0.1");
            kafkaConfig.zkPort = 2181;

            TopologyBuilder builder = new TopologyBuilder();
            //设并行度为2，即2个Executor，每个executor下面有一个Task
            builder.setSpout("spout", new KafkaSpout(kafkaConfig), 2);
            builder.setBolt("check", new CheckOrderBolt(),1).shuffleGrouping("spout");
            builder.setBolt("translate", new TranslateBolt(),1).shuffleGrouping("check");
            builder.setBolt("save", new SaveMysqlBolt(),1).shuffleGrouping("translate");

            Config config = new Config();
            config.setDebug(true);

            if (args != null && args.length > 0) {
                config.setNumWorkers(2);  //使用两个工作进程
                StormSubmitter.submitTopology(args[0], config, builder.createTopology());
            } else {
                config.setMaxTaskParallelism(3);
                LocalCluster localCluster = new LocalCluster();
                localCluster.submitTopology("special-topology", config, builder.createTopology());
                Thread.sleep(500000);
                localCluster.shutdown();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
