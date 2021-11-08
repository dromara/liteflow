package com.yomahub.liteflow.asynctool.test.seqwork;

import com.yomahub.liteflow.asynctool.executor.Async;
import com.yomahub.liteflow.asynctool.test.seqwork.work.*;
import com.yomahub.liteflow.asynctool.wrapper.WorkerWrapper;
import org.junit.Test;

public class SeqWorkTest {

    @Test
    public void test1(){
        SeqWork1 seqWork1 = new SeqWork1();
        SeqWork2 seqWork2 = new SeqWork2();
        SeqWork3 seqWork3 = new SeqWork3();

        Callback1 callback1 = new Callback1();
        Callback2 callback2 = new Callback2();
        Callback3 callback3 = new Callback3();

        WorkerWrapper<String, String> workerWrapper1 = new WorkerWrapper.Builder<String, String>()
                .worker(seqWork1)
                .callback(callback1)
                .param("param1")
                .build();

        WorkerWrapper<String, String> workerWrapper2 = new WorkerWrapper.Builder<String, String>()
                .worker(seqWork2)
                .callback(callback2)
                .param("param2")
                .depend(workerWrapper1)
                .build();

        WorkerWrapper<String, String> workerWrapper3 = new WorkerWrapper.Builder<String, String>()
                .worker(seqWork3)
                .callback(callback3)
                .param("param3")
                .depend(workerWrapper2)
                .build();

        try{
            boolean flag = Async.beginWork(3500,workerWrapper1);
            System.out.println(workerWrapper3.getWorkResult().getResultState());
            System.out.println(flag);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            Async.shutDown();
        }
    }

    @Test
    public void test2(){
        SeqWork1 seqWork1 = new SeqWork1();
        SeqWork2 seqWork2 = new SeqWork2();
        SeqWork3 seqWork3 = new SeqWork3();

        Callback1 callback1 = new Callback1();
        Callback2 callback2 = new Callback2();
        Callback3 callback3 = new Callback3();

        WorkerWrapper<String, String> workerWrapper1 = new WorkerWrapper.Builder<String, String>()
                .worker(seqWork1)
                .callback(callback1)
                .param("param1")
                .build();

        WorkerWrapper<String, String> workerWrapper2 = new WorkerWrapper.Builder<String, String>()
                .worker(seqWork2)
                .callback(callback2)
                .param("param2")
                .depend(workerWrapper1)
                .build();

        WorkerWrapper<String, String> workerWrapper3 = new WorkerWrapper.Builder<String, String>()
                .worker(seqWork3)
                .callback(callback3)
                .param("param3")
                .depend(workerWrapper2)
                .build();

        try{
            boolean flag = Async.beginWork(2500,workerWrapper1);
            System.out.println(workerWrapper3.getWorkResult().getResultState());
            System.out.println(flag);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            Async.shutDown();
        }
    }
}
