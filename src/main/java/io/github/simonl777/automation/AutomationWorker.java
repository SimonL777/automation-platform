package io.github.simonl777.automation;
import io.github.simonl777.common.*;
import io.github.simonl777.contract.Workflow;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Component @ConditionalOnProperty(name="app.worker-enabled",havingValue="true",matchIfMissing=true)
public class AutomationWorker {
 private final Jobs jobs;private final Dependencies dependencies;private final Artifacts artifacts;private final GitWorkspace git;
 public AutomationWorker(Jobs jobs,Dependencies dependencies,Artifacts artifacts,GitWorkspace git){this.jobs=jobs;this.dependencies=dependencies;this.artifacts=artifacts;this.git=git;}
 @Scheduled(fixedDelay=1000) public void tick(){
  for(var stale:jobs.expired()){if(stale.remoteId()!=null)try{dependencies.cancel(stale.owner(),stale.remoteId());}catch(Exception ignored){}jobs.finish(stale,"LOST",null,"WORKER_LEASE_EXPIRED");}
  var job=jobs.claim(Duration.ofMinutes(6));if(job==null)return;
  try{
   JsonNode payload=Json.read(job.payload());
   if(payload.path("kind").asText().equals("studio")){runStudio(job,payload);return;}
   if(payload.path("kind").asText().equals("git")){var result=git.checkout(payload.path("repositoryKey").asText(),payload.path("ref").asText());artifacts.putText(job.owner(),job.id(),"repository.json",Json.write(result));jobs.finish(job,"SUCCEEDED",result,null);return;}
   RunRequest request=Json.decode(payload.path("request").toString(),RunRequest.class).validate(git);
   Map<String,Object> result=new LinkedHashMap<>();result.put("workspaceId",request.workspaceId());result.put("caseId",request.caseId());result.put("title",request.title());
   if(request.repositoryKey()!=null&&!request.repositoryKey().isBlank()){var source=git.checkout(request.repositoryKey(),request.ref()==null?"main":request.ref());result.put("repository",source);artifacts.putText(job.owner(),job.id(),"repository.json",Json.write(source));}
   Workflow workflow=request.workflow();
   if(workflow==null){JsonNode generated=dependencies.plan(job.owner(),request.requirement());workflow=Json.decode(generated.path("workflow").toString(),Workflow.class).validate();result.put("modelRequestId",generated.path("requestId").asText());result.put("providerMode",generated.path("providerMode").asText());}
   else result.put("providerMode","not-used");
   String spec=workflow.compile();result.put("workflowHash",workflow.hash());result.put("specHash",Json.hash(spec));result.put("compilerVersion",workflow.compilerVersion());result.put("workflow",workflow);
   artifacts.putText(job.owner(),job.id(),"workflow.json",Json.write(workflow));artifacts.putText(job.owner(),job.id(),"spec.js",spec);
   if(!jobs.active(job))return;
   JsonNode execution=dependencies.submit(job.owner(),job.id(),Map.of("workload",workflow.channel().equals("API")?"api-test":"web-test","workflow",workflow,"timeoutSeconds",60));String executionId=execution.path("id").asText();
   if(!jobs.attach(job,executionId)){dependencies.cancel(job.owner(),executionId);return;}
   long deadline=System.nanoTime()+Duration.ofSeconds(180).toNanos();
   while(true){
    if(!jobs.active(job)){dependencies.cancel(job.owner(),executionId);return;}
    execution=dependencies.get(job.owner(),executionId);String status=execution.path("status").asText();
    if(!Set.of("QUEUED","RUNNING").contains(status))break;
    if(System.nanoTime()>deadline){dependencies.cancel(job.owner(),executionId);throw new ApiFailure(504,"TRIAL_DEADLINE");}
    jobs.heartbeat(job,Duration.ofMinutes(6));try{Thread.sleep(1000);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new ApiFailure(503,"WORKER_INTERRUPTED");}
   }
   result.put("executionId",executionId);result.put("execution",execution);result.put("report",execution.path("result").path("report"));result.put("artifacts",List.of("workflow.json","spec.js","report.json"));
   artifacts.putText(job.owner(),job.id(),"report.json",Json.write(result));
   boolean hashMatches=Json.hash(spec).equals(execution.path("result").path("report").path("specHash").asText());boolean success=execution.path("status").asText().equals("SUCCEEDED")&&hashMatches;jobs.finish(job,success?"SUCCEEDED":"FAILED",result,success?null:(hashMatches?"TRIAL_FAILED":"COMPILED_ARTIFACT_MISMATCH"));
  }catch(ApiFailure e){jobs.finish(job,e.status==504?"TIMED_OUT":"FAILED",null,e.code);}
  catch(Exception e){jobs.finish(job,"FAILED",null,"AUTOMATION_FAILED");}
 }
 private void runStudio(Jobs.Job job,JsonNode payload){
  StudioRequest req=Json.decode(payload.path("request").toString(),StudioRequest.class).validate();
  artifacts.putText(job.owner(),job.id(),"input.md",req.markdown());
  if(req.source()!=null&&!req.source().isBlank())artifacts.putText(job.owner(),job.id(),"source.txt",req.source());
  JsonNode response=dependencies.capability(job.owner(),req);Map<String,Object> result=Json.MAPPER.convertValue(response,Map.class);
  if(req.capability().equals("ai-cr")){
   result.put("_meta",Map.of("contract","dd-open/v1","inputHash",Json.hash(Json.canonical(req)),"requirementHash",Json.hash(req.markdown()),"sourceHash",Json.hash(Objects.toString(req.source(),"")),"mode","review"));
   result.put("degraded_capabilities",List.of("AICR baseline not supplied","No repository-wide tool exploration in Markdown/source mode"));
   result.put("coverage",Map.of("requirementSource","input.md","fullFeatureSourceProvided",req.source()!=null&&!req.source().isBlank(),"baseSourceProvided",req.baseSource()!=null&&!req.baseSource().isBlank()));
   artifacts.putText(job.owner(),job.id(),"defect_detector_output.json",Json.write(result));
  }
  if(req.capability().equals("case-compile")){Workflow w=Json.decode(response.path("workflow").toString(),Workflow.class).validate();result.put("spec",w.compile());result.put("specHash",Json.hash(w.compile()));result.put("workflowHash",w.hash());result.put("compilerVersion",w.compilerVersion());artifacts.putText(job.owner(),job.id(),"workflow.json",Json.write(w));artifacts.putText(job.owner(),job.id(),"spec.js",w.compile());}
  artifacts.putText(job.owner(),job.id(),"output.md",response.path("markdown").asText());artifacts.putText(job.owner(),job.id(),"result.json",Json.write(result));
  result.put("artifacts",req.capability().equals("case-compile")?List.of("input.md","output.md","workflow.json","spec.js","result.json"):(req.capability().equals("ai-cr")?List.of("input.md","output.md","defect_detector_output.json","result.json"):List.of("input.md","output.md","result.json")));jobs.finish(job,"SUCCEEDED",result,null);
 }

}
