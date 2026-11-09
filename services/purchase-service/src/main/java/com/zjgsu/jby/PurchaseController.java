package com.zjgsu.jby;
import java.util.Map;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
/** 接收购买请求并转发给商品服务，商品状态由商品服务更新。 */
@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {
 private final DiscoveryClient discovery; private final RestTemplate http;
 public PurchaseController(DiscoveryClient discovery) {
  this.discovery=discovery;
  // 商品服务不可达或响应过慢时，让请求在有限时间内失败。
  var f=new SimpleClientHttpRequestFactory(); f.setConnectTimeout(3000); f.setReadTimeout(5000); this.http=new RestTemplate(f);
 }
 public record Request(Long productId, Long buyerId) {}
 @PostMapping public ResponseEntity<Map> purchase(@RequestBody Request req) {
  // 通过服务名获取实例，再用实例 URI 拼出商品购买接口。
  var instances=discovery.getInstances("product-service");
  if(instances.isEmpty()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"商品服务无可用实例");
  String url=instances.get(0).getUri()+"/api/products/"+req.productId()+"/purchase";
  // 商品编号放在路径中，买家编号作为 JSON 请求体发送。
  try { var response=http.postForEntity(url,Map.of("buyerId",req.buyerId()),Map.class); return ResponseEntity.status(response.getStatusCode()).body(response.getBody()); }
  // 保留商品服务的业务拒绝状态，其他远程调用失败按 503 处理。
  catch(HttpClientErrorException.NotFound ex) { throw new ResponseStatusException(HttpStatus.NOT_FOUND,"商品或买家不存在",ex); }
  catch(HttpClientErrorException ex) { throw new ResponseStatusException(ex.getStatusCode(),"购买请求被拒绝",ex); }
  catch(RestClientException ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"商品服务调用失败",ex); }
 }
}
