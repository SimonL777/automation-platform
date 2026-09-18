import {chromium,request as apiRequest} from 'playwright';
import {validate,compile,specHash} from './contract.mjs';
import {startFixture} from './fixture.mjs';
let browser,api,fixture,page,response,bodyText='',compiledHash=null,workflow,screenshot=null;const results=[],started=Date.now();
const fail=message=>{throw new Error(message);};
try{
 const encoded=process.env.RUN_PAYLOAD_BASE64;if(!encoded||encoded.length>256000)fail('INVALID_PAYLOAD');
 ({workflow}=JSON.parse(Buffer.from(encoded,'base64').toString('utf8')));validate(workflow);compiledHash=specHash(workflow);
 let base=process.env.RUN_TARGET_URL;if(!base){fixture=await startFixture();base=fixture.url;}const allowed=new URL(base).origin;
 const resolve=path=>{const url=new URL(path,base);if(url.origin!==allowed)fail('TARGET_ORIGIN_NOT_ALLOWED');return url.href;};
 const channel=workflow.target.startsWith('api:')?'API':'WEB';
 if(channel==='WEB'){
  browser=await chromium.launch({headless:true,args:['--no-sandbox','--disable-dev-shm-usage']});const context=await browser.newContext({serviceWorkers:'block',viewport:{width:1280,height:800}});
  await context.route('**/*',route=>{try{new URL(route.request().url()).origin===allowed?route.continue():route.abort();}catch{route.abort();}});page=await context.newPage();page.setDefaultTimeout(8000);await page.goto(base);
 }else api=await apiRequest.newContext({baseURL:base,ignoreHTTPSErrors:false,timeout:10000});
 const locator=t=>{if(workflow.version===1)return page.getByTestId(t);const [kind,...tail]=t.split(':');const value=tail.join(':');if(kind==='testId')return page.getByTestId(value);if(kind==='css')return page.locator(value);if(kind==='text')return page.getByText(value,{exact:true});if(kind==='role'){const [role,...name]=value.split('|');return page.getByRole(role,name.length?{name:name.join('|'),exact:true}:{});}fail('INVALID_LOCATOR');};
 const step=async(op,target,value)=>{
  const start=Date.now();const detail={index:results.length,op,target,passed:false};
  try{
   if(channel==='API'){
    if(op==='request'){const req=JSON.parse(value);response=await api.fetch(resolve(req.path),{method:target,data:req.body,headers:req.headers,maxRedirects:0});bodyText=await response.text();if(bodyText.length>200000)fail('RESPONSE_TOO_LARGE');detail.http={method:target,path:req.path,status:response.status(),body:bodyText.slice(0,16000)};}
    if(op==='assertStatus'&&response.status()!==Number(value))fail(`STATUS_MISMATCH: expected ${value}, got ${response.status()}`);
    if(op==='assertBody'&&!bodyText.includes(value))fail('BODY_ASSERTION_FAILED');
    if(op==='assertJson'){let actual=JSON.parse(bodyText);for(const part of target.replace(/^\$\.?/,'').replace(/\[(\d+)\]/g,'.$1').split('.').filter(Boolean))actual=actual?.[part];const expected=JSON.parse(value);if(JSON.stringify(actual)!==JSON.stringify(expected))fail(`JSON_ASSERTION_FAILED: ${target}`);}
   }else{
    if(op==='navigate')await page.goto(resolve(target));
    else{const loc=locator(target);
     if(op==='fill')await loc.fill(value);if(op==='click')await loc.click();if(op==='check')await loc.check();if(op==='select')await loc.selectOption(value);
     if(op==='assertText'){await loc.waitFor({state:'visible'});if(!(await loc.textContent())?.includes(value))fail('TEXT_ASSERTION_FAILED');}
     if(op==='assertVisible')await loc.waitFor({state:'visible'});
     if(op==='assertCount'&&(await loc.count())!==Number(value))fail('COUNT_ASSERTION_FAILED');
    }
   }
   detail.passed=true;
  }catch(e){detail.error=String(e.message).slice(0,400);throw e;}finally{detail.durationMs=Date.now()-start;results.push(detail);}
 };
 const AsyncFunction=Object.getPrototypeOf(async function(){}).constructor;
 if(workflow.version===1){const tracked={getByTestId:t=>({fill:v=>step('fill',t,v),click:()=>step('click',t,null),_target:t})};const expect=l=>({toContainText:v=>step('assertText',l._target,v)});await new AsyncFunction('page','expect',compile(workflow))(tracked,expect);}
 else await new AsyncFunction('step',compile(workflow))(step);
 if(page)screenshot=(await page.screenshot({type:'png',fullPage:true})).toString('base64');
 console.log(JSON.stringify({kind:'report',passed:true,channel,syntheticTarget:!!fixture,specHash:compiledHash,steps:results,durationMs:Date.now()-started,screenshotBase64:screenshot}));
}catch(error){if(page)try{screenshot=(await page.screenshot({type:'png'})).toString('base64');}catch{}
 console.log(JSON.stringify({kind:'report',passed:false,channel:workflow?.target?.startsWith('api:')?'API':'WEB',syntheticTarget:!!fixture,specHash:compiledHash,error:String(error.message).slice(0,500),steps:results,durationMs:Date.now()-started,screenshotBase64:screenshot}));process.exitCode=1;
}finally{await api?.dispose();await browser?.close();await fixture?.close();}
