import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.index.IssueIndexingService

//Variables
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def cField = customFieldManager.getCustomFieldObject (18600)
def cFieldPl = customFieldManager.getCustomFieldObject(20012)
def issue = ComponentAccessor.issueManager.getIssueObject(event.issue.getId())
def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
def klusaField = cField.getValue(issue)
def plText = issue.getCustomFieldValue(cFieldPl)
def options = ComponentAccessor.getOptionsManager().getOptions(cFieldPl.getRelevantConfig(event.issue))
Long secLevel = 0
def sendEmailWithChange = false

log.warn("SEC LEVEL ID: "+issue.getSecurityLevelId())

if(issue.getIssueType().getName() == "Project"){

//Associates pl value to a security level ID
if(plText == null){
    secLevel = null
}
else if(plText.getValue() == "32"){
secLevel = 10609
}
else if(plText.getValue() == "38"){
    secLevel = 10610
}
else if(plText.getValue() == "55"){
    secLevel = 10612
}
else{
    log.warn(plText.getValue() +" is not a valid security level")
    return
}

issue.setSecurityLevelId(secLevel)

log.warn("KLUSAFIELD: "+klusaField)
log.warn("PL Text: "+ plText.toString())
log.warn("SEC LEVEL: " + secLevel)

def priority = issue.getPriority()
def links = ComponentAccessor.getIssueLinkManager().getOutwardLinks(issue.id)
if(issue.getIssueType().getName() == "Project"){
    for (l in links){
        def board = ComponentAccessor.issueManager.getIssueByCurrentKey(l.getDestinationObject().getKey())
        if(plText == "32"){
            board.setCustomFieldValue(cFieldPl,options.get(0))
        }
        else if(plText == "38"){
            board.setCustomFieldValue(cFieldPl,options.get(1))
        }
        else if(plText == "55"){
            board.setCustomFieldValue(cFieldPl,options.get(2))
        }
        board.setSecurityLevelId(issue.getSecurityLevelId())
        ComponentAccessor.getIssueManager().updateIssue(user, board, EventDispatchOption.DO_NOT_DISPATCH,sendEmailWithChange )
        def subtasks = ComponentAccessor.getSubTaskManager().getSubTaskObjects(board)
        for(s in subtasks){
            def subtask = ComponentAccessor.getIssueManager().getIssueByCurrentKey(s.getKey())
            subtask.setSecurityLevelId(board.getSecurityLevelId())
            ComponentAccessor.getIssueManager().updateIssue(user, subtask, EventDispatchOption.DO_NOT_DISPATCH,sendEmailWithChange )
        }
    }
}

log.warn("SEC LEVEL ID: "+issue.getSecurityLevelId())

//Making the MutableIssue setted value permanent 
def indexingService = ComponentAccessor.getComponent(IssueIndexingService.class)
ComponentAccessor.getIssueManager().updateIssue(user, issue, EventDispatchOption.DO_NOT_DISPATCH,sendEmailWithChange )
indexingService.reIndex(issue)
}
