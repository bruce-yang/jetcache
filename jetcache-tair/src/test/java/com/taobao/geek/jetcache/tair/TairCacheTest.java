/**
 * Created on  13-09-24 18:47
 */
package com.taobao.geek.jetcache.tair;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.taobao.geek.jetcache.CacheConfig;
import com.taobao.geek.jetcache.CacheResult;
import com.taobao.geek.jetcache.CacheResultCode;
import com.taobao.tair.DataEntry;
import com.taobao.tair.Result;
import com.taobao.tair.ResultCode;
import com.taobao.tair.TairManager;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author yeli.hl
 */
public class TairCacheTest {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    private TairCache cache;
    private TairManager tairManager;

    @Before
    public void setup() {
        tairManager = context.mock(TairManager.class);
        cache = new TairCache();
        cache.setNamespace(20);
        cache.setTairManager(tairManager);
    }

    @Test
    public void testGet() {
        context.checking(new Expectations() {
            {
                oneOf(tairManager).get(20, "SA1K1");
                will(returnValue(new Result(ResultCode.DATANOTEXSITS)));
                oneOf(tairManager).get(20, "SA2K2");
                will(returnValue(new Result(ResultCode.DATAEXPIRED)));
                oneOf(tairManager).get(20, "SA3K3");
                will(returnValue(new Result(ResultCode.PARTSUCC)));
                oneOf(tairManager).get(20, "SA4K4");
                will(throwException(new RuntimeException()));
                oneOf(tairManager).get(20, "SA5K5");
                DataEntry dn = new DataEntry(cache.encode(new TairValue(System.currentTimeMillis() + 100000, "V")));
                will(returnValue(new Result<DataEntry>(ResultCode.SUCCESS, dn)));
                oneOf(tairManager).get(20, "SA6K6");
                DataEntry dn2 = new DataEntry(cache.encode(new TairValue(System.currentTimeMillis() -1, "V")));
                will(returnValue(new Result<DataEntry>(ResultCode.SUCCESS, dn2)));
            }
        });
        Assert.assertEquals(CacheResultCode.NOT_EXISTS, cache.get(null, "SA1", "K1").getResultCode());
        Assert.assertEquals(CacheResultCode.EXPIRED, cache.get(null, "SA2", "K2").getResultCode());
        Assert.assertEquals(CacheResultCode.FAIL, cache.get(null, "SA3", "K3").getResultCode());
        Assert.assertEquals(CacheResultCode.FAIL, cache.get(null, "SA4", "K4").getResultCode());
        CacheResult result = cache.get(null, "SA5", "K5");
        Assert.assertEquals(CacheResultCode.SUCCESS, result.getResultCode());
        Assert.assertEquals("V", result.getValue());

        Assert.assertEquals(CacheResultCode.EXPIRED, cache.get(null, "SA6", "K6").getResultCode());
    }

    @Test
    public void testPut() {
        final CacheConfig cc = new CacheConfig();
        cc.setExpire(200);
        context.checking(new Expectations() {
            {
                oneOf(tairManager).put(with(20), with("SA1K1"), with(any(byte[].class)), with(0), with(cc.getExpire()));
                will(returnValue(ResultCode.SUCCESS));
                oneOf(tairManager).put(with(20), with("SA2K2"), with(any(byte[].class)), with(0), with(cc.getExpire()));
                will(returnValue(ResultCode.CONNERROR));
            }
        });
        Assert.assertEquals(CacheResultCode.SUCCESS, cache.put(cc, "SA1", "K1", "V1"));
        Assert.assertEquals(CacheResultCode.FAIL, cache.put(cc, "SA2", "K2", "V2"));
    }
}
