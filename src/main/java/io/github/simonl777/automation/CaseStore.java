package io.github.simonl777.automation;
import io.github.simonl777.common.*;
import io.github.simonl777.contract.Workflow;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
@Repository
public class CaseStore {
 public record Input(String title,String description,Workflow workflow,List<String> tags,Integer revision){}
 private final JdbcTemplate db;private final String schema;
 public CaseStore(JdbcTemplate db,@Value("${app.schema}")String schema){this.db=db;this.schema=schema;}
 private final RowMapper<Map<String,Object>> mapper=(r,n)->{Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getString("id"));m.put("title",r.getString("title"));m.put("channel",r.getString("channel"));m.put("description",r.getString("description"));m.put("workflow",Json.read(r.getString("workflow")));m.put("revision",r.getInt("revision"));m.put("status",r.getString("status"));m.put("tags",Json.read(r.getString("tags")));m.put("demo",r.getBoolean("demo"));m.put("createdAt",r.getLong("created_at"));m.put("updatedAt",r.getLong("updated_at"));return m;};
 public List<Map<String,Object>> list(String owner){return db.query("SELECT * FROM "+schema+".case_assets WHERE owner_id=? ORDER BY updated_at DESC LIMIT 200",mapper,owner);}
 public Map<String,Object> get(String owner,String id){return db.query("SELECT * FROM "+schema+".case_assets WHERE id=? AND owner_id=?",mapper,id,owner).stream().findFirst().orElseThrow(()->new ApiFailure(404,"CASE_NOT_FOUND"));}
 @Transactional public Map<String,Object> save(String owner,String id,Input input,boolean demo){
  if(input.title()==null||input.title().isBlank()||input.title().length()>160||input.workflow()==null||input.description()!=null&&input.description().length()>8000||input.tags()!=null&&(input.tags().size()>12||input.tags().stream().anyMatch(t->t==null||t.length()>40)))throw new ApiFailure(422,"INVALID_CASE");
  Workflow w=input.workflow().validate();long now=System.currentTimeMillis();int revision=1;
  if(id==null){id=UUID.randomUUID().toString();db.update("INSERT INTO "+schema+".case_assets VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",id,owner,input.title(),w.channel(),Objects.toString(input.description(),""),Json.write(w),1,"DRAFT",Json.write(input.tags()==null?List.of():input.tags()),demo,now,now);}
  else{Map<String,Object> old=get(owner,id);int current=(int)old.get("revision");if(input.revision()==null||input.revision()!=current)throw new ApiFailure(409,"CASE_REVISION_CONFLICT");revision=current+1;if(db.update("UPDATE "+schema+".case_assets SET title=?,channel=?,description=?,workflow=?,revision=?,tags=?,updated_at=? WHERE id=? AND owner_id=? AND revision=?",input.title(),w.channel(),Objects.toString(input.description(),""),Json.write(w),revision,Json.write(input.tags()==null?List.of():input.tags()),now,id,owner,current)!=1)throw new ApiFailure(409,"CASE_REVISION_CONFLICT");}
  db.update("INSERT INTO "+schema+".case_revisions VALUES(?,?,?,?,?)",id,revision,Json.write(w),w.hash(),now);return get(owner,id);
 }
 public List<Map<String,Object>> revisions(String owner,String id){get(owner,id);return db.query("SELECT revision,workflow_hash,created_at FROM "+schema+".case_revisions WHERE case_id=? ORDER BY revision DESC",(r,n)->Map.<String,Object>of("revision",r.getInt(1),"workflowHash",r.getString(2),"createdAt",r.getLong(3)),id);}
}
