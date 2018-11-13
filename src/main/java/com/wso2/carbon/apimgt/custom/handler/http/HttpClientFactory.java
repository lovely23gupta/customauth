package com.wso2.carbon.apimgt.custom.handler.http;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpClientFactory {

    private static CloseableHttpClient client;

    private static PoolingHttpClientConnectionManager connPool = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientFactory.class);

    static{
        try
        {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    builder.build());

            Registry r = RegistryBuilder. create()
                    .register("https", sslsf).build();

            connPool = new PoolingHttpClientConnectionManager(r);
            // Increase max total connection to 200
            connPool.setMaxTotal(200);

            connPool.setDefaultMaxPerRoute(20);


            client = HttpClients.custom().
                    setConnectionManager(connPool).
                    setSSLSocketFactory(sslsf).build();
        }
        catch(Exception e){
            LOGGER.error("Error initiliazing HttpClientFactory :: ",e);
        }
    }

    public static CloseableHttpClient getHttpsClient() throws KeyManagementException, NoSuchAlgorithmException  {

        if (client != null) {
            return client;
        }
        throw new RuntimeException("Client is not initiliazed properly");

    }
    public static void releaseInstance() {
        client = null;
    }
}