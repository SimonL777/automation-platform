package io.github.simonl777.automation;
import io.github.simonl777.common.*;
import java.util.*;
public record StudioRequest(String capability,String title,String markdown,String source,String baseSource,String diff,String channel){
 public StudioRequest validate(){if(!Set.of("ai-cr","case-generation","case-compile").contains(Objects.toString(capability,""))||markdown==null||markdown.isBlank()||Json.write(this).length()>42000||title!=null&&title.length()>160)throw new ApiFailure(422,"INVALID_STUDIO_REQUEST");return this;}
}
