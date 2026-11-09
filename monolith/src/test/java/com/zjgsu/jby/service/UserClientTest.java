package com.zjgsu.jby.service;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class UserClientTest {
    private final DiscoveryClient discovery = mock(DiscoveryClient.class);
    private final RestTemplate rest = mock(RestTemplate.class);
    private final UserClient client = new UserClient(rest, discovery);

    @Test
    void emptyInstancesReturn503WithoutHttp() {
        when(discovery.getInstances("user-service")).thenReturn(List.of());
        assertThatThrownBy(() -> client.findById(1L))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        verifyNoInteractions(rest);
    }

    @Test
    void resolvesAgainForEveryOperationAndUsesDiscoveredUri() {
        when(discovery.getInstances("user-service")).thenReturn(
            List.of(new DefaultServiceInstance("a", "user-service", "10.0.0.2", 8081, false)),
            List.of(new DefaultServiceInstance("b", "user-service", "10.0.0.3", 8081, false)));
        when(rest.getForObject("http://10.0.0.2:8081/api/users/1", Map.class)).thenReturn(Map.of("id", 1));
        when(rest.getForObject("http://10.0.0.3:8081/api/users/1", Map.class)).thenReturn(Map.of("id", 1));
        assertThat(client.findById(1L)).containsEntry("id", 1);
        assertThat(client.findById(1L)).containsEntry("id", 1);
        verify(discovery, times(2)).getInstances("user-service");
        verify(rest).getForObject("http://10.0.0.3:8081/api/users/1", Map.class);
    }

    @Test
    void staleAddressFailureReturns503() {
        when(discovery.getInstances("user-service")).thenReturn(
            List.of(new DefaultServiceInstance("a", "user-service", "10.0.0.2", 8081, false)));
        when(rest.getForObject(anyString(), eq(Map.class))).thenThrow(new ResourceAccessException("connection failed"));
        assertThatThrownBy(() -> client.findById(1L))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
