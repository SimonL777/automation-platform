package io.github.simonl777;
import io.github.simonl777.common.*;
import io.github.simonl777.contract.Workflow;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
@SpringBootTest @ActiveProfiles("test")
class CoreTests {
 @Autowired Jobs jobs;@Autowired Artifacts artifacts;@Autowired JdbcTemplate db;@Value("${app.schema}")String schema;
 @BeforeEach void clean(){db.update("DELETE FROM "+schema+".jobs");}
 @Test void sameKeySamePayloadReturnsSameTask(){var first=jobs.submit("owner-a","key",Map.of("a",1,"b",2));var replay=jobs.submit("owner-a","key",new LinkedHashMap<>(Map.of("b",2,"a",1)));assertEquals(first.job().id(),replay.job().id());assertFalse(replay.created());assertThrows(ApiFailure.class,()->jobs.submit("owner-a","key",Map.of("a",2)));assertNotEquals(first.job().id(),jobs.submit("owner-b","key",Map.of("a",1)).job().id());}
 @Test void ownershipIsEnforced(){var j=jobs.submit("owner-a","key",Map.of()).job();assertThrows(ApiFailure.class,()->jobs.get(j.id(),"owner-b"));}
 @Test void concurrentClaimHasOneWinner()throws Exception{jobs.submit("owner-a","key",Map.of());try(var pool=Executors.newFixedThreadPool(8)){var barrier=new CyclicBarrier(8);List<Future<Jobs.Job>> futures=new ArrayList<>();for(int i=0;i<8;i++)futures.add(pool.submit(()->{barrier.await();return jobs.claim(Duration.ofMinutes(1));}));int winners=0;for(var f:futures)if(f.get(10,TimeUnit.SECONDS)!=null)winners++;assertEquals(1,winners);}}
 @Test void cancellationFencesLateCompletion(){var j=jobs.submit("owner-a","key",Map.of()).job();var lease=jobs.claim(Duration.ofMinutes(1));jobs.cancel(j.id(),"owner-a");assertFalse(jobs.finish(lease,"SUCCEEDED",Map.of(),null));jobs.cleanup(j.id(),"DESTROYED");assertEquals("CANCELLED",jobs.get(j.id(),"owner-a").status());assertEquals("DESTROYED",jobs.get(j.id(),"owner-a").cleanupStatus());}
 @Test void staleLeaseCannotFinishNewState(){var j=jobs.submit("owner-a","key",Map.of()).job();var lease=jobs.claim(Duration.ofMinutes(1));db.update("UPDATE "+schema+".jobs SET lease_id=? WHERE id=?",UUID.randomUUID().toString(),j.id());assertFalse(jobs.finish(lease,"SUCCEEDED",Map.of(),null));}
 @Test void artifactPathsAndOwnershipNamespacesAreBounded(){String id=UUID.randomUUID().toString();artifacts.putText("owner-a",id,"report.json","{}");assertEquals("{}",new String(artifacts.get("owner-a",id,"report.json")));assertThrows(ApiFailure.class,()->artifacts.get("owner-b",id,"report.json"));assertThrows(ApiFailure.class,()->artifacts.get("owner-a",id,"../../secrets"));}
 @Test void compilerIsDeterministicAndQuotesData(){var w=Workflow.example("task\"; throw new Error('x'); //");assertEquals(w.compile(),w.compile());assertTrue(w.compile().contains("task\\\";"));assertEquals(3,w.compile().lines().filter(l->l.startsWith("await ")).count());}
 @Test void unsupportedWorkflowFailsClosed(){assertThrows(ApiFailure.class,()->new Workflow(1,"todo-demo",List.of(new Workflow.Step("eval","items","process.exit()"))).validate());assertThrows(ApiFailure.class,()->new Workflow(1,"todo-demo",List.of(new Workflow.Step("click","add",null))).validate());}
}
