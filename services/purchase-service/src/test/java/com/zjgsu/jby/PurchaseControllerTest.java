package com.zjgsu.jby;

import feign.FeignException;
import feign.Request;
import feign.Response;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PurchaseControllerTest {
    private final PurchaseController.Request request = new PurchaseController.Request(1L, 2L);

    @Test
    void returnsProductResponse() {
        var controller = new PurchaseController(
                new PurchaseService((id, buyer) -> ResponseEntity.ok(Map.of("status", "SOLD"))));
        assertEquals("SOLD", controller.purchase(request).getBody().get("status"));
    }

    @Test
    void mapsDownstreamFailureTo503() {
        var downstream = Request.create(Request.HttpMethod.POST,
                "http://product-service/api/products/1/purchase", Map.of(), null, null, null);
        var failure = FeignException.errorStatus("purchase", Response.builder()
                .status(503).reason("Unavailable").request(downstream).build());
        var controller = new PurchaseController(new PurchaseService((id, buyer) -> { throw failure; }));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(ResponseStatusException.class, () -> controller.purchase(request)).getStatusCode());
    }
}
