package io.github.simonl777.common;
import java.net.http.*;
import java.io.IOException;
/** Retry only replayable object-storage operations; never retries model calls or business tasks. */
final class StorageHttp {
 static <T> HttpResponse<T> send(HttpClient client,HttpRequest request,HttpResponse.BodyHandler<T> body)throws IOException,InterruptedException{
  for(int attempt=0;;attempt++){
   try{HttpResponse<T> response=client.send(request,body);int status=response.statusCode();if(attempt==1||!(status==429||status>=500))return response;}
   catch(IOException e){if(attempt==1)throw e;}
   Thread.sleep(350);
  }
 }
}
