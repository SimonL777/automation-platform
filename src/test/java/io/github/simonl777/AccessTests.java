package io.github.simonl777;
import io.github.simonl777.common.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class AccessTests {
 @Autowired MockMvc mvc;@Autowired Jobs jobs;
 @Test void healthIsPublicButArtifactsAreNot()throws Exception{mvc.perform(get("/actuator/health")).andExpect(status().isOk());mvc.perform(get("/api/artifacts/00000000-0000-0000-0000-000000000000/report.json")).andExpect(status().isUnauthorized());}
 @Test void ownerHeaderCannotOverrideOperatorIdentity()throws Exception{var j=jobs.submit("other-owner","access-"+System.nanoTime(),Map.of()).job();mvc.perform(get("/api/artifacts/"+j.id()+"/report.json").header("Authorization","Bearer test-operator-token-not-for-deployment").header("X-Owner-Id","other-owner")).andExpect(status().isNotFound());}
 @Test void serviceRequiresExplicitOwner()throws Exception{mvc.perform(get("/api/artifacts/00000000-0000-0000-0000-000000000000/report.json").header("Authorization","Bearer test-service-token-not-for-deployment")).andExpect(status().isUnauthorized());}
}
