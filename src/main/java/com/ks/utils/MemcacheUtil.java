package com.ks.utils;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.utils.AddrUtil;

public class MemcacheUtil {

    private static MemcachedClient memcachedClient = null;

    public  static  MemcachedClient getInstance(){

        try {
            if (memcachedClient == null) {
                MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(Constant.MEMCACHE_HOST_PORT), Constant.MEMCACHE_WEIGHT);
                memcachedClient = builder.build();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return  memcachedClient;
    }


}
