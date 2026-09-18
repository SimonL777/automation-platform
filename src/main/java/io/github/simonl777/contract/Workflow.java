package io.github.simonl777.contract;

import io.github.simonl777.common.*;
import java.util.*;
import java.net.URI;

/** Version 1 remains byte-for-byte compilable. Version 2 adds web and HTTP test contracts. */
public record Workflow(int version,String target,List<Step> steps) {
 public record Step(String op,String target,String value) {}
 public static final String COMPILER="0.2.0";
 public String channel(){return target!=null&&target.startsWith("api:")?"API":"WEB";}
 public String profile(){return target!=null&&target.contains(":")?target.substring(target.indexOf(':')+1):target;}
 public Workflow validate(){
  if((version!=1&&version!=2)||target==null||steps==null||steps.isEmpty()||steps.size()>60)throw new ApiFailure(422,"INVALID_WORKFLOW");
  if(version==1&&!target.equals("todo-demo"))throw new ApiFailure(422,"INVALID_TARGET");
  if(version==2&&!target.matches("(web|api):[a-zA-Z0-9_-]{1,64}"))throw new ApiFailure(422,"INVALID_TARGET_PROFILE");
  boolean assertion=false,requested=false;
  for(Step step:steps){
   if(step==null||step.op()==null||step.target()==null||step.target().length()>500||step.value()!=null&&step.value().length()>16000)throw new ApiFailure(422,"INVALID_STEP");
   String op=step.op(),t=step.target(),v=step.value();
   if(channel().equals("API")){
    switch(op){
     case "request" -> {if(!Set.of("GET","POST","PUT","PATCH","DELETE").contains(t)||v==null)throw new ApiFailure(422,"INVALID_HTTP_METHOD");var request=Json.read(v);String path=request.path("path").asText();if(!path.startsWith("/")||path.startsWith("//")||path.contains("\\")||path.contains("..")||URI.create(path).isAbsolute())throw new ApiFailure(422,"INVALID_HTTP_PATH");if(request.has("headers")){if(!request.get("headers").isObject())throw new ApiFailure(422,"INVALID_HEADERS");request.get("headers").fieldNames().forEachRemaining(h->{if(Set.of("host","authorization","cookie","proxy-authorization","connection").contains(h.toLowerCase()))throw new ApiFailure(422,"HEADER_REQUIRES_AUTH_PROFILE");});}requested=true;}
     case "assertStatus" -> {if(!requested||v==null||!v.matches("[1-5][0-9]{2}"))throw new ApiFailure(422,"INVALID_STATUS_ASSERTION");assertion=true;}
     case "assertJson" -> {if(!requested||!t.matches("\\$?(\\.[a-zA-Z0-9_-]+|\\[[0-9]+\\])*")||v==null)throw new ApiFailure(422,"INVALID_JSON_ASSERTION");Json.read(v);assertion=true;}
     case "assertBody" -> {if(!requested||v==null||v.isBlank())throw new ApiFailure(422,"INVALID_BODY_ASSERTION");assertion=true;}
     default -> throw new ApiFailure(422,"UNSUPPORTED_API_STEP");
    }
   }else{
    if(version==1){if(!Set.of("title","add","items").contains(t))throw new ApiFailure(422,"INVALID_TARGET");}
    else if(!op.equals("navigate")&&(t.isBlank()||!t.matches("(testId:|css:|text:|role:).+")))throw new ApiFailure(422,"INVALID_LOCATOR");
    switch(op){
     case "navigate" -> {if(version==1||!t.startsWith("/")||t.startsWith("//")||t.contains("..")||t.contains("\\"))throw new ApiFailure(422,"INVALID_NAVIGATION");}
     case "fill","select" -> {if(v==null||version==1&&!t.equals("title"))throw new ApiFailure(422,"INVALID_FILL");}
     case "click","check" -> {if(v!=null||version==1&&!t.equals("add"))throw new ApiFailure(422,"INVALID_CLICK");}
     case "assertText" -> {if(v==null||v.isBlank()||version==1&&!t.equals("items"))throw new ApiFailure(422,"INVALID_ASSERTION");assertion=true;}
     case "assertVisible","assertCount" -> {if(version==1||op.equals("assertCount")&&(v==null||!v.matches("[0-9]{1,4}")))throw new ApiFailure(422,"INVALID_ASSERTION");assertion=true;}
     default -> throw new ApiFailure(422,"UNSUPPORTED_WEB_STEP");
    }
   }
  }
  if(!assertion)throw new ApiFailure(422,"ASSERTION_REQUIRED");return this;
 }
 public String compile(){
  validate();StringBuilder b=new StringBuilder(version==1?"// workflow-v1; compiler 0.1.0\n":"// workflow-v2; compiler 0.2.0\n");
  for(Step s:steps){if(version==2){b.append("await step(").append(Json.write(s.op())).append(", ").append(Json.write(s.target())).append(", ").append(Json.write(s.value())).append(");\n");continue;}
   String locator="page.getByTestId("+Json.write(s.target())+")";
   switch(s.op()){
    case "fill"->b.append("await ").append(locator).append(".fill(").append(Json.write(s.value())).append(");\n");
    case "click"->b.append("await ").append(locator).append(".click();\n");
    case "assertText"->b.append("await expect(").append(locator).append(").toContainText(").append(Json.write(s.value())).append(");\n");
   }
  }return b.toString();
 }
 public String compilerVersion(){return version==1?"0.1.0":COMPILER;}
 public String hash(){return Json.hash(Json.canonical(this));}
 public static Workflow example(String value){return new Workflow(1,"todo-demo",List.of(new Step("fill","title",value),new Step("click","add",null),new Step("assertText","items",value)));}
}
