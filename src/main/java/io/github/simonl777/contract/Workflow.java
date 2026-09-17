package io.github.simonl777.contract;
import java.util.*;
import io.github.simonl777.common.*;
public record Workflow(int version,String target,List<Step> steps){
 public record Step(String op,String target,String value){}
 public Workflow validate(){
  if(version!=1||!"todo-demo".equals(target)||steps==null||steps.isEmpty()||steps.size()>20)throw new ApiFailure(422,"INVALID_WORKFLOW");
  boolean assertion=false;
  for(Step step:steps){
   if(step==null||step.op()==null||step.target()==null)throw new ApiFailure(422,"INVALID_STEP");
   if(step.value()!=null&&step.value().length()>500)throw new ApiFailure(422,"STEP_VALUE_TOO_LONG");
   switch(step.op()){
    case "fill" -> {if(!step.target().equals("title")||step.value()==null)throw new ApiFailure(422,"INVALID_FILL");}
    case "click" -> {if(!step.target().equals("add")||step.value()!=null)throw new ApiFailure(422,"INVALID_CLICK");}
    case "assertText" -> {if(!step.target().equals("items")||step.value()==null||step.value().isBlank())throw new ApiFailure(422,"INVALID_ASSERTION");assertion=true;}
    default -> throw new ApiFailure(422,"UNSUPPORTED_STEP");
   }
  }
  if(!assertion)throw new ApiFailure(422,"ASSERTION_REQUIRED");return this;
 }
 public String compile(){
  validate();StringBuilder b=new StringBuilder("// workflow-v1; compiler 0.1.0\n");
  for(Step s:steps){String locator="page.getByTestId("+Json.write(s.target())+")";
   switch(s.op()){
    case "fill" -> b.append("await ").append(locator).append(".fill(").append(Json.write(s.value())).append(");\n");
    case "click" -> b.append("await ").append(locator).append(".click();\n");
    case "assertText" -> b.append("await expect(").append(locator).append(").toContainText(").append(Json.write(s.value())).append(");\n");
   }
  }return b.toString();
 }
 public String hash(){return Json.hash(Json.canonical(this));}
 public static Workflow example(String value){return new Workflow(1,"todo-demo",List.of(new Step("fill","title",value),new Step("click","add",null),new Step("assertText","items",value)));}
}
