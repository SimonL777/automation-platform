package io.github.simonl777;
import io.github.simonl777.automation.*;
import io.github.simonl777.common.*;
import io.github.simonl777.contract.Workflow;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
@SpringBootTest @ActiveProfiles("test")
class CaseAssetTests {
 @Autowired CaseStore cases;
 @Test void editCreatesImmutableVersionAndConflictingEditFails(){String owner=UUID.randomUUID().toString();var input=new CaseStore.Input("API product check","original",DemoApi.samples().get(6).workflow(),List.of("P0"),null);var first=cases.save(owner,null,input,false);String id=(String)first.get("id");var updated=cases.save(owner,id,new CaseStore.Input("Renamed","edited",input.workflow(),List.of(),1),false);assertEquals(2,updated.get("revision"));assertEquals(2,cases.revisions(owner,id).size());assertThrows(ApiFailure.class,()->cases.save(owner,id,new CaseStore.Input("Stale","",input.workflow(),List.of(),1),false));assertThrows(ApiFailure.class,()->cases.get("another-user",id));}
 @Test void allSeedCasesCompileAndCoverBothChannels(){var samples=DemoApi.samples();assertEquals(12,samples.size());assertEquals(6,samples.stream().filter(i->i.workflow().channel().equals("WEB")).count());assertEquals(6,samples.stream().filter(i->i.workflow().channel().equals("API")).count());for(var input:samples)assertFalse(input.workflow().validate().compile().isBlank());}
}
