package com.zjgsu.jby.service;

import java.util.Map;
import org.springframework.cloud.client.discovery.DiscoveryClient;
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

    public UserClient(RestTemplate rest, DiscoveryClient discovery) {
        this.rest = rest;
        this.discovery = discovery;
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
        try {
            // 得到地址后直接向用户服务发送 HTTP 请求，读取用户资料。
            return rest.getForObject(url, Map.class);
            // 资源不存在与依赖调用失败分别返回 404 和 503。
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "依赖资源不存在", ex);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "user-service 调用失败", ex);
        }
    }
}
