package io.github.simonl777.automation;
import io.github.simonl777.common.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.JsonNode;
@Component
public class Dependencies {
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
 private final String model,sandbox,modelToken,sandboxToken;
 public Dependencies(@Value("${automation.model-url}")String model,@Value("${automation.sandbox-url}")String sandbox,@Value("${automation.model-token}")String mt,@Value("${automation.sandbox-token}")String st){this.model=base(model);this.sandbox=base(sandbox);this.modelToken=mt;this.sandboxToken=st;}
 private static String base(String value){URI u=URI.create(value);if(!Set.of("http","https").contains(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getQuery()!=null)throw new IllegalStateException("Invalid dependency URL");return value.replaceAll("/$","");}
 public JsonNode capability(String owner,Object input){return call(model+"/v1/capabilities",modelToken,owner,"POST",input,null,60);}
 public JsonNode plan(String owner,String requirement){return call(model+"/v1/plans",modelToken,owner,"POST",Map.of("requirement",requirement),null,55);}
 public JsonNode submit(String owner,String id,Object body){return call(sandbox+"/api/executions",sandboxToken,owner,"POST",body,"automation-"+id,15);}
 public JsonNode get(String owner,String id){validId(id);return call(sandbox+"/api/executions/"+id,sandboxToken,owner,"GET",null,null,10);}
 public JsonNode cancel(String owner,String id){validId(id);return call(sandbox+"/api/executions/"+id+"/cancel",sandboxToken,owner,"POST",Map.of(),null,10);}
 private static void validId(String id){try{UUID.fromString(id);}catch(Exception e){throw new ApiFailure(502,"INVALID_DEPENDENCY_ID");}}
 private JsonNode call(String url,String token,String owner,String method,Object body,String key,int timeout){
  if(token.isBlank())throw new ApiFailure(503,"DEPENDENCY_TOKEN_NOT_CONFIGURED");
  try{var b=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(timeout)).header("Authorization","Bearer "+token).header("X-Owner-Id",owner).header("Content-Type","application/json");if(key!=null)b.header("Idempotency-Key",key);b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(Json.write(body),StandardCharsets.UTF_8));
   var r=http.send(b.build(),HttpResponse.BodyHandlers.ofByteArray());if(r.body().length>2_000_000)throw new ApiFailure(502,"DEPENDENCY_RESPONSE_TOO_LARGE");if(r.statusCode()/100!=2)throw new ApiFailure(502,"DEPENDENCY_HTTP_"+r.statusCode());return Json.read(new String(r.body(),StandardCharsets.UTF_8));
  }catch(ApiFailure e){throw e;}catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();throw new ApiFailure(502,"DEPENDENCY_UNAVAILABLE");}
 }
}
