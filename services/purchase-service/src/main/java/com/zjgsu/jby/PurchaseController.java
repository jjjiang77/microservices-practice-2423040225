package com.zjgsu.jby;
import java.util.Map;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {
 private final DiscoveryClient discovery; private final RestTemplate http;
 public PurchaseController(DiscoveryClient discovery) {
  this.discovery=discovery;
  var f=new SimpleClientHttpRequestFactory(); f.setConnectTimeout(3000); f.setReadTimeout(5000); this.http=new RestTemplate(f);
 }
 public record Request(Long productId, Long buyerId) {}
 @PostMapping public ResponseEntity<Map> purchase(@RequestBody Request req) {
  var instances=discovery.getInstances("product-service");
  if(instances.isEmpty()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"商品服务无可用实例");
  String url=instances.get(0).getUri()+"/api/products/"+req.productId()+"/purchase";
  try { var response=http.postForEntity(url,Map.of("buyerId",req.buyerId()),Map.class); return ResponseEntity.status(response.getStatusCode()).body(response.getBody()); }
  catch(HttpClientErrorException.NotFound ex) { throw new ResponseStatusException(HttpStatus.NOT_FOUND,"商品或买家不存在",ex); }
  catch(HttpClientErrorException ex) { throw new ResponseStatusException(ex.getStatusCode(),"购买请求被拒绝",ex); }
  catch(RestClientException ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"商品服务调用失败",ex); }
 }
}
