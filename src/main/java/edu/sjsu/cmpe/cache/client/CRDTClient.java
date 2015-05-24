package edu.sjsu.cmpe.cache.client;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class CRDTClient implements CRDTInterface {

    private ConcurrentHashMap<String, CacheServiceInterface> Server;
    private ArrayList<String> successServer;
    private ConcurrentHashMap<String, ArrayList<String>> dictResults;

    private static CountDown countDown;

    public CRDTClient() {

        Server = new ConcurrentHashMap<String, CacheServiceInterface>(3);
        CacheServiceInterface cache0 = new DistributedCacheService("http://localhost:3000", this);
        CacheServiceInterface cache1 = new DistributedCacheService("http://localhost:3001", this);
        CacheServiceInterface cache2 = new DistributedCacheService("http://localhost:3002", this);
        Server.put("http://localhost:3000", cache0);
        Server.put("http://localhost:3001", cache1);
        Server.put("http://localhost:3002", cache2);
    }

    @Override
    public void putFailed(Exception e) {
        System.out.println("The request has failed");
        countDown.countDown();
    }

    @Override
    public void putCompleted(HttpResponse<JsonNode> response, String serverUrl) {
        int getCode = response.getCode();
        System.out.println("HTTP code =>[" + getCode + "] Response Complete! server =>" + serverUrl);
        successServer.add(serverUrl);
        countDown.countDown();
    }

    @Override
    public void getFailed(Exception e) {
        System.out.println("The request has failed");
        countDown.countDown();
    }

    @Override
    public void getCompleted(HttpResponse<JsonNode> response, String serverUrl) {

        String value = null;
        if (response != null && response.getCode() == 200) {
            value = response.getBody().getObject().getString("value");
            System.out.println("Value from server [ " + serverUrl + " ] =>" + value);
            ArrayList ServerWithValue = dictResults.get(value);
            if (ServerWithValue == null) {
                ServerWithValue = new ArrayList(3);
            }
            ServerWithValue.add(serverUrl);

            dictResults.put(value, ServerWithValue);
        }

        countDown.countDown();
    }


    public boolean put(long key, String value) throws InterruptedException {
        successServer = new ArrayList(Server.size());
        countDown = new CountDownLatch(Server.size());

        for (CacheServiceInterface cache : Server.values()) {
            cache.put(key, value);
        }

        countDown.await();

        boolean isSuccess = Math.round((float) successServer.size() / Server.size()) == 1;

        if (!isSuccess) {
            delete(key, value);
        }
        return isSuccess;
    }

    public void delete(long key, String value) {

        for (final String serverUrl : successServer) {
            CacheServiceInterface server = Server.get(serverUrl);
            server.delete(key);
        }
    }

    public String get(long key) throws InterruptedException {
        dictResults = new ConcurrentHashMap<String, ArrayList<String>>();
        countDown = new CountDownLatch(Server.size());

        for (final CacheServiceInterface server : Server.values()) {
            server.get(key);
        }
        countDown.await();

        String rightValue = dictResults.keys().nextElement();

        if (dictResults.keySet().size() > 1 || dictResults.get(rightValue).size() != Server.size()) {

            ArrayList<String> maxValues = maxKeyForTable(dictResults);

            if (maxValues.size() == 1) {

                rightValue = maxValues.get(0);

                ArrayList<String> repairServer = new ArrayList(Server.keySet());
                repairServer.removeAll(dictResults.get(rightValue));
                for (String serverUrl : repairServer) {

                    System.out.println(" Repairing [" + serverUrl + "]  value: " + rightValue);
                    CacheServiceInterface server = Server.get(serverUrl);
                    server.put(key, rightValue);

                }

            } else {

            }
        }

        return rightValue;

    }

    public ArrayList<String> maxKeyForTable(ConcurrentHashMap<String, ArrayList<String>> table) {
        ArrayList<String> maxKeys = new ArrayList<String>();
        int maxValue = -1;
        for (Map.Entry<String, ArrayList<String>> entry : table.entrySet()) {
            if (entry.getValue().size() > maxValue) {
                maxKeys.clear();
                maxKeys.add(entry.getKey());
                maxValue = entry.getValue().size();
            } else if (entry.getValue().size() == maxValue) {
                maxKeys.add(entry.getKey());
            }
        }
        return maxKeys;
    }
}
