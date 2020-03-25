package com.thebeastshop.flowtest.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.RetryNTimes;

public class CuratorTest2 {

    /** Zookeeper info */
    private static final String ZK_ADDRESS = "114.55.174.189:2181";
    private static final String ZK_PATH = "/zktest/ffff";

    public static void main(String[] args) throws Exception {
        // 1.Connect to zk
        CuratorFramework client = CuratorFrameworkFactory.newClient(
                ZK_ADDRESS,
                new RetryNTimes(10, 5000)
        );
        client.start();

//        removeNodeData(client);

//        createNode(client);

//        nodeListen(client);
//
        modifyNodeData(client);

    }

    private static void createNode(CuratorFramework client) throws Exception {
    	String data1 = "hello";
        print("create", ZK_PATH, data1);
        client.create().
                creatingParentsIfNeeded().
                forPath(ZK_PATH, data1.getBytes());
    }

    private static void getNodeData(CuratorFramework client) throws Exception {
    	print("ls", "/");
        print(client.getChildren().forPath("/"));
        print("get", ZK_PATH);
        print(client.getData().forPath(ZK_PATH));
    }

    private static void modifyNodeData(CuratorFramework client) throws Exception {
    	String data2 = "world for u";
        print("set", ZK_PATH, data2);
        client.setData().forPath(ZK_PATH, data2.getBytes());
        print("get", ZK_PATH);
        print(client.getData().forPath(ZK_PATH));
    }

    private static void removeNodeData(CuratorFramework client) throws Exception {
    	print("delete", "/zktest/dddd");
        client.delete().forPath("/zktest/dddd");
        print("ls", "/");
        print(client.getChildren().forPath("/"));
    }

    private static void nodeListen(CuratorFramework client) throws Exception {
    	final NodeCache cache = new NodeCache(client,ZK_PATH);
        cache.start();

        cache.getListenable().addListener(new NodeCacheListener() {

            @Override
            public void nodeChanged() throws Exception {
                byte[] res = cache.getCurrentData().getData();
                System.out.println("data: " + new String(res));
            }
        });
    }

    private static void childNodeListen(CuratorFramework client) throws Exception {
    	final PathChildrenCache cache = new PathChildrenCache(client,"/zktest",true);
        cache.start();

        cache.getListenable().addListener(new PathChildrenCacheListener() {

            @Override
            public void childEvent(CuratorFramework curator, PathChildrenCacheEvent event) throws Exception {
                switch (event.getType()) {
                case CHILD_ADDED:
                    System.out.println("add:" + event.getData().getPath() + ":" + new String(event.getData().getData()));
                    break;
                case CHILD_UPDATED:
                    System.out.println("update:" + event.getData().getPath() + ":" + new String(event.getData().getData()));
                    break;
                case CHILD_REMOVED:
                    System.out.println("remove:" + event.getData().getPath() + ":" + new String(event.getData().getData()));
                    break;
                default:
                    break;
                }
            }
        });
    }


    private static void print(String... cmds) {
        StringBuilder text = new StringBuilder("$ ");
        for (String cmd : cmds) {
            text.append(cmd).append(" ");
        }
        System.out.println(text.toString());
    }

    private static void print(Object result) {
        System.out.println(
                result instanceof byte[]
                    ? new String((byte[]) result)
                        : result);
    }

}
