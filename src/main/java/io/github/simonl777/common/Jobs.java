package io.github.simonl777.common;
import java.time.Duration;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
@Repository
public class Jobs {
 public record Job(String id,String owner,String key,String payload,String status,String lease,long leaseUntil,String remoteId,String cleanupStatus,String result,String error,long createdAt,long updatedAt){}
 public record Submitted(Job job,boolean created){}
 private final JdbcTemplate db;private final String table;
 private final RowMapper<Job> mapper=(r,n)->new Job(r.getString("id"),r.getString("owner_id"),r.getString("idempotency_key"),r.getString("payload"),r.getString("status"),r.getString("lease_id"),r.getLong("lease_until"),r.getString("remote_id"),r.getString("cleanup_status"),r.getString("result"),r.getString("error_code"),r.getLong("created_at"),r.getLong("updated_at"));
 public Jobs(JdbcTemplate db,@Value("${app.schema}")String schema){if(!schema.matches("[a-z][a-z0-9_]{0,50}"))throw new IllegalStateException("Invalid DB_SCHEMA");this.db=db;this.table=schema+".jobs";}
 public Submitted submit(String owner,String key,Object payload){
  if(key==null||!key.matches("[A-Za-z0-9._:-]{1,128}"))throw new ApiFailure(400,"INVALID_IDEMPOTENCY_KEY");
  String text=Json.canonical(payload),hash=Json.hash(text),id=UUID.randomUUID().toString();long now=System.currentTimeMillis();
  try{db.update("INSERT INTO "+table+"(id,owner_id,idempotency_key,request_hash,payload,status,created_at,updated_at) VALUES(?,?,?,?,?,'QUEUED',?,?)",id,owner,key,hash,text,now,now);return new Submitted(get(id,owner),true);}
  catch(DuplicateKeyException e){
   List<Job> rows=db.query("SELECT * FROM "+table+" WHERE owner_id=? AND idempotency_key=?",mapper,owner,key);
   if(rows.isEmpty())throw e;
   Job existing=rows.getFirst();if(!Json.hash(existing.payload()).equals(hash))throw new ApiFailure(409,"IDEMPOTENCY_PAYLOAD_MISMATCH");return new Submitted(existing,false);
  }
 }
 public Job get(String id,String owner){return db.query("SELECT * FROM "+table+" WHERE id=? AND owner_id=?",mapper,id,owner).stream().findFirst().orElseThrow(()->new ApiFailure(404,"NOT_FOUND"));}
 public List<Job> list(String owner){return db.query("SELECT * FROM "+table+" WHERE owner_id=? ORDER BY created_at DESC LIMIT 50",mapper,owner);}
 @Transactional public Job claim(Duration duration){
  List<Job> rows=db.query("SELECT * FROM "+table+" WHERE status='QUEUED' ORDER BY created_at LIMIT 1",mapper);if(rows.isEmpty())return null;
  Job j=rows.getFirst();String lease=UUID.randomUUID().toString();long now=System.currentTimeMillis();
  if(db.update("UPDATE "+table+" SET status='RUNNING',lease_id=?,lease_until=?,updated_at=? WHERE id=? AND status='QUEUED'",lease,now+duration.toMillis(),now,j.id())!=1)return null;
  return get(j.id(),j.owner());
 }
 public Job start(String id,String owner,Duration duration){Job j=get(id,owner);String lease=UUID.randomUUID().toString();long now=System.currentTimeMillis();if(db.update("UPDATE "+table+" SET status='RUNNING',lease_id=?,lease_until=?,updated_at=? WHERE id=? AND owner_id=? AND status='QUEUED'",lease,now+duration.toMillis(),now,id,owner)!=1)throw new ApiFailure(409,"TASK_NOT_QUEUED");return get(id,owner);}
 public boolean attach(Job j,String remoteId){return db.update("UPDATE "+table+" SET remote_id=?,updated_at=? WHERE id=? AND lease_id=? AND status='RUNNING'",remoteId,System.currentTimeMillis(),j.id(),j.lease())==1;}
 public boolean heartbeat(Job j,Duration duration){long now=System.currentTimeMillis();return db.update("UPDATE "+table+" SET lease_until=?,updated_at=? WHERE id=? AND lease_id=? AND status='RUNNING'",now+duration.toMillis(),now,j.id(),j.lease())==1;}
 public boolean finish(Job j,String state,Object result,String code){if(!Set.of("SUCCEEDED","FAILED","TIMED_OUT","LOST").contains(state))throw new IllegalArgumentException("terminal state");return db.update("UPDATE "+table+" SET status=?,result=?,error_code=?,lease_until=0,updated_at=? WHERE id=? AND lease_id=? AND status='RUNNING'",state,result==null?null:Json.write(result),code,System.currentTimeMillis(),j.id(),j.lease())==1;}
 public Job cancel(String id,String owner){get(id,owner);db.update("UPDATE "+table+" SET status='CANCELLED',updated_at=? WHERE id=? AND owner_id=? AND status IN ('QUEUED','RUNNING')",System.currentTimeMillis(),id,owner);return get(id,owner);}
 public List<Job> expired(){return db.query("SELECT * FROM "+table+" WHERE status='RUNNING' AND lease_until<?",mapper,System.currentTimeMillis());}
 public boolean active(Job j){Job now=get(j.id(),j.owner());return now.status().equals("RUNNING")&&Objects.equals(now.lease(),j.lease());}
 public void cleanup(String id,String state){if(!Set.of("PENDING","DESTROYED","FAILED","NOT_REQUIRED").contains(state))throw new IllegalArgumentException();db.update("UPDATE "+table+" SET cleanup_status=?,updated_at=? WHERE id=?",state,System.currentTimeMillis(),id);}
 public List<Job> pendingCleanup(){return db.query("SELECT * FROM "+table+" WHERE cleanup_status IN ('PENDING','FAILED') AND status NOT IN ('QUEUED','RUNNING') ORDER BY updated_at LIMIT 20",mapper);}
 public Map<String,Object> detail(Job j){Map<String,Object> out=new LinkedHashMap<>(view(j));out.put("input",Json.read(j.payload()));return out;}
 public Map<String,Object> summary(String owner){var all=list(owner);long completed=all.stream().filter(j->j.status().equals("SUCCEEDED")||j.status().equals("FAILED")).count();return Map.of("total",all.size(),"running",all.stream().filter(j->j.status().equals("RUNNING")).count(),"queued",all.stream().filter(j->j.status().equals("QUEUED")).count(),"succeeded",all.stream().filter(j->j.status().equals("SUCCEEDED")).count(),"failed",all.stream().filter(j->Set.of("FAILED","TIMED_OUT","LOST").contains(j.status())).count(),"successRate",completed==0?0:Math.round(100.0*all.stream().filter(j->j.status().equals("SUCCEEDED")).count()/completed));}
 public static Map<String,Object> view(Job j){Map<String,Object> out=new LinkedHashMap<>();out.put("id",j.id());out.put("status",j.status());out.put("createdAt",j.createdAt());out.put("updatedAt",j.updatedAt());out.put("remoteId",j.remoteId());out.put("cleanupStatus",j.cleanupStatus());out.put("attemptId",j.lease());out.put("result",j.result()==null?null:Json.read(j.result()));out.put("error",j.error());var payload=Json.read(j.payload());out.put("kind",payload.path("kind").asText("execution"));out.put("title",payload.path("title").asText(payload.path("request").path("title").asText(payload.path("workload").asText("Run "+j.id().substring(0,8)))));out.put("durationMs",Math.max(0,j.updatedAt()-j.createdAt()));out.put("demo",payload.path("demo").asBoolean(false));out.put("channel",payload.path("request").path("workflow").path("target").asText(payload.path("workflow").path("target").asText()).startsWith("api:")?"API":"WEB");return out;}
}
