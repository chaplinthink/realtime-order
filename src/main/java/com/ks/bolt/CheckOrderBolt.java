package com.ks.bolt;

import com.ks.utils.DateUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

public class CheckOrderBolt extends BaseBasicBolt {

    public void execute(Tuple tuple, BasicOutputCollector collector) {
        String data = tuple.getString(0);
        if(data!=null&&data.length()>0){
            String [] values = data.split("\t");
         /**   message:
            订单号      用户id   	原金额 	优惠价	标示字段		下单时间
            id		memberid  totalprice  youhui    sendpay          createdate
          */
            if(values.length==6){
                String id = values[0];
                String memberid = values[1];
                String totalprice = values[2];
                String youhui = values[3];
                String sendpay = values[4];
                String createdate = values[5];

                if (StringUtils.isNotEmpty(id)&&StringUtils.isNotEmpty(memberid)&&StringUtils.isNotEmpty(totalprice)){
                    if (DateUtil.isDate(createdate,"2014-04-19")){
                        //数据有效，提交到下游
                        collector.emit(new Values(id,memberid,totalprice,youhui,sendpay));
                    }
                }
            }
        }
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        //声明发送数据的结构
        declarer.declare(new Fields("id","memberid","totalprice","youhui","sendpay"));
    }
}
