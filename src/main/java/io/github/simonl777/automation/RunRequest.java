package io.github.simonl777.automation;
import io.github.simonl777.contract.Workflow;
import io.github.simonl777.common.*;
public record RunRequest(String requirement,Workflow workflow,String workspaceId,String repositoryKey,String ref){
 public RunRequest validate(GitWorkspace git){
  if(workflow==null&&(requirement==null||requirement.isBlank()))throw new ApiFailure(422,"REQUIREMENT_REQUIRED");
  if(requirement!=null&&requirement.length()>8000)throw new ApiFailure(413,"REQUIREMENT_TOO_LONG");if(workflow!=null)workflow.validate();
  if(workspaceId!=null&&!workspaceId.matches("[A-Za-z0-9_-]{1,128}"))throw new ApiFailure(422,"INVALID_WORKSPACE_REFERENCE");
  if(repositoryKey!=null&&!repositoryKey.isBlank())git.validate(repositoryKey,ref==null?"main":ref);return this;
 }
}
