package client.part2;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.MAX_PRIORITY;

public class SkierClient {
    final static private int DEFAULT_LIFTS = 40;
    final static private int DEFAULT_RUNS = 10;
    private final int numThreads;
    private final int numSkiers;
    private final int numLifts;
    private final int numRuns;
    private final String port;
    private int successCount;
    private int failCount;
    private final HttpClient client;
    private ExecutorService pool;
    private List<Long> latencyList;
    private final PrintWriter writer;

    synchronized public void writeToFile(String message) {
        writer.println(message);
    }

    synchronized public void incrementSuccessCount(int count) {
        successCount += count;
    }

    synchronized public void incrementFailCount(int count) {
        failCount += count;
    }

    synchronized public int getSuccessCount() {
        return this.successCount;
    }

    synchronized public int getFailCount() {
        return this.failCount;
    }

    synchronized public void addLatency(long latency) {
        this.latencyList.add(latency);
    }

    public void shutDownPool() {
        pool.shutdown();
        while (!pool.isTerminated()) {
            try {
                pool.awaitTermination(MAX_PRIORITY, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.writer.close();
    }

    public SkierClient(int numThreads, int numSkiers, int numLifts, int numRuns, String port) throws FileNotFoundException, UnsupportedEncodingException {
        this.numThreads = numThreads;
        this.numSkiers = numSkiers;
        this.numLifts = numLifts;
        this.numRuns = numRuns;
        this.port = port;
        MultiThreadedHttpConnectionManager connectionManager =
                new MultiThreadedHttpConnectionManager();
        this.client = new HttpClient(connectionManager);
        this.pool = Executors.newFixedThreadPool(numThreads + numThreads / 2);
        this.latencyList = new ArrayList<>();
        this.writer = new PrintWriter(String.format("%s.csv", numThreads), "UTF-8");
    }

    public HttpClient getHttpClient() {
        return this.client;
    }
    
    public int getNumThreads() {
        return this.numThreads;
    }

    public int getNumSkiers() {
        return this.numSkiers;
    }

    public int getNumLifts() {
        return this.numLifts;
    }

    public int getNumRuns() {
        return this.numRuns;
    }

    public String getPort() {
        return this.port;
    }

    synchronized public double getMeanResponseTime() {
        return (double) latencyList.stream().mapToLong(a -> a).sum() / latencyList.size();
    }

    synchronized public long getTotalResponseTime() {
        return latencyList.stream().mapToLong(a -> a).sum();
    }

    synchronized public double getMedianResponseTime() {
        Collections.sort(latencyList);
        if (latencyList.size() % 2 == 0) {
            return ((double)latencyList.get(latencyList.size() / 2) + (double)latencyList.get(latencyList.size() / 2 - 1)) / 2;
        } else {
            return ((double)latencyList.get(latencyList.size() / 2));
        }
    }

    synchronized public long getP99() {
        Collections.sort(latencyList);
        return latencyList.get((int) (latencyList.size() * 0.99));
    }

    synchronized public long getMaxResponseTime() {
        Collections.sort(latencyList, Collections.reverseOrder());
        return latencyList.get(0);
    }

    private void startClient(int threadCount, int startTime, int endTime, int numPost) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch((int)Math.ceil(threadCount / 10f));
        for (int i = 0; i < threadCount; i++) {
            ClientThread clientThread = new ClientThread(i * numSkiers / threadCount + 1,
                    (i + 1) * numSkiers / threadCount, this, latch, startTime, endTime, numPost);
            pool.execute(clientThread);
        }
        latch.await();
    }

    public static void main(String[] args) throws InterruptedException, FileNotFoundException, UnsupportedEncodingException {
        // check if arguments are valid
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("specified arguments must have a value");
        }
        int threads = 0;
        int skiers = 0;
        int lifts = DEFAULT_LIFTS;
        int runs = DEFAULT_RUNS;
        String port = null;
        for (int i = 0; i < args.length; i += 2) {
            String argument = args[i];
            String value = args[i + 1];
            if ("-numThreads".equals(argument)) {
                if (Integer.valueOf(value) <= 0 || Integer.valueOf(value) > 256) {
                    throw new IllegalArgumentException("numThreads - max 256");
                } else {
                    threads = Integer.valueOf(value);
                }
            } else if ("-numSkiers".equals(argument)) {
                if (Integer.valueOf(value) <= 0 || Integer.valueOf(value) > 100000) {
                    throw new IllegalArgumentException("numSkiers - max 100000");
                } else {
                    skiers = Integer.valueOf(value);
                }
            } else if ("-numLifts".equals(argument)) {
                if (Integer.valueOf(value) < 5 || Integer.valueOf(value) > 60) {
                    throw new IllegalArgumentException("numLifts - range 5-60, default 40");
                } else {
                    lifts = Integer.valueOf(value);
                }
            } else if ("-numRuns".equals(argument)) {
                if (Integer.valueOf(value) <= 0 || Integer.valueOf(value) > 20) {
                    throw new IllegalArgumentException("numRuns - default 10, max 20");
                } else {
                    runs = Integer.valueOf(value);
                }
            } else if ("-address".equals(argument)) {
                port = value;
            } else {
                throw new IllegalArgumentException("Invalid argument name or value");
            }
        }
        if (threads == 0) {
            throw new IllegalArgumentException("Must specify numThreads");
        }
        if (skiers == 0) {
            throw new IllegalArgumentException("Must specify numSkiers");
        }
        if (port == null || port.length() == 0) {
            throw new IllegalArgumentException("Must specify port");
        }
        SkierClient client = new SkierClient(threads, skiers, lifts, runs, port);
        long start = System.currentTimeMillis();
        // phase 1
        int phaseOneThreads = threads / 4;
        int startTime = 1;
        int endTime = 90;
        int numPost = (runs / 5) * (skiers / phaseOneThreads);
        //System.out.println(numPost * phaseOneThreads);
        client.startClient(phaseOneThreads, startTime, endTime, numPost);
        // phase 2
        int phaseTwoThreads = threads;
        startTime = 91;
        endTime = 360;
        numPost = (int) ((runs * 0.6) * (skiers / phaseTwoThreads));
        //System.out.println(numPost * phaseTwoThreads);
        client.startClient(phaseTwoThreads, startTime, endTime, numPost);
        // phase 3
        int phaseThreeThreads = threads / 4;
        startTime = 361;
        endTime = 420;
        numPost = runs / 10;
        //System.out.println(numPost * phaseThreeThreads);
        client.startClient(phaseThreeThreads, startTime, endTime, numPost);
        // wait for all thread to complete
        client.shutDownPool();
        // Calculate the wall time.
        long duration = System.currentTimeMillis() - start;
        System.out.println("Successful requests sent " + client.getSuccessCount());
        System.out.println("Unsuccessful requests " + client.getFailCount());
        System.out.println("WallTime " + duration + "ms");
        System.out.println("Mean reponse time " + client.getMeanResponseTime());
        System.out.println("Median reponse time " + client.getMedianResponseTime());
        System.out.println("Total throughput per ms " + ((client.getSuccessCount() + client.getFailCount()) / (float)duration));
        System.out.println("p99 " + client.getP99());
        System.out.println("Max response time " + client.getMaxResponseTime());
    }
}
