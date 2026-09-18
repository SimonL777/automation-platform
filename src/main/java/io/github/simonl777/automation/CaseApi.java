package io.github.simonl777.automation;
import io.github.simonl777.common.*;
import io.github.simonl777.contract.Workflow;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
@RestController @RequestMapping("/api/cases")
public class CaseApi {
 private final CaseStore cases;private final Jobs jobs;
 public CaseApi(CaseStore cases,Jobs jobs){this.cases=cases;this.jobs=jobs;}
 @GetMapping public Object list(Authentication auth){return cases.list(Access.owner(auth));}
 @PostMapping public Object create(@RequestBody CaseStore.Input input,Authentication auth){return cases.save(Access.owner(auth),null,input,false);}
 @GetMapping("/{id}") public Object get(@PathVariable String id,Authentication auth){return cases.get(Access.owner(auth),id);}
 @PutMapping("/{id}") public Object update(@PathVariable String id,@RequestBody CaseStore.Input input,Authentication auth){return cases.save(Access.owner(auth),id,input,false);}
 @GetMapping("/{id}/versions") public Object revisions(@PathVariable String id,Authentication auth){return cases.revisions(Access.owner(auth),id);}
 @PostMapping("/{id}/run") public ResponseEntity<?> run(@PathVariable String id,@RequestHeader("Idempotency-Key")String key,Authentication auth){var asset=cases.get(Access.owner(auth),id);var w=Json.decode(Json.write(asset.get("workflow")),Workflow.class).validate();var req=new RunRequest(null,w,null,null,null,(String)asset.get("title"),id);var payload=new LinkedHashMap<String,Object>();payload.put("kind","automation");payload.put("title",asset.get("title"));payload.put("demo",asset.get("demo"));payload.put("caseRevision",asset.get("revision"));payload.put("request",req);var task=jobs.submit(Access.owner(auth),key,payload);return ResponseEntity.status(task.created()?202:200).body(jobs.detail(task.job()));}
}
