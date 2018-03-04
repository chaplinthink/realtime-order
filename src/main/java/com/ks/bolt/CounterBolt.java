package com.ks.bolt;


import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Tuple;

public class CounterBolt extends BaseBasicBolt {

    private  static  long  counter = 0;

    public void execute(Tuple tuple, BasicOutputCollector collector) {

        System.out.println("msg = "+ tuple.getString(0)+"----counter = " + (counter++) );
    }


    public void declareOutputFields(OutputFieldsDeclarer declarer) {

    }
}
