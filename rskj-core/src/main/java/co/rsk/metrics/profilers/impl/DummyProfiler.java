package co.rsk.metrics.profilers.impl;


import co.rsk.metrics.profilers.Metric;
import co.rsk.metrics.profilers.Profiler;

public class DummyProfiler implements Profiler {

    @Override
    public Metric start(PROFILING_TYPE type) {
        return null;
    }

    @Override
    public void stop(Metric metric) {

    }


    @Override
    public void newBlock(long blockId, int trxQty){
    }

}
