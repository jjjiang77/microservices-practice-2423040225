"""验收个人项目当前链路，新增带期中验收前缀的演示商品，不修改已有商品。"""
import json, urllib.request, urllib.error, datetime
from pathlib import Path
base='http://127.0.0.1:'
results=[]
def call(name,port,path,data=None,expected=200):
    req=urllib.request.Request(base+str(port)+path,data=None if data is None else json.dumps(data).encode(),headers={'Content-Type':'application/json'})
    try:
        with urllib.request.urlopen(req,timeout=15) as r: status=r.status; body=json.load(r)
    except urllib.error.HTTPError as e: status=e.code; body=json.load(e)
    results.append({'check':name,'method':req.get_method(),'path':path,'status':status,'expected':expected,'pass':status==expected,'response':body})
    if status!=expected: raise RuntimeError(name+': '+str(status))
    return body
try:
    for port in (8080,8081,8083): call('应用健康',port,'/actuator/health')
    call('卖家查询',8081,'/api/users/1');call('买家查询',8081,'/api/users/2')
    title='期中验收-'+datetime.datetime.now().strftime('%Y%m%d-%H%M%S')
    data=dict(title=title,description='自动验收演示商品，非真实交易',price=45,category='TEXTBOOK',conditionLevel='LIKE_NEW',sellerId=1)
    product=call('发布商品',8080,'/api/products',data,201); pid=product['id']
    assert product['status']=='PENDING'
    call('重复发布拦截',8080,'/api/products',data,409)
    call('价格上限拦截',8080,'/api/products',{**data,'title':title+'-超价','price':5000},422)
    product=call('审核上架',8080,f'/api/products/{pid}/approve',{});assert product['status']=='ON_SALE'
    filtered=call('卖家状态分类组合筛选',8080,'/api/products?sellerId=1&status=ON_SALE&category=TEXTBOOK&size=50')
    assert any(p['id']==pid for p in filtered['content'])
    assert all(p['sellerId']==1 and p['status']=='ON_SALE' and p['category']=='TEXTBOOK' for p in filtered['content'])
    call('自购拦截',8080,f'/api/products/{pid}/purchase',{'buyerId':1},422)
    sold=call('购买服务跨服务调用',8083,'/api/purchases',{'productId':pid,'buyerId':2});assert sold['status']=='SOLD'
    call('重复购买拦截',8083,'/api/purchases',{'productId':pid,'buyerId':2},422)
    detail=call('查询持久化结果',8080,f'/api/products/{pid}');assert detail['status']=='SOLD'
finally:
    Path('docs/midterm/evidence/verification.json').write_text(json.dumps({'checked_at':datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).isoformat(),'results':results},ensure_ascii=False,indent=2))
    print('\n'.join(f"{r['status']} {r['check']}: {'PASS' if r['pass'] else 'FAIL'}" for r in results))
