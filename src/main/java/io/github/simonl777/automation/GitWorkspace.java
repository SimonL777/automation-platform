package io.github.simonl777.automation;
import io.github.simonl777.common.*;
import java.nio.file.*;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
@Component
public class GitWorkspace {
 private final Map<String,String> repositories;
 public GitWorkspace(@Value("${automation.git-repositories:{}}")String json){try{repositories=Json.MAPPER.readValue(json.isBlank()?"{}":json,new com.fasterxml.jackson.core.type.TypeReference<Map<String,String>>(){});}catch(Exception e){throw new IllegalStateException("Invalid GIT_REPOSITORIES_JSON",e);}}
 public Set<String> keys(){return repositories.keySet();}
 public void validate(String key,String ref){
  if(key==null||!repositories.containsKey(key))throw new ApiFailure(422,"REPOSITORY_NOT_REGISTERED");
  if(ref==null||!ref.matches("[A-Za-z0-9][A-Za-z0-9._/-]{0,119}")||ref.contains("..")||ref.endsWith("/"))throw new ApiFailure(422,"INVALID_GIT_REF");
  URI u=URI.create(repositories.get(key));if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null)throw new ApiFailure(422,"REPOSITORY_HTTPS_REQUIRED");
 }
 public Map<String,Object> checkout(String key,String ref){
  validate(key,ref);Path workspace=null;Map<String,Object> outcome=new LinkedHashMap<>();
  try{
   workspace=Files.createTempDirectory("ai-sdlc-git-");Path checkout=workspace.resolve("repo");
   List<String> clone=command("clone","--no-checkout","--depth","1","--no-recurse-submodules","--",repositories.get(key),checkout.toString());run(clone,workspace,45);
   run(command("fetch","--depth","1","origin",ref),checkout,30);
   run(command("checkout","--detach","FETCH_HEAD"),checkout,10);
   String sha=run(command("rev-parse","HEAD"),checkout,5).trim();if(!sha.matches("[a-f0-9]{40,64}"))throw new ApiFailure(502,"INVALID_COMMIT_SHA");
   String files=run(command("ls-tree","--name-only","HEAD"),checkout,5);
   outcome.putAll(Map.of("repositoryKey",key,"requestedRef",ref,"commitSha",sha,"rootFiles",files.lines().limit(100).toList()));return outcome;
  }catch(ApiFailure e){throw e;}catch(Exception e){throw new ApiFailure(502,"GIT_WORKSPACE_FAILED");}
  finally{if(workspace!=null)try(var paths=Files.walk(workspace)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);outcome.put("workspaceRetained",false);outcome.put("cleanupStatus","DELETED");}catch(Exception ignored){outcome.put("workspaceRetained",true);outcome.put("cleanupStatus","FAILED");}}
 }
 private static List<String> command(String...parts){var c=new ArrayList<>(List.of("git","-c","core.hooksPath=/dev/null","-c","credential.helper=","-c","http.followRedirects=false","-c","protocol.file.allow=never","-c","protocol.ext.allow=never","-c","submodule.recurse=false"));c.addAll(List.of(parts));return c;}
 private static String run(List<String> args,Path cwd,int timeout){var result=Processes.run(args,cwd,Map.of("GIT_TERMINAL_PROMPT","0","GIT_CONFIG_GLOBAL","/dev/null","GIT_CONFIG_SYSTEM","/dev/null"),Duration.ofSeconds(timeout),512_000);if(result.exitCode()!=0)throw new ApiFailure(502,"GIT_OPERATION_FAILED");return result.output();}
}
