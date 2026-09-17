import { createHash } from 'node:crypto';
export const html = `<!doctype html><html><head><meta charset="utf-8"><title>AI-SDLC synthetic task app</title></head><body><h1>Tasks</h1><label>Task <input data-testid="title"></label><button data-testid="add">Add</button><ul data-testid="items"></ul><script>document.querySelector('button').onclick=()=>{const input=document.querySelector('input');if(!input.value.trim())return;const li=document.createElement('li');li.textContent=input.value;document.querySelector('ul').append(li);input.value='';};</script></body></html>`;
export function validate(workflow) {
 if(!workflow||workflow.version!==1||workflow.target!=='todo-demo'||!Array.isArray(workflow.steps)||!workflow.steps.length||workflow.steps.length>20)throw new Error('INVALID_WORKFLOW');
 let assertion=false;
 for(const s of workflow.steps){
  if(!s||typeof s.op!=='string'||typeof s.target!=='string'||(s.value!=null&&(typeof s.value!=='string'||s.value.length>500)))throw new Error('INVALID_STEP');
  if(s.op==='fill'&&s.target==='title'&&typeof s.value==='string')continue;
  if(s.op==='click'&&s.target==='add'&&s.value==null)continue;
  if(s.op==='assertText'&&s.target==='items'&&typeof s.value==='string'&&s.value.trim()){assertion=true;continue;}
  throw new Error('UNSUPPORTED_STEP');
 }
 if(!assertion)throw new Error('ASSERTION_REQUIRED');return workflow;
}
export function compile(workflow){validate(workflow);let result='// workflow-v1; compiler 0.1.0\n';for(const s of workflow.steps){const loc=`page.getByTestId(${JSON.stringify(s.target)})`;if(s.op==='fill')result+=`await ${loc}.fill(${JSON.stringify(s.value)});\n`;if(s.op==='click')result+=`await ${loc}.click();\n`;if(s.op==='assertText')result+=`await expect(${loc}).toContainText(${JSON.stringify(s.value)});\n`;}return result;}
export function specHash(workflow){return createHash('sha256').update(compile(workflow)).digest('hex');}
