package io.github.simonl777.common;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
@Repository
public class ArtifactCatalog {
 private final JdbcTemplate db;private final String table;
 public ArtifactCatalog(JdbcTemplate db,@Value("${app.schema}")String schema){this.db=db;this.table=schema+".artifact_catalog";}
 public void record(String owner,String id,String name,byte[] data,String backend){String hash;try{hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data));}catch(Exception e){throw new IllegalStateException(e);}long now=System.currentTimeMillis();int updated=db.update("UPDATE "+table+" SET size_bytes=?,sha256=?,backend=?,created_at=? WHERE task_id=? AND name=? AND owner_id=?",data.length,hash,backend,now,id,name,owner);if(updated==0)db.update("INSERT INTO "+table+"(task_id,owner_id,name,size_bytes,sha256,backend,created_at) VALUES(?,?,?,?,?,?,?)",id,owner,name,data.length,hash,backend,now);}
 public List<Map<String,Object>> list(String owner){return db.query("SELECT * FROM "+table+" WHERE owner_id=? ORDER BY created_at DESC LIMIT 150",(r,n)->Map.<String,Object>of("taskId",r.getString("task_id"),"name",r.getString("name"),"size",r.getLong("size_bytes"),"sha256",r.getString("sha256"),"backend",r.getString("backend"),"createdAt",r.getLong("created_at")),owner);}
}
