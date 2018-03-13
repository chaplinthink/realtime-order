package com.ks.bolt;

import com.ks.utils.JDBCUtil;
import com.ks.utils.MemcacheUtil;
import net.rubyeye.xmemcached.MemcachedClient;
import org.apache.commons.lang.StringUtils;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Tuple;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class SaveMysqlBolt extends BaseBasicBolt {

    private static Map<String,String> memberMap = null; //sendpay,counterMember
    private  static MemcachedClient client = null;

    public void execute(Tuple tuple, BasicOutputCollector collector) {
        List<Object> list  =  tuple.getValues();
        String id = (String) list.get(0);
        String memberid = (String) list.get(1);
        String totalprice = (String) list.get(2);
        String youhui = (String) list.get(3);
        String sendpay = (String) list.get(4);
        //String createdate = (String) list.get(5);

        saveCounterMember(memberid,sendpay,totalprice,youhui); //记录独立用户数
    }

    private void saveCounterMember(String memberid,String sendpay,String totalprice,String youhui){

        try {
            //String key = sendpay + "_" +memberid + "_"+createdate;
            String key = sendpay + "_" + memberid;
            String vx = client.get(key);
            boolean isHasMem = false;

            if(StringUtils.isNotEmpty(vx)){
                isHasMem = true;
            }

            saveMap(sendpay,isHasMem,totalprice,youhui);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void saveMap(String sendpay, boolean isHasMem, String totalprice, String youhui) {
        if(checkMap()){
            String value = memberMap.get(sendpay); //value = count(id),sum(totalprice),sum(totalprice-youhui),count(distinct memberid)
            if (value!=null) {
                String vals[] = value.split(",");
                int id_num = Integer.valueOf(vals[0]) + 1;
                double tp = Double.valueOf(vals[1]) + Double.valueOf(totalprice);
                double etp = Double.valueOf(vals[2]) + (Double.valueOf(totalprice) - Double.valueOf(youhui));
                int counter_member = Integer.valueOf(vals[3]) + (isHasMem ? 0 : 1);
            }else{
                value = 1+","+ totalprice+","+(Double.valueOf(totalprice) - Double.valueOf(youhui))+","+(isHasMem ? 0 : 1);
            }
            memberMap.put(sendpay,value);
        }
    }

    private boolean checkMap() {
        if (memberMap==null){
            memberMap = new HashMap<String, String>();
        }
        return  true;
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer) {

    }

    @Override
    public void prepare(Map stormConf, TopologyContext context) {
        memberMap = new HashMap<String, String>();
        client = MemcacheUtil.getInstance();
        Timer timer =  new  Timer();
        timer.schedule(new SaveMysqlBolt.cacheTimer(),new Date(),5000);
    }

    class  cacheTimer extends TimerTask{

        public void run() {
            Map<String,String> tmpMap = new HashMap<String, String>();
            //将memberMap所有数据copy过来
            tmpMap.putAll(memberMap);
            memberMap = new HashMap<String, String>();
            saveMysql(tmpMap);
        }
    }

    private void saveMysql(Map<String, String> tmpMap) {

        try {
            Connection conn = JDBCUtil.getConnectionByJDBC();
            for (Map.Entry<String, String> entry : tmpMap.entrySet()) {
                //id,order_nums,p_total_price,y_total_price,order_members,sendpay
                String key = entry.getKey();
                String value = entry.getValue();
                String vals[] = value.split(",");
                int id_num = Integer.valueOf(vals[0]);
                double tp = Double.valueOf(vals[1]);
                double etp = Double.valueOf(vals[2]);
                int counter_member = Integer.valueOf(vals[3]);

                Statement stmt = conn.createStatement();
                String sql = "select id,order_nums,p_total_price,y_total_price,order_members from total_order where sendpay='"+key+"'";
                ResultSet set = stmt.executeQuery(sql);

                int id = 0;
                int order_nums = 0;
                double p_total_price = 0;
                double y_total_price =0;
                int order_member = 0;

                while (set.next()){
                    id = set.getInt(0);
                    order_nums = set.getInt(1);
                    p_total_price = set.getDouble(2);
                    y_total_price = set.getDouble(3);
                    order_member = set.getInt(4);
                }

                order_nums += id_num;
                p_total_price += tp;
                y_total_price += etp;
                order_member += counter_member;

                StringBuffer  stringBuffer = new StringBuffer();
                
                if(id==0){ //insert
                     stringBuffer.append("insert into total_order(order_nums,p_total_price,y_total_price,order_member,sendpay) values(")
                             .append(order_nums+","+p_total_price+","+y_total_price+","+order_member+",'"+key+"')");
                }else{ //update
                    stringBuffer.append("update total_order　set order_nums="+order_nums)
                    .append(",p_total_price="+p_total_price)
                    .append(",y_total_price="+y_total_price)
                    .append(",order_member="+order_member)
                    .append(" where id="+id);

                }

            }
        }catch (Exception e){

        }
    }
}
