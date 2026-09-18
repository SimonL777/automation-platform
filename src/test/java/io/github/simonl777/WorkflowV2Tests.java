package io.github.simonl777;
import io.github.simonl777.common.*;
import io.github.simonl777.contract.Workflow;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class WorkflowV2Tests {
 @Test void apiMustRequestBeforeAsserting(){assertThrows(ApiFailure.class,()->new Workflow(2,"api:todo-demo",List.of(new Workflow.Step("assertStatus","response","200"))).validate());}
 @Test void noUnregisteredUrlOrAuthorizationInsideWorkflow(){assertThrows(ApiFailure.class,()->new Workflow(2,"api:https://example.com",List.of()).validate());assertThrows(ApiFailure.class,()->new Workflow(2,"api:todo-demo",List.of(new Workflow.Step("request","GET","{\"path\":\"//internal/\"}"),new Workflow.Step("assertStatus","response","200"))).validate());assertThrows(ApiFailure.class,()->new Workflow(2,"api:todo-demo",List.of(new Workflow.Step("request","GET","{\"path\":\"/api/tasks\",\"headers\":{\"Authorization\":\"secret\"}}"),new Workflow.Step("assertStatus","response","200"))).validate());}
 @Test void apiCompilesRequestsAndJsonAssertionsDeterministically(){var w=new Workflow(2,"api:todo-demo",List.of(new Workflow.Step("request","GET","{\"path\":\"/api/products\"}"),new Workflow.Step("assertStatus","response","200"),new Workflow.Step("assertJson","$.items[0].name","\"Mechanical Keyboard\"")));assertEquals("API",w.validate().channel());assertEquals("0.2.0",w.compilerVersion());assertTrue(w.compile().contains("await step(\"assertJson\""));assertEquals(w.compile(),w.compile());}
 @Test void webDataCannotBecomeCode(){var w=new Workflow(2,"web:todo-demo",List.of(new Workflow.Step("fill","testId:title","';throw new Error(1);//"),new Workflow.Step("assertVisible","css:li",null)));assertEquals(2,w.compile().lines().filter(x->x.startsWith("await step")).count());assertThrows(ApiFailure.class,()->new Workflow(2,"web:todo-demo",List.of(new Workflow.Step("evaluate","css:body","x"))).validate());}
}
