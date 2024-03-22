import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.component.ComponentAccessor

//Variables and Context
def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
def issueManager = ComponentAccessor.getIssueManager()
def links = ComponentAccessor.getIssueLinkManager().getLinkCollection(issue,user).allIssues
def reasonCf = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(16900)
MutableIssue meeting = null
def desc = null
String nDesc = null

for (l in links){
    if(l.getIssueType().getName() == "Meeting"){
        meeting = issueManager.getIssueObject(l.getId())
        desc = meeting.getDescription()
        log.warn("OLD DESCRP: "+ desc)
        if(desc == null){
            meeting.setDescription( "Meeting notes: ")
            desc = meeting.getDescription()
        }
        log.warn("NEW DESC: " + desc)
        log.warn("MEETING: " + meeting)
        if(!desc.contains(issue.getCustomFieldValue(reasonCf))){
            nDesc = desc + "\n\n " + issue.getSummary() + ": " + issue.getCustomFieldValue(reasonCf) + "\n "
            meeting.setDescription(nDesc)
            log.warn("NEW DESC: "+ nDesc)
        }
        
    }
}
issueManager.updateIssue(user, meeting, EventDispatchOption.ISSUE_UPDATED, false )
