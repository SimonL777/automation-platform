import { chromium } from 'playwright';
import { validate, html, specHash, compile } from './contract.mjs';
let browser;let compiledHash=null;const results=[];const started=Date.now();
try {
 const encoded=process.env.RUN_PAYLOAD_BASE64;if(!encoded||encoded.length>128000)throw new Error('INVALID_PAYLOAD');
 const {workflow}=JSON.parse(Buffer.from(encoded,'base64').toString('utf8'));validate(workflow);compiledHash=specHash(workflow);
 browser=await chromium.launch({headless:true,args:['--no-sandbox','--disable-dev-shm-usage']});
 const context=await browser.newContext({serviceWorkers:'block'});await context.route('**/*',route=>route.abort());
 const page=await context.newPage();page.setDefaultTimeout(5000);await page.setContent(html);
 const trackedPage={getByTestId(target){const locator=page.getByTestId(target);return {
  async fill(value){await locator.fill(value);results.push({index:results.length,op:'fill',passed:true});},
  async click(){await locator.click();results.push({index:results.length,op:'click',passed:true});},
  textContent:()=>locator.textContent()
 };}};
 const expect=locator=>({async toContainText(value){const actual=await locator.textContent();if(!actual?.includes(value))throw new Error(`ASSERTION_FAILED_STEP_${results.length}`);results.push({index:results.length,op:'assertText',passed:true});}});
 const AsyncFunction=Object.getPrototypeOf(async function(){}).constructor;
 await new AsyncFunction('page','expect',compile(workflow))(trackedPage,expect);
 console.log(JSON.stringify({kind:'report',passed:true,syntheticTarget:true,specHash:specHash(workflow),steps:results,durationMs:Date.now()-started}));
} catch(error) {
 console.log(JSON.stringify({kind:'report',passed:false,syntheticTarget:true,specHash:compiledHash,error:String(error.message).slice(0,300),steps:results,durationMs:Date.now()-started}));process.exitCode=1;
} finally {await browser?.close();}
