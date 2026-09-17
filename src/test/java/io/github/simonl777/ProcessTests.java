package io.github.simonl777;
import io.github.simonl777.common.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ProcessTests {
 @Test void deadlineStopsAChildProcess(){var e=assertThrows(ApiFailure.class,()->Processes.run(List.of("sh","-c","sleep 5"),null,Map.of(),Duration.ofMillis(100),1024));assertEquals("PROCESS_TIMEOUT",e.code);}
 @Test void outputIsBounded(){var e=assertThrows(ApiFailure.class,()->Processes.run(List.of("sh","-c","while true; do echo payload; done"),null,Map.of(),Duration.ofSeconds(3),100));assertEquals("PROCESS_OUTPUT_LIMIT",e.code);}
}
