package io.github.simonl777.common;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
public final class Processes {
 public record Result(int exitCode,String output){}
 public static Result run(List<String> args,Path cwd,Map<String,String> environment,Duration timeout,int maxBytes){
  Process process=null;Thread reader=null;ByteArrayOutputStream output=new ByteArrayOutputStream();java.util.concurrent.atomic.AtomicBoolean overflow=new java.util.concurrent.atomic.AtomicBoolean();
  try{
   ProcessBuilder b=new ProcessBuilder(args).redirectErrorStream(true);if(cwd!=null)b.directory(cwd.toFile());b.environment().putAll(environment);process=b.start();process.getOutputStream().close();Process p=process;
   reader=Thread.ofVirtual().start(()->{try(var in=p.getInputStream()){byte[] chunk=new byte[4096];int n;while((n=in.read(chunk))!=-1){synchronized(output){if(output.size()+n>maxBytes){overflow.set(true);kill(p);break;}output.write(chunk,0,n);}}}catch(IOException ignored){}});
   if(!process.waitFor(timeout.toMillis(),TimeUnit.MILLISECONDS)){kill(process);throw new ApiFailure(504,"PROCESS_TIMEOUT");}
   reader.join(2000);if(overflow.get())throw new ApiFailure(502,"PROCESS_OUTPUT_LIMIT");
   synchronized(output){return new Result(process.exitValue(),output.toString(StandardCharsets.UTF_8));}
  }catch(ApiFailure e){throw e;}catch(InterruptedException e){Thread.currentThread().interrupt();throw new ApiFailure(503,"PROCESS_INTERRUPTED");}catch(IOException e){throw new ApiFailure(503,"EXECUTOR_UNAVAILABLE");}finally{if(process!=null&&process.isAlive())kill(process);}
 }
 private static void kill(Process p){p.descendants().forEach(ProcessHandle::destroyForcibly);p.destroyForcibly();}
}
