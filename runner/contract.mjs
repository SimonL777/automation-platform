import {createHash} from 'node:crypto';
export function validate(w){
 if(!w||![1,2].includes(w.version)||typeof w.target!=='string'||!Array.isArray(w.steps)||w.steps.length<1||w.steps.length>60)throw new Error('INVALID_WORKFLOW');
 if(w.version===1&&w.target!=='todo-demo'||w.version===2&&!/^(web|api):[a-zA-Z0-9_-]{1,64}$/.test(w.target))throw new Error('INVALID_TARGET');
 const api=w.target.startsWith('api:');let asserted=false,requested=false;
 for(const s of w.steps){
  if(!s||typeof s.op!=='string'||typeof s.target!=='string'||s.target.length>500||s.value!=null&&(typeof s.value!=='string'||s.value.length>16000))throw new Error('INVALID_STEP');
  if(api){
   if(s.op==='request'){
    if(!['GET','POST','PUT','PATCH','DELETE'].includes(s.target))throw new Error('INVALID_METHOD');const r=JSON.parse(s.value);
    if(typeof r.path!=='string'||!r.path.startsWith('/')||r.path.startsWith('//')||r.path.includes('..')||r.path.includes('\\'))throw new Error('INVALID_PATH');
    for(const h of Object.keys(r.headers||{}))if(['host','authorization','cookie','proxy-authorization','connection'].includes(h.toLowerCase()))throw new Error('FORBIDDEN_HEADER');requested=true;
   }else if(s.op==='assertStatus'&&requested&&/^[1-5]\d{2}$/.test(s.value))asserted=true;
   else if(s.op==='assertJson'&&requested&&/^\$?(\.[a-zA-Z0-9_-]+|\[\d+\])*$/.test(s.target)){JSON.parse(s.value);asserted=true;}
   else if(s.op==='assertBody'&&requested&&s.value?.trim())asserted=true;
   else throw new Error('INVALID_API_STEP');
  }else{
   if(w.version===1){if(!['title','add','items'].includes(s.target))throw new Error('INVALID_TARGET');}
   else if(s.op!=='navigate'&&!/^(testId:|css:|text:|role:).+/.test(s.target))throw new Error('INVALID_LOCATOR');
   if(s.op==='navigate'){if(w.version===1||!s.target.startsWith('/')||s.target.startsWith('//')||s.target.includes('..')||s.target.includes('\\'))throw new Error('INVALID_NAVIGATION');}
   else if(['fill','select'].includes(s.op)&&typeof s.value==='string'&&(w.version!==1||s.target==='title')){}
   else if(['click','check'].includes(s.op)&&s.value==null&&(w.version!==1||s.target==='add')){}
   else if(s.op==='assertText'&&s.value?.trim()&&(w.version!==1||s.target==='items'))asserted=true;
   else if(w.version===2&&s.op==='assertVisible')asserted=true;
   else if(w.version===2&&s.op==='assertCount'&&/^\d{1,4}$/.test(s.value))asserted=true;
   else throw new Error('INVALID_WEB_STEP');
  }
 }
 if(!asserted)throw new Error('ASSERTION_REQUIRED');return w;
}
export function compile(w){validate(w);let result=w.version===1?'// workflow-v1; compiler 0.1.0\n':'// workflow-v2; compiler 0.2.0\n';for(const s of w.steps){if(w.version===2){result+=`await step(${JSON.stringify(s.op)}, ${JSON.stringify(s.target)}, ${JSON.stringify(s.value??null)});\n`;continue;}const loc=`page.getByTestId(${JSON.stringify(s.target)})`;if(s.op==='fill')result+=`await ${loc}.fill(${JSON.stringify(s.value)});\n`;if(s.op==='click')result+=`await ${loc}.click();\n`;if(s.op==='assertText')result+=`await expect(${loc}).toContainText(${JSON.stringify(s.value)});\n`;}return result;}
export const specHash=w=>createHash('sha256').update(compile(w)).digest('hex');
