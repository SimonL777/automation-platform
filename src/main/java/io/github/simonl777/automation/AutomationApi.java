package io.github.simonl777.automation;
import io.github.simonl777.common.*;
import io.github.simonl777.contract.Workflow;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
@RestController @RequestMapping("/api")
public class AutomationApi {
 private final Jobs jobs;private final GitWorkspace git;
 public AutomationApi(Jobs jobs,GitWorkspace git){this.jobs=jobs;this.git=git;}
 @PostMapping("/runs") public ResponseEntity<?> submit(@RequestBody RunRequest req,@RequestHeader("Idempotency-Key")String key,Authentication auth){var task=jobs.submit(Access.owner(auth),key,Map.of("kind","automation","request",req.validate(git)));return ResponseEntity.status(task.created()?202:200).body(Jobs.view(task.job()));}
 @GetMapping("/runs") public Object list(Authentication auth){return jobs.list(Access.owner(auth)).stream().filter(j->Json.read(j.payload()).path("kind").asText().equals("automation")).map(Jobs::view).toList();}
 @GetMapping("/runs/{id}") public Object get(@PathVariable String id,Authentication auth){return Jobs.view(jobs.get(id,Access.owner(auth)));}
 @PostMapping("/runs/{id}/cancel") public Object cancel(@PathVariable String id,Authentication auth){return Jobs.view(jobs.cancel(id,Access.owner(auth)));}
 @PostMapping("/compile") public Object compile(@RequestBody Workflow w){w.validate();String spec=w.compile();return Map.of("workflow",w,"workflowHash",w.hash(),"compilerVersion","0.1.0","spec",spec,"specHash",Json.hash(spec));}
 @GetMapping("/repositories") public Object repositories(){return Map.of("registered",git.keys());}
 public record GitRequest(String repositoryKey,String ref){}
 @PostMapping("/repository-tasks") public ResponseEntity<?> git(@RequestBody GitRequest req,@RequestHeader("Idempotency-Key")String key,Authentication auth){String ref=req.ref()==null?"main":req.ref();git.validate(req.repositoryKey(),ref);var sub=jobs.submit(Access.owner(auth),key,Map.of("kind","git","repositoryKey",req.repositoryKey(),"ref",ref));return ResponseEntity.status(sub.created()?202:200).body(Jobs.view(sub.job()));}
 @GetMapping("/repository-tasks/{id}") public Object gitResult(@PathVariable String id,Authentication auth){return Jobs.view(jobs.get(id,Access.owner(auth)));}
}
