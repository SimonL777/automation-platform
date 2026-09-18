package io.github.simonl777.common;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.util.concurrent.atomic.AtomicInteger;
class StorageHttpTests {
 @Test void retriesSameObjectBytesAfterTransientFailure()throws Exception{var count=new AtomicInteger();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/object",exchange->{byte[] body=exchange.getRequestBody().readAllBytes();assertEquals("artifact",new String(body));int status=count.incrementAndGet()==1?503:200;exchange.sendResponseHeaders(status,2);exchange.getResponseBody().write("ok".getBytes());exchange.close();});server.start();try{var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/object")).POST(HttpRequest.BodyPublishers.ofString("artifact")).build();assertEquals(200,StorageHttp.send(HttpClient.newHttpClient(),req,HttpResponse.BodyHandlers.ofByteArray()).statusCode());assertEquals(2,count.get());}finally{server.stop(0);}}
 @Test void doesNotRetryAuthorizationFailure()throws Exception{var count=new AtomicInteger();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/object",exchange->{count.incrementAndGet();exchange.sendResponseHeaders(403,-1);exchange.close();});server.start();try{var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/object")).GET().build();assertEquals(403,StorageHttp.send(HttpClient.newHttpClient(),req,HttpResponse.BodyHandlers.discarding()).statusCode());assertEquals(1,count.get());}finally{server.stop(0);}}
}
