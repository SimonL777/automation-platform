package io.github.simonl777.automation;
import io.github.simonl777.common.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.*;
@RestController @RequestMapping("/api/overview")
public class OverviewApi {
 private final Jobs jobs;private final CaseStore cases;private final Artifacts artifacts;
 public OverviewApi(Jobs jobs,CaseStore cases,Artifacts artifacts){this.jobs=jobs;this.cases=cases;this.artifacts=artifacts;}
 @GetMapping public Object overview(Authentication auth){String owner=Access.owner(auth);var assets=cases.list(owner);return Map.of("metrics",jobs.summary(owner),"caseCount",assets.size(),"webCases",assets.stream().filter(c->c.get("channel").equals("WEB")).count(),"apiCases",assets.stream().filter(c->c.get("channel").equals("API")).count(),"storage",artifacts.configuration(),"recent",jobs.list(owner).stream().limit(8).map(Jobs::view).toList());}
}
