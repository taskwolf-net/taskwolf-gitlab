package net.taskwolf.gitlab.access;

import net.taskwolf.core.access.TaskwolfRestController;
import net.taskwolf.core.database.condition.DatabaseCondition;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.gitlab.structure.GitlabDatabaseTable;
import net.taskwolf.workflow.WorkflowModule;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.UUID;

@RestController
public class GitlabEventController extends TaskwolfRestController {
  private final GitlabDatabaseTable gitlabDatabaseTable;
  private final WorkflowModule workflowModule;

  private GitlabEventController(
    Key productKey, UserDatabaseTable userDatabaseTable,
    GitlabDatabaseTable gitlabDatabaseTable, WorkflowModule workflowModule
  ) {
    super(productKey, userDatabaseTable);
    this.gitlabDatabaseTable = gitlabDatabaseTable;
    this.workflowModule = workflowModule;
  }

  @RequestMapping(path = "/gitlab/event/", method = RequestMethod.POST)
  public void processGitlabEvent(
    HttpServletRequest request, @RequestBody String payload
  ) {
    var state = request.getHeader("X-Gitlab-Token");
    if (state == null) {
      return;
    }
    var split = state.split("TASKWOLF-STATE-SPLIT");
    var gitlabId = UUID.fromString(split[0]);
    var projectId = split[1];
    var webhookSecret = split[2];
    gitlabDatabaseTable.gitlabExists(gitlabId)
      .thenAccept(exists -> processGitlabEvent(gitlabId, projectId,
        webhookSecret, payload, exists));
  }

  private void processGitlabEvent(
    UUID gitlabId, String projectId, String webhookSecret, String payload,
    boolean gitlabExists
  ) {
    if (!gitlabExists) {
      return;
    }
    var information = findTriggerInformation(new JSONObject(payload));
    var triggerType = (String) information.get("triggerType");
    if (triggerType.isEmpty()) {
      return;
    }
    information.remove("triggerType");
    workflowModule.triggerWorkflows("gitlab", triggerType,
      DatabaseCondition.of("gitlabId", gitlabId, "projectId", projectId,
        "webhookSecret", webhookSecret),
      information, false);
  }

  private Map<String, Object> findTriggerInformation(JSONObject payload) {
    return switch (payload.getString("object_kind")) {
      case "push" -> findCommitInformation(payload);
      case "issue" -> findIssueCreateInformation(payload);
      case "pipeline" -> findPipelineChangeInformation(payload);
      case "merge_request" -> findMergeRequestChangeInformation(payload);
      case "note" -> findNoteAddInformation(payload);
      default -> Map.of("triggerType", "");
    };
  }

  private Map<String, Object> findCommitInformation(JSONObject payload) {
    var commit = payload.getJSONArray("commits").getJSONObject(0);
    var author = commit.getJSONObject("author");
    var information = Maps.<String, Object>newHashMap();
    information.put("triggerType", "gitlab-commit-trigger");
    information.put("commitIdentifier", commit.getString("id"));
    information.put("commitReference", payload.getString("ref"));
    information.put("commitMessage", commit.getString("message"));
    information.put("commitAuthorName", author.getString("name"));
    information.put("commitAuthorEmail", author.getString("email"));
    return information;
  }

  private Map<String, Object> findIssueCreateInformation(JSONObject payload) {
    var attributes = payload.getJSONObject("object_attributes");
    var user = payload.getJSONObject("user");
    var information = Maps.<String, Object>newHashMap();
    information.put("triggerType", "gitlab-issue-change-trigger");
    information.put("issueIdentifier", String.valueOf(attributes.getInt("iid")));
    information.put("issueTitle", attributes.getString("title"));
    information.put("issueDescription", attributes.getString("description"));
    information.put("issueStatus", attributes.getString("state"));
    information.put("issueCreatorName", user.getString("name"));
    information.put("issueCreatorEmail", user.getString("email"));
    return information;
  }

  private Map<String, Object> findPipelineChangeInformation(JSONObject payload) {
    var attributes = payload.getJSONObject("object_attributes");
    var information = Maps.<String, Object>newHashMap();
    information.put("triggerType", "gitlab-pipeline-change-trigger");
    information.put("pipelineIdentifier", String.valueOf(attributes.getInt("id")));
    information.put("pipelineReference", attributes.getString("ref"));
    information.put("pipelineWebUrl", attributes.getString("url"));
    information.put("pipelineStatus", attributes.getString("status"));
    return information;
  }

  private Map<String, Object> findMergeRequestChangeInformation(JSONObject payload) {
    var attributes = payload.getJSONObject("object_attributes");
    var user = payload.getJSONObject("user");
    var information = Maps.<String, Object>newHashMap();
    information.put("triggerType", "gitlab-merge-request-change-trigger");
    information.put("mergeRequestIdentifier", String.valueOf(attributes.getInt("iid")));
    information.put("mergeRequestTitle", attributes.getString("title"));
    information.put("mergeRequestDescription", attributes.getString("description"));
    information.put("mergeRequestWebUrl", attributes.getString("url"));
    information.put("mergeRequestStatus", attributes.getString("state"));
    information.put("mergeRequestSourceBranch", attributes.getString("source_branch"));
    information.put("mergeRequestTargetBranch", attributes.getString("target_branch"));
    information.put("mergeRequestCreatorName", user.getString("name"));
    information.put("mergeRequestCreatorEmail", user.getString("email"));
    return information;
  }

  private Map<String, Object> findNoteAddInformation(JSONObject payload) {
    var attributes = payload.getJSONObject("object_attributes");
    var user = payload.getJSONObject("user");
    var information = Maps.<String, Object>newHashMap();
    information.put("triggerType", "gitlab-note-add-trigger");
    information.put("noteIdentifier", String.valueOf(attributes.getInt("id")));
    information.put("noteContent", attributes.getString("note"));
    information.put("noteableType", attributes.getString("noteable_type"));
    information.put("noteableIdentifier", String.valueOf(attributes.getInt("noteable_id")));
    information.put("noteAuthorName", user.getString("name"));
    information.put("noteAuthorEmail", user.getString("email"));
    return information;
  }
}
