package io.github.simonl777.automation;
import io.github.simonl777.common.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
@RestController @RequestMapping("/api/studio")
public class StudioApi {
 private final Jobs jobs;
 public StudioApi(Jobs jobs){this.jobs=jobs;}
 @PostMapping("/runs") public ResponseEntity<?> create(@RequestBody StudioRequest req,@RequestHeader("Idempotency-Key")String key,Authentication auth){req.validate();var task=jobs.submit(Access.owner(auth),key,Map.of("kind","studio","title",Objects.toString(req.title(),req.capability()),"request",req));return ResponseEntity.status(task.created()?202:200).body(jobs.detail(task.job()));}
 @GetMapping("/runs") public Object list(Authentication auth){return jobs.list(Access.owner(auth)).stream().filter(j->Json.read(j.payload()).path("kind").asText().equals("studio")).map(jobs::detail).toList();}
 @GetMapping("/runs/{id}") public Object get(@PathVariable String id,Authentication auth){var task=jobs.get(id,Access.owner(auth));if(!Json.read(task.payload()).path("kind").asText().equals("studio"))throw new ApiFailure(404,"NOT_FOUND");return jobs.detail(task);}
}
