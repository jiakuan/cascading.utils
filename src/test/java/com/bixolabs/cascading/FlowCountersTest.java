package com.bixolabs.cascading;

import java.util.Map;

import junit.framework.Assert;

import org.apache.hadoop.mapred.JobConf;
import org.junit.Test;

import cascading.flow.Flow;
import cascading.flow.FlowConnector;
import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Filter;
import cascading.operation.FilterCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.scheme.SequenceFile;
import cascading.tap.Lfs;
import cascading.tap.SinkMode;
import cascading.tap.Tap;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntryCollector;


public class FlowCountersTest {

    private enum FlowCountersTestEnum {
        TUPLE_COUNT,
        UNUSED_COUNT,
        BOGUS_COUNT
    }
    
    @SuppressWarnings("serial")
    private static class CountTuplesFunction extends BaseOperation<NullContext> implements Filter<NullContext> {
        
        @Override
        public boolean isRemove(FlowProcess flowProcess, FilterCall<NullContext> filterCall) {
            flowProcess.increment(FlowCountersTestEnum.TUPLE_COUNT, 1);
            return false;
        }
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void testCounters() throws Throwable {
        final Fields testFields = new Fields("user", "value");
        
        final int numDatums = 1;
        
        final String testDir = "build/test/FlowCountersTest/testCounters/";
        String in = testDir + "in";

        Lfs sourceTap = new Lfs(new SequenceFile(testFields), in, SinkMode.REPLACE);
        TupleEntryCollector write = sourceTap.openForWrite(new JobConf());
        
        for (int i = 0; i < numDatums; i++) {
            String username = "user-" + (i % 3);
            write.add(new Tuple(username, i));
        }
        
        write.close();

        Pipe pipe = new Pipe("test");
        pipe = new Each(pipe, new CountTuplesFunction());
        Tap sinkTap = new NullSinkTap(testFields);
        
        Flow flow = new FlowConnector().connect(sourceTap, sinkTap, pipe);
        Map<Enum, Long> counters = FlowCounters.run(flow, FlowCountersTestEnum.TUPLE_COUNT,
                        FlowCountersTestEnum.UNUSED_COUNT);
        
        Assert.assertEquals(numDatums, (long)counters.get(FlowCountersTestEnum.TUPLE_COUNT));
        Assert.assertEquals(0, (long)counters.get(FlowCountersTestEnum.UNUSED_COUNT));
        Assert.assertNull(counters.get(FlowCountersTestEnum.BOGUS_COUNT));
    }
}
