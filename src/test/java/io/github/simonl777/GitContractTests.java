package io.github.simonl777;
import io.github.simonl777.automation.*;
import io.github.simonl777.common.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GitContractTests {
 @Test void callerCannotChooseArbitraryRepositoryOrOption(){var git=new GitWorkspace("{\"example\":\"https://github.com/example/example.git\"}");assertDoesNotThrow(()->git.validate("example","main"));assertThrows(ApiFailure.class,()->git.validate("unknown","main"));assertThrows(ApiFailure.class,()->git.validate("example","--upload-pack=evil"));assertThrows(ApiFailure.class,()->git.validate("example","../secret"));assertThrows(ApiFailure.class,()->git.validate("example","main; whoami"));}
 @Test void secretsAndLocalProtocolsAreNotAcceptedInUrls(){assertThrows(ApiFailure.class,()->new GitWorkspace("{\"x\":\"https://user:secret@example.com/repo\"}").validate("x","main"));assertThrows(ApiFailure.class,()->new GitWorkspace("{\"x\":\"file:///etc\"}").validate("x","main"));}
}
