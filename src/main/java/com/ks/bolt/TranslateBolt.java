package com.ks.bolt;

import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.util.List;

/**
 * 转义
 */
public class TranslateBolt extends BaseBasicBolt{

    public void execute(Tuple tuple, BasicOutputCollector collector) {
           List<Object> list  =  tuple.getValues();
           String id = (String) list.get(0);
           String memberid = (String) list.get(1);
           String totalprice = (String) list.get(2);
           String youhui = (String) list.get(3);
           String sendpay = (String) list.get(4);
          //String createdate = (String) list.get(5);

           if ("0".equals(sendpay)){
                sendpay = "-1";
           }

          collector.emit(new Values(id,memberid,totalprice,youhui,sendpay));
          // System.out.println("list="+list.toString()+"  sendpay= "+sendpay);
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer) {

        declarer.declare(new Fields("id","memberid","totalprice","youhui","sendpay"));
    }
}
