package client.part1;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.params.HttpMethodParams;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class ClientThread implements Runnable {
    private final int startRange;
    private final int endRange;
    private final int startTime;
    private final int endTime;
    private SkierClient client;
    private int successCount;
    private int failCount;
    private final CountDownLatch latch;
    private final int numPost;

    public ClientThread(int startRange, int endRange, SkierClient client, CountDownLatch latch, int startTime, int endTime, int numPost) {
        this.startRange = startRange;
        this.endRange = endRange;
        this.client = client;
        this.latch = latch;
        this.startTime = startTime;
        this.endTime = endTime;
        this.numPost = numPost;
    }

    private void sendPostRequest(int skierID, int liftID, int time) {
        String url = String.format("http://%s/cs6650_hw1_war_exploded/skiers/%d/seasons/2019/days/1/skiers/123", this.client.getPort(), skierID);
        PostMethod method = new PostMethod(url);

        // Provide custom retry handler is necessary
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                new DefaultHttpMethodRetryHandler(5, false));

        NameValuePair[] data = {
                new NameValuePair("time", String.valueOf(time)),
                new NameValuePair("liftID", String.valueOf(liftID))
        };
        method.setRequestBody(data);
        try {
            // Execute the method.
            long startTime = System.currentTimeMillis();
            int statusCode = client.getHttpClient().executeMethod(method);
            long endTime   = System.currentTimeMillis();
            long latency = endTime - startTime;
            if (statusCode != HttpStatus.SC_CREATED) {
                System.err.println("Method failed: " + method.getStatusLine());
            }

            // Read the response body.
            byte[] responseBody = method.getResponseBody();

            // Deal with the response.
            // Use caution: ensure correct character encoding and is not binary data
            successCount += 1;
            //System.out.println(String.format("%s %d %d", new String(responseBody), latency, statusCode));
        } catch (HttpException e) {
            failCount += 1;
            System.err.println("Fatal protocol violation: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            failCount += 1;
            System.err.println("Fatal transport error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Release the connection.
            method.releaseConnection();
        }
    }

    public void run() {
        for (int i = 0; i < this.numPost; i++) {
            Random random = new Random();
            int skierID = random.nextInt(endRange - startRange + 1) + startRange;
            int liftID = Math.abs(random.nextInt());
            int time = random.nextInt(endTime - startTime + 1) + startTime;
            sendPostRequest(skierID, liftID, time);
        }
        latch.countDown();
        client.incrementSuccessCount(successCount);
        client.incrementFailCount(failCount);
    }
}
