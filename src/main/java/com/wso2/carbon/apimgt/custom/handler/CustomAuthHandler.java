package com.wso2.carbon.apimgt.custom.handler;

import org.apache.axis2.Constants;
import com.citrix.ccauth.*;
import com.citrix.ccauth.net.RequestMessage;
import com.citrix.ccauth.net.RequestMethod;
import com.citrix.ccauth.security.crypto.RSASigner;
import com.citrix.ccauth.security.crypto.Signer;
import com.wso2.carbon.apimgt.custom.handler.exception.CustomHandlerException;
import com.wso2.carbon.apimgt.custom.handler.http.HttpClientFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.AbstractHandler;
import org.bouncycastle.cert.ocsp.Req;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class CustomAuthHandler extends AbstractHandler implements ManagedLifecycle {
    private static final Log log = LogFactory.getLog(CustomAuthHandler.class);

    private String tokenServerUrl;
    private String tokenPrefix;

    public boolean handleRequest(MessageContext messageContext) {
        long starttime = System.nanoTime();
        log.info("this is the start time for CPET" + starttime);
        String method = (String) (((Axis2MessageContext)messageContext).getAxis2MessageContext().getProperty(
                Constants.Configuration.HTTP_METHOD));
        if (method.equalsIgnoreCase("OPTIONS")) {
            log.info("identified option call");
            return true;
        } else {
            log.info("identified " + method);
        }
        try {
/*            RelayUtils.buildMessage(((Axis2MessageContext) messageContext)
                    .getAxis2MessageContext(), false);

            String jsonPayloadToString = JsonUtil
                    .jsonPayloadToString(((Axis2MessageContext) messageContext)
                            .getAxis2MessageContext());

            log.debug("body: " + jsonPayloadToString);

            JSONParser parser = new JSONParser();
            JSONObject body = (JSONObject) parser.parse(jsonPayloadToString);*/

            log.info("this is the message context values" + messageContext);

            Map headers = (Map) ((Axis2MessageContext) messageContext).getAxis2MessageContext().
                    getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            String clientId = (String) headers.get("clientId");
            String clientSecret = (String) headers.get("clientSecret");
            log.info("this is the client Id" + clientId);
            log.info("this is the client Secret " + clientSecret);
            //adding this code for temporary purpose, need to modify the API
            headers.remove(clientId);
            headers.remove(clientSecret);
            String token = generateBearerToken(clientId, clientSecret);
            log.info("this is the token" + token);
            boolean isTokenValid = AuthorizeBearerToken(token.toString(),"ptipkmndeg54");
            if(isTokenValid) {
                headers.put("Authorization", token);
            }
            else
            {
                log.error("please enter the valid token");
            }
            log.info("this is the header after putting the value" + headers);
            long endtime = System.nanoTime();
            long duration = starttime - endtime;
            log.info("this is the duration for CPET" + duration);
            return true;
        } catch (Exception e) {
            log.error("Error while handling json input", e);
        }
        return false;
    }

    private static String generateBearerToken(String clientId, String clientSecret) throws CustomHandlerException, UnsupportedEncodingException {
        String token1 = null;
        StringBuilder token = new StringBuilder();
        HttpPost httpPost = new HttpPost("https://trust.ctxwsstgapi.net/ptipkmndeg54/tokens/clients");
        List<NameValuePair> params = new ArrayList<NameValuePair>(2);
        JSONObject json = new JSONObject();
        json.put("clientId", "3ada351a-9083-4c51-aefb-59983ce33a02");
        json.put("clientSecret", "oMjOxSIHNkNak589J_MNhg==");
        httpPost.addHeader("content-type", "application/json");
        httpPost.setEntity(new StringEntity(json.toString()));
        RequestConfig defaultRequestConfig = RequestConfig.custom().setConnectTimeout(30000).setSocketTimeout(30000)
                .build();
        httpPost.setConfig(defaultRequestConfig);
        CloseableHttpClient apacheClient = null;
        try {
            apacheClient = HttpClientFactory.getHttpsClient();
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new CustomHandlerException("Error while getting https client", e);
        }
        long startTime = System.nanoTime();
        log.info("this is the start of the method======" + startTime);
        try (CloseableHttpResponse response = apacheClient.execute(httpPost)) {
            long endTime = System.nanoTime();
            log.info("this is the end of the method====" + endTime);
            long duration = (endTime - startTime);
            log.info("this is the duration of the method======" + duration);
            String responseString = IOUtils.toString(response.getEntity().getContent());
            log.info("hi this is the response from token gen API" + responseString);
            JSONParser parser = new JSONParser();
            JSONObject body = (JSONObject) parser.parse(responseString);
            token1 = (String) body.get("token");
            token.append("CWSAuth bearer=");
            token.append(token1);
            log.info("this is the bearer token " + token);
            String token2 = Base64.getEncoder().withoutPadding().encodeToString(token.toString().getBytes());
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException | ParseException e) {
            throw new CustomHandlerException("Error while getting or parsing response for get token request", e);
        }
        //return Response.status(Response.Status.OK).entity("{\"token\" : \"" + token + "\"}").build();
        return token.toString();
    }


    //start of cc auth implementation

    public static boolean AuthorizeBearerToken(String token,String customerId)
    {

       // log.info("Inside Authorization of token");
        String header = token;
        IdentityValidationContext context = new IdentityValidationContext(customerId);
        RequestMessage request = new RequestMessage(RequestMethod.GET,"https://catalogs.apps.cloudchalupa.com/ptipkmndeg54/sites");
       // RequestMessage requestMessage = new RequestMessage(RequestMethod.GET);
        IdentityValidationResult result = ValidateHeader(header, request, context);

        System.out.println("this is the result==="+result);

        log.info("This is the result"+result);

            if (result.isSuccessAuthorizationCode())
            {
                return true;
            }
            else {
                return false;
            }
        }

    public static IdentityValidationResult ValidateHeader(String header,RequestMessage request,IdentityValidationContext context)
    {
        CCAuthOptions configOptions = new CCAuthOptions();
        CCAuthHandleOptions options = new CCAuthHandleOptions();
        configOptions.routeTemplate("https://[service].ctxwsstgapi.net");
        configOptions.serviceName("XenApp");
        configOptions.serviceInstanceId("E16942-0");
        Signer signer = new RSASigner("PFJTQUtleVZhbHVlPjxNb2R1bHVzPnRMVTNtTy9HUW1IbjFFMGgxM2QrSVEydlpQdi8xMHVpbWtJZXlPQkRqYll2MHo5MVpXMnBTd0lRNW9LL2htMjVtaUNnMkIxeHJzVHNPelJsenArWm5nUXhoVXA0d3RNY3hvNktLZGZDcGRRTXFZTmw1OHFBMlg2bWRoSW5RblJtQ0pobFFrNWM3ejRGYUZQZVFuVHFjVXl5L2dPWjNHN0RjeC91YVV6MkJuYVhPNXJhdmxId0dOckQ1N3oyOUFkbVQzUHRrcW94eXlVUjdGRWVYUTdnalViUXNSVEhNYWJWWi81dXFKUTM1dnlWeFVuUTBFVFVrUlptSHBETGVVeFplbUlzWXNOM3ZZK05CSktIOXFJOC9EM0FDMEY5STQrMmp5ZUVoL1lXV21RVkdBTDlCRDljYUFKV29kYTVjZTF2VEtlclBZekdsTWVydXFaNGNyTU02UT09PC9Nb2R1bHVzPjxFeHBvbmVudD5BUUFCPC9FeHBvbmVudD48UD4yS1ZsZFN0U0JIYWJRdjBMelM4U1JNNDAwVG96RXg0MjVscG1TcEZ4eld6VEtGeVlmb2c3WGxZdGxOZVkzeHdQcDBZbkRWbnNJWStuTkcxcTd0Y3hCaWo5QlRLUVVDTWRXY2R1N0dLcHVFL2I5QytnUGFodWhZMmUyc25mazJ4TVRnQmJWZE5EZEVwblQ5UEtvdEczZFI5Tzc5Y2JiVk5IUVB0T21LaEtoanM9PC9QPjxRPjFZaVovSVJqRElRWEdPekVVb1pGeDNESHhNTUd5NERPMUl1eW5kRmZubmtieW4weTkrQmVPMEFkSHo5NUdlMzYyWHIyaFpNQWN4aG9Nb3c1NE9oYmhUaFliVCtsb2dINlZTeFMySDN2R2YvSFJoenJUbUFkNExpblRyZno3ZU1vK1VoMnEvMzd3dWU5L2FDajB1VGhDOER0NG5rbyt4dmFSSUJJMVVPR2N5cz08L1E+PERQPmxDc2FaclpJRTlGaWdzQTZFQXkvZTl0aitDekx2YW1PZHFFaEVLTEVxSEJqUWxtQjJoZ21NbkRTSDlnTUw1c3JnWVhUTTZocWZOR1kwNXg1NC91OUJhK0d6TVUyT2ZpcUhEcnZ2REFHVDQ0ZXFyVGY3UXVDKzBoT2V1aFNScXRzekhRbUExN0g1WUwxZ1gwaU81VWUyYldkOFI2M0hXQTFmVlhpL1Rrc0x2MD08L0RQPjxEUT5ac2J4RnhIQmV1eTVFVTRrMEhQQWNsWmVVTjV1RHRWWXVBVGxYQURDdlV3ZGpFRG1uMWhuQXEzQlZxRCtjUFNTb01zR2pSUk1TeG1jVFhnaEE1ZlROVFFCbTlQZXJUTzJnZmhyaDdoVnRYWGZQR2YrK2lKWlB5aWhuc243cHF5SHREU2txZlA5a2JwcFFBSnAxOEJDY1ozUzRnYmZLcjRsT0lObWl2K05YekU9PC9EUT48SW52ZXJzZVE+eUo5bWFteENER2dQVGVHa2RQdm1jcURsK3R2OUU5VmlRK3pqWXNSeFcwRENMT2toUERsWnh1RkhEZlI1cXZvNy8vZXJxUFNpZlZJRWFEaFJFenlMeTI2cFhlRjhUajRXdi9NMWMyL1V4WEZHS212Mk1qbEFScXNsVE4yS1hTMjR1YVhuY0J0dUtEUXZ1ZWJ2TUtYZjV4VDBBTEFYd1NYSmtabkhKREtTMUQ0PTwvSW52ZXJzZVE+PEQ+VXVuTEJyc05acXZ4YVBHekUxL2FXV1FrRTl1a09hNlVmdDdUclN0cEUzNkNWeFVJMG04TWZFUUlhUnVZc1I5clI1S3MrandZU3k1RGphNnUvNjB2R05lbnVSYkFiZlBiZ2ljb3NhWXp3MDZXT0xqM2F2RDVTZEhZb3RnVmQySmM1cGkxN1VSelU3cExWT0VXVzd1MVRpTDVCWisxV3ZUZmJOVTcyTWpkSVgxRzZWMWpTTGNsa0hyTnJDNmRYRCtzdlYxa3JFYUxndlVtUXM1THhmT29zSmVhVEF0eHE2NE13bDBLdjNUWlR4c3NGV3dQUVY3ZWhCY1h2bU1nYnN2WXpwbU9QeGRRTXZMY3l5NWVOS0JCZDh5MmpSaUI5ZmtCUDA5WEttdDNvenFLNkFQN1hoMDA5WS9qaEJnUTY4VStXRFIyTC9sQ2FWZmd1Skk0TEVkUzJRPT08L0Q+PC9SU0FLZXlWYWx1ZT4=");
        options.configurationOptions(configOptions);
        options.signer(signer);
        try (CCAuthHandle handle = new CCAuthHandle(options)) {
            return handle.validateIdentity(header, request, context);
        } catch (Exception ignored) {
            // Handle and/or log error.
            return null;
        }
    }

    public static String createServiceKey(RequestMessage request) {
        CCAuthOptions configOptions = new CCAuthOptions();
        configOptions.serviceName("XenApp");
        configOptions.serviceInstanceId("E16942-0"); // optional field, fill if needed

        try ( Signer signer = new RSASigner("PFJTQUtleVZhbHVlPjxNb2R1bHVzPnRMVTNtTy9HUW1IbjFFMGgxM2QrSVEydlpQdi8xMHVpbWtJZXlPQkRqYll2MHo5MVpXMnBTd0lRNW9LL2htMjVtaUNnMkIxeHJzVHNPelJsenArWm5nUXhoVXA0d3RNY3hvNktLZGZDcGRRTXFZTmw1OHFBMlg2bWRoSW5RblJtQ0pobFFrNWM3ejRGYUZQZVFuVHFjVXl5L2dPWjNHN0RjeC91YVV6MkJuYVhPNXJhdmxId0dOckQ1N3oyOUFkbVQzUHRrcW94eXlVUjdGRWVYUTdnalViUXNSVEhNYWJWWi81dXFKUTM1dnlWeFVuUTBFVFVrUlptSHBETGVVeFplbUlzWXNOM3ZZK05CSktIOXFJOC9EM0FDMEY5STQrMmp5ZUVoL1lXV21RVkdBTDlCRDljYUFKV29kYTVjZTF2VEtlclBZekdsTWVydXFaNGNyTU02UT09PC9Nb2R1bHVzPjxFeHBvbmVudD5BUUFCPC9FeHBvbmVudD48UD4yS1ZsZFN0U0JIYWJRdjBMelM4U1JNNDAwVG96RXg0MjVscG1TcEZ4eld6VEtGeVlmb2c3WGxZdGxOZVkzeHdQcDBZbkRWbnNJWStuTkcxcTd0Y3hCaWo5QlRLUVVDTWRXY2R1N0dLcHVFL2I5QytnUGFodWhZMmUyc25mazJ4TVRnQmJWZE5EZEVwblQ5UEtvdEczZFI5Tzc5Y2JiVk5IUVB0T21LaEtoanM9PC9QPjxRPjFZaVovSVJqRElRWEdPekVVb1pGeDNESHhNTUd5NERPMUl1eW5kRmZubmtieW4weTkrQmVPMEFkSHo5NUdlMzYyWHIyaFpNQWN4aG9Nb3c1NE9oYmhUaFliVCtsb2dINlZTeFMySDN2R2YvSFJoenJUbUFkNExpblRyZno3ZU1vK1VoMnEvMzd3dWU5L2FDajB1VGhDOER0NG5rbyt4dmFSSUJJMVVPR2N5cz08L1E+PERQPmxDc2FaclpJRTlGaWdzQTZFQXkvZTl0aitDekx2YW1PZHFFaEVLTEVxSEJqUWxtQjJoZ21NbkRTSDlnTUw1c3JnWVhUTTZocWZOR1kwNXg1NC91OUJhK0d6TVUyT2ZpcUhEcnZ2REFHVDQ0ZXFyVGY3UXVDKzBoT2V1aFNScXRzekhRbUExN0g1WUwxZ1gwaU81VWUyYldkOFI2M0hXQTFmVlhpL1Rrc0x2MD08L0RQPjxEUT5ac2J4RnhIQmV1eTVFVTRrMEhQQWNsWmVVTjV1RHRWWXVBVGxYQURDdlV3ZGpFRG1uMWhuQXEzQlZxRCtjUFNTb01zR2pSUk1TeG1jVFhnaEE1ZlROVFFCbTlQZXJUTzJnZmhyaDdoVnRYWGZQR2YrK2lKWlB5aWhuc243cHF5SHREU2txZlA5a2JwcFFBSnAxOEJDY1ozUzRnYmZLcjRsT0lObWl2K05YekU9PC9EUT48SW52ZXJzZVE+eUo5bWFteENER2dQVGVHa2RQdm1jcURsK3R2OUU5VmlRK3pqWXNSeFcwRENMT2toUERsWnh1RkhEZlI1cXZvNy8vZXJxUFNpZlZJRWFEaFJFenlMeTI2cFhlRjhUajRXdi9NMWMyL1V4WEZHS212Mk1qbEFScXNsVE4yS1hTMjR1YVhuY0J0dUtEUXZ1ZWJ2TUtYZjV4VDBBTEFYd1NYSmtabkhKREtTMUQ0PTwvSW52ZXJzZVE+PEQ+VXVuTEJyc05acXZ4YVBHekUxL2FXV1FrRTl1a09hNlVmdDdUclN0cEUzNkNWeFVJMG04TWZFUUlhUnVZc1I5clI1S3MrandZU3k1RGphNnUvNjB2R05lbnVSYkFiZlBiZ2ljb3NhWXp3MDZXT0xqM2F2RDVTZEhZb3RnVmQySmM1cGkxN1VSelU3cExWT0VXVzd1MVRpTDVCWisxV3ZUZmJOVTcyTWpkSVgxRzZWMWpTTGNsa0hyTnJDNmRYRCtzdlYxa3JFYUxndlVtUXM1THhmT29zSmVhVEF0eHE2NE13bDBLdjNUWlR4c3NGV3dQUVY3ZWhCY1h2bU1nYnN2WXpwbU9QeGRRTXZMY3l5NWVOS0JCZDh5MmpSaUI5ZmtCUDA5WEttdDNvenFLNkFQN1hoMDA5WS9qaEJnUTY4VStXRFIyTC9sQ2FWZmd1Skk0TEVkUzJRPT08L0Q+PC9SU0FLZXlWYWx1ZT4=")) {
            CCAuthHandleOptions options = new CCAuthHandleOptions();
            options.configurationOptions(configOptions);
            options.signer(signer);
            try (CCAuthHandle handle = new CCAuthHandle(options)) {
                return handle.createServiceKey(request);
            }
        } catch (Exception ignored) {
            // Handle and/or log error.
            return null;
        }
    }
public static String GenerateServiceKey(String requestURL)
{

    RequestMessage request = new RequestMessage(RequestMethod.GET,requestURL);
    {
        String serviceKey = createServiceKey(request);
        System.out.println("This is the service key"+serviceKey);

        if (serviceKey != null)
        {
            return serviceKey;
        }
    }
    return "invalid";
}

    public boolean handleResponse(MessageContext messageContext) {
        return true;
    }

    public void setTokenServerUrl(String tokenServerUrl) {
        this.tokenServerUrl = tokenServerUrl;
    }

    public String getTokenServerUrl() {
        return tokenServerUrl;
    }

    @Override
    public void init(SynapseEnvironment synapseEnvironment) {

    }

    @Override
    public void destroy() {

    }

    public String getTokenPrefix() {
        return tokenPrefix;
    }

    public void setTokenPrefix(String tokenPrefix) {
        this.tokenPrefix = tokenPrefix;
    }


    public static void main(String[] args) throws UnsupportedEncodingException, CustomHandlerException {
       Boolean bool = AuthorizeBearerToken(generateBearerToken("3ada351a-9083-4c51-aefb-59983ce33a02","oMjOxSIHNkNak589J_MNhg=="),"ptipkmndeg54");
       log.info("This is the boolean value"+bool);
    }
}