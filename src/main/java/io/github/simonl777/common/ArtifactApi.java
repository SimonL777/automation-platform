package io.github.simonl777.common;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
@RestController
public class ArtifactApi {
 private final Jobs jobs;private final Artifacts artifacts;
 public ArtifactApi(Jobs jobs,Artifacts artifacts){this.jobs=jobs;this.artifacts=artifacts;}
 @GetMapping("/api/artifacts/{id}/{name}") public ResponseEntity<byte[]> read(@PathVariable String id,@PathVariable String name,Authentication auth){String owner=Access.owner(auth);jobs.get(id,owner);return ResponseEntity.ok().header("X-Content-Type-Options","nosniff").header("Content-Disposition","attachment; filename=\""+name.replaceAll("[^A-Za-z0-9._-]","")+"\"").contentType(MediaType.APPLICATION_OCTET_STREAM).body(artifacts.get(owner,id,name));}
}
