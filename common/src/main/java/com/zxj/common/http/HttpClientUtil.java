package cn.zxj.utils.http;

import com.alibaba.fastjson.JSON;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.testng.util.Strings;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.http.HttpHeaders.USER_AGENT;

/**
 * http服务类
 */
public class HttpClientUtil {
    static final Logger LOGGER = LoggerFactory.getLogger(HttpClientUtil.class);
    /**
     * 一个单例的httpClient
     */
    private static final CloseableHttpClient HTTPCLIENT;

    static {
        // httpClient连接的属性设置，如超时时间，连接数的最大限制
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(HttpConfigConstant.READWRITE_TIMEOUT)
                .setConnectTimeout(HttpConfigConstant.CONNECT_TIMEOUT)
                .setConnectionRequestTimeout(HttpConfigConstant.ACQUIRE_CONNECTION_TIMEOUT)
                .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                .build();

        SocketConfig socketConfig = SocketConfig.custom().setTcpNoDelay(true).setSoKeepAlive(true)
                .build();
        //http连接池配置
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        // 将最大连接数增加到200
        cm.setMaxTotal(200);
        // 将每个路由基础的连接增加到20
        cm.setDefaultMaxPerRoute(20);


        HttpRequestRetryHandler retryHandler = new HttpRequestRetryHandler() {
            @Override
            public boolean retryRequest(
                    IOException exception,
                    int executionCount,
                    HttpContext context) {
                if (executionCount >= 3) {
                    // 如果超过最大重试次数，那么就不要继续了
                    return false;
                }
                if (exception instanceof NoHttpResponseException) {
                    // 如果服务器丢掉了连接，那么就重试
                    return true;
                }
                if (exception instanceof SSLHandshakeException) {
                    // 不要重试SSL握手异常
                    return false;
                }
                HttpRequest request = (HttpRequest) context.getAttribute(
                        ExecutionContext.HTTP_REQUEST);
                boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
                if (idempotent) {
                    // 如果请求被认为是幂等的，那么就重试
                    return true;
                }
                return false;
            }
        };

        HTTPCLIENT = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .setDefaultSocketConfig(socketConfig)
                .setRetryHandler(retryHandler)
                .build();
    }

    /**
     * 禁用构造函数
     */
    private HttpClientUtil() {
        // 禁用构造函数
    }

    /**
     * 发送http get请求
     *
     * @param url
     * @param requestParams
     * @return
     */
    public static HttpClientResult doGet(String url, Map<String, String> requestParams) {
        return null;
    }

    /**
     * 发送http请求
     *
     * @param url           http + domain + pathURI(一定要加上http协议名称)
     * @param requestParams 请求的参数
     * @return
     */
    public static HttpClientResult doPost(String url, Map<String, String> requestParams) {
        HttpClientResult result = new HttpClientResult();
        // 2. 构建request请求中的参数
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        if (null != requestParams && !requestParams.isEmpty()) {
            for (Map.Entry<String, String> entry : requestParams.entrySet()) {
                params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
        }

        CloseableHttpResponse httpResponse = null;
        // 3. 根据请求的不同类型来进行http请求
        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader(USER_AGENT,"Mozilla/5.0");
            httpPost.setEntity(new UrlEncodedFormEntity(params,"UTF-8"));
            httpResponse = HTTPCLIENT.execute(httpPost);
            String content = EntityUtils.toString(httpResponse.getEntity());
            result.setSuccess(true);
            result.setData(content);
        } catch (Exception e) {
            LOGGER.error("http请求出错了:URL=" + url + " , httpMethod=",
                    " ,requestParams=" + requestParams+"error = "+e.getMessage());
            result.setSuccess(false);
        }

        return result;
    }
    /**
     * 发送http请求
     *
     * @param url           http + domain + pathURI(一定要加上http协议名称)
     * @param params 请求的参数
     * @return
     */
    public static HttpClientResult doJsonPost(String url,Object params,Map<String,String> headers) {
        HttpClientResult result = new HttpClientResult();
        CloseableHttpResponse httpResponse = null;
        // 3. 根据请求的不同类型来进行http请求
        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader(USER_AGENT,"Mozilla/5.0");
            if (null != headers && !headers.isEmpty()) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }
            }

            StringEntity entity = new StringEntity(JSON.toJSONString(params), ContentType.create("application/json", "utf-8"));
            httpPost.setEntity(entity);
            httpResponse = HTTPCLIENT.execute(httpPost);
            String content = EntityUtils.toString(httpResponse.getEntity());
            result.setSuccess(true);
            result.setData(content);
        } catch (Exception e) {
            LOGGER.error("http请求出错了:URL=" + url+" ,requestParams=" +JSON.toJSONString(params),e.getMessage());
            result.setSuccess(false);
        }

        return result;
    }
}

