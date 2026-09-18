package io.github.simonl777.automation;
import io.github.simonl777.common.*;
import io.github.simonl777.contract.Workflow;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
@RestController @RequestMapping("/api/demo")
public class DemoApi {
 private final CaseStore cases;
 public DemoApi(CaseStore cases){this.cases=cases;}
 private static Workflow.Step s(String op,String target,String value){return new Workflow.Step(op,target,value);}
 @PostMapping("/seed") public Object seed(Authentication auth){String owner=Access.owner(auth);List<Map<String,Object>> existing=cases.list(owner);int created=0;for(var sample:samples()){if(existing.stream().anyMatch(c->c.get("title").equals(sample.title())&&Boolean.TRUE.equals(c.get("demo"))))continue;cases.save(owner,null,sample,true);created++;}return Map.of("created",created,"cases",cases.list(owner));}
 public static List<CaseStore.Input> samples(){return List.of(
  web("任务创建 · 正向链路","P0",List.of(s("fill","testId:title","Prepare interview"),s("click","testId:add",null),s("assertText","testId:items","Prepare interview"))),
  web("空标题 · 输入校验","P1",List.of(s("click","testId:add",null),s("assertVisible","testId:error",null),s("assertCount","css:li","0"))),
  web("多任务 · 列表累加","P1",List.of(s("fill","testId:title","Review requirements"),s("click","testId:add",null),s("fill","testId:title","Run regression"),s("click","testId:add",null),s("assertCount","css:li","2"))),
  web("商品展示 · 目录内容","P1",List.of(s("assertText","testId:products","Mechanical Keyboard"),s("assertCount","css:.product","3"))),
  web("表单清空 · 提交后状态","P2",List.of(s("fill","testId:title","Write release notes"),s("click","testId:add",null),s("assertText","testId:counter","1 tasks"))),
  web("失败演练 · 缺失文本","P2",List.of(s("fill","testId:title","Actual task"),s("click","testId:add",null),s("assertText","testId:items","Text that does not exist"))),
  api("GET 商品列表 · 数据契约","P0",List.of(s("request","GET","{\"path\":\"/api/products\"}"),s("assertStatus","response","200"),s("assertJson","$.total","3"),s("assertJson","$.items[0].name","\"Mechanical Keyboard\""))),
  api("POST 任务 · 创建成功","P0",List.of(s("request","POST","{\"path\":\"/api/tasks\",\"body\":{\"title\":\"Prepare interview\"}}"),s("assertStatus","response","201"),s("assertJson","$.title","\"Prepare interview\""))),
  api("POST 任务 · 空值拒绝","P1",List.of(s("request","POST","{\"path\":\"/api/tasks\",\"body\":{\"title\":\"\"}}"),s("assertStatus","response","422"),s("assertJson","$.error","\"title_required\""))),
  api("POST 购物车 · 金额计算","P0",List.of(s("request","POST","{\"path\":\"/api/cart\",\"body\":{\"productId\":1,\"quantity\":2}}"),s("assertStatus","response","201"),s("assertJson","$.total","178"))),
  api("POST 购物车 · 无效数量","P1",List.of(s("request","POST","{\"path\":\"/api/cart\",\"body\":{\"productId\":1,\"quantity\":0}}"),s("assertStatus","response","422"),s("assertJson","$.error","\"invalid_quantity\""))),
  api("GET 不存在资源 · 404","P2",List.of(s("request","GET","{\"path\":\"/api/missing\"}"),s("assertStatus","response","404"),s("assertJson","$.error","\"not_found\"")))
 );}
 private static CaseStore.Input web(String title,String priority,List<Workflow.Step> steps){return input(title,priority,"WEB",steps);}
 private static CaseStore.Input api(String title,String priority,List<Workflow.Step> steps){return input(title,priority,"API",steps);}
 private static CaseStore.Input input(String title,String priority,String type,List<Workflow.Step> steps){return new CaseStore.Input(title,"Northstar 演示业务 · 独立环境与合成数据。用例可编辑、编译并实际执行。",new Workflow(2,type.toLowerCase()+":todo-demo",steps),List.of(priority,"Northstar","回归"),null);}
}
