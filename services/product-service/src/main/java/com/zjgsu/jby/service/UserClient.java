package com.zjgsu.jby.service;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class UserClient {
    private final RestTemplate rest;
    private final DiscoveryClient discovery;
    private final String gatewaySecret;

    public UserClient(RestTemplate rest, DiscoveryClient discovery,
                      @Value("${app.security.gateway-secret}") String gatewaySecret) {
        this.rest = rest;
        this.discovery = discovery;
        this.gatewaySecret = gatewaySecret;
    }

    private String address(Long id) {
        // 服务发现返回实例地址，当前实现选择列表中的第一个实例。
        var instances = discovery.getInstances("user-service");
        if (instances.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "user-service 无可用实例");
        }
        return instances.get(0).getUri().toString() + "/api/users/" + id;
    }

    public Map<String, Object> findById(Long id) {
        String url = address(id);
        // 用户服务要求内部密钥，服务间直连也需携带，避免匿名访问用户资料。
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Gateway-Secret", gatewaySecret);
        try {
            // 得到地址后直接向用户服务发送 HTTP 请求，读取用户资料。
            return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class).getBody();
            // 资源不存在与依赖调用失败分别返回 404 和 503。
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "依赖资源不存在", ex);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "user-service 调用失败", ex);
        }
    }
}
