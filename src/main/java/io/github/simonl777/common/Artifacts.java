package io.github.simonl777.common;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class Artifacts {
 private final String mode,url,key,bucket;private final ArtifactCatalog catalog;private final Path dir;private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
 public Artifacts(@Value("${app.artifact-mode}")String mode,@Value("${app.artifact-dir}")String dir,@Value("${app.supabase-url}")String url,@Value("${app.supabase-service-key}")String key,@Value("${app.storage-bucket}")String bucket,ArtifactCatalog catalog){
  this.mode=mode;this.dir=Path.of(dir).toAbsolutePath().normalize();this.url=url.replaceAll("/$","");this.key=key;this.bucket=bucket;this.catalog=catalog;
  if(!mode.equals("local")&&!mode.equals("supabase"))throw new IllegalStateException("Invalid ARTIFACT_MODE");
  if(mode.equals("supabase")&&(!url.startsWith("https://")||key.isBlank()||!bucket.matches("[a-z0-9-]+")))throw new IllegalStateException("Supabase Storage configuration required");
 }
 private String path(String owner,String id,String name){if(!id.matches("[a-f0-9-]{36}")||!name.matches("[a-z0-9][a-z0-9._-]{0,80}"))throw new ApiFailure(400,"INVALID_ARTIFACT_PATH");return Json.hash(owner).substring(0,24)+"/"+id+"/"+name;}
 public java.util.Map<String,Object> configuration(){return java.util.Map.of("backend",mode,"bucket",bucket,"private",true);}
 public void put(String owner,String id,String name,byte[] bytes){write(owner,id,name,bytes);catalog.record(owner,id,name,bytes,mode);}
 private void write(String owner,String id,String name,byte[] bytes){if(bytes.length>8_000_000)throw new ApiFailure(413,"ARTIFACT_TOO_LARGE");String p=path(owner,id,name);
  try{if(mode.equals("local")){Path file=dir.resolve(p);Files.createDirectories(file.getParent());Files.write(file,bytes);return;}
   var req=HttpRequest.newBuilder(URI.create(url+"/storage/v1/object/"+bucket+"/"+p)).timeout(Duration.ofSeconds(30)).header("Authorization","Bearer "+key).header("apikey",key).header("x-upsert","true").header("Content-Type","application/octet-stream").POST(HttpRequest.BodyPublishers.ofByteArray(bytes)).build();
   int status=StorageHttp.send(http,req,HttpResponse.BodyHandlers.discarding()).statusCode();if(status/100!=2)throw new ApiFailure(502,"STORAGE_UPLOAD_FAILED");
  }catch(ApiFailure e){throw e;}catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();throw new ApiFailure(502,"STORAGE_UNAVAILABLE");}
 }
 public void putText(String owner,String id,String name,String text){put(owner,id,name,text.getBytes(StandardCharsets.UTF_8));}
 public byte[] get(String owner,String id,String name){String p=path(owner,id,name);try{
  if(mode.equals("local")){Path file=dir.resolve(p);if(!Files.exists(file))throw new ApiFailure(404,"ARTIFACT_NOT_FOUND");return Files.readAllBytes(file);}
  var req=HttpRequest.newBuilder(URI.create(url+"/storage/v1/object/authenticated/"+bucket+"/"+p)).timeout(Duration.ofSeconds(30)).header("Authorization","Bearer "+key).header("apikey",key).GET().build();
  var res=StorageHttp.send(http,req,HttpResponse.BodyHandlers.ofByteArray());if(res.statusCode()==404)throw new ApiFailure(404,"ARTIFACT_NOT_FOUND");if(res.statusCode()/100!=2)throw new ApiFailure(502,"STORAGE_READ_FAILED");if(res.body().length>8_000_000)throw new ApiFailure(502,"ARTIFACT_TOO_LARGE");return res.body();
 }catch(ApiFailure e){throw e;}catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();throw new ApiFailure(502,"STORAGE_UNAVAILABLE");}}
}
