package com.wso2.carbon.apimgt.custom.handler;

import com.wso2.carbon.apimgt.custom.handler.http.HttpClientFactory;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

public class HttpClientFactoryTest {
    @Test
    public void getHttpsClient() throws Exception {
        CloseableHttpClient apacheClient = HttpClientFactory.getHttpsClient();


        HttpGet httpGet = new HttpGet("https://localhost:9443/token-service/token?clientId=sadasda&clientSecret=dsasdaad");
        RequestConfig defaultRequestConfig = RequestConfig.custom().setConnectTimeout(30000).setSocketTimeout(30000)
                .build();
        httpGet.setConfig(defaultRequestConfig);
        try (CloseableHttpResponse response = apacheClient.execute(httpGet)){
            String responseString = IOUtils.toString(response.getEntity().getContent());
            System.out.println(responseString);

            JSONParser parser = new JSONParser();
            JSONObject body = (JSONObject) parser.parse(responseString);

            String token = (String) body.get("token");
            System.out.println(token);
        }
    }
}