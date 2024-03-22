import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.event.type.EventDispatchOption


//Variables and Context
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def cFieldAgenda = customFieldManager.getCustomFieldObject(18900)
def cFieldPresenter = customFieldManager.getCustomFieldObject(18612)
def cFieldPl = customFieldManager.getCustomFieldObject(20012)
def cFieldTopic = customFieldManager.getCustomFieldObject(20006)
def cFieldPersonToBeInvited = customFieldManager.getCustomFieldObject(20001)
def cFieldDuration = customFieldManager.getCustomFieldObject(20005)

def agendaTable = "||PL||Topic||Approval||Responsible||Add. Contributors||Timeslot||Duration||Comment||Link to presentation||\n"
def approvalIssues = getRelatedIssues(issue, ["Approval", "Change Request"])

for (approvalIssue in approvalIssues) {
    def usernames = []
    for (user in approvalIssue.getCustomFieldValue(cFieldPresenter)) {
        usernames.add(user.getDisplayName())
    }
    def usernames2 = []
        for (user in approvalIssue.getCustomFieldValue(cFieldPersonToBeInvited)) {
        usernames2.add(user.getDisplayName())
    }

    def duration = approvalIssue.getCustomFieldValue(cFieldDuration).toString()
    def pl = "No linked project on Approval Issue"
    def topic = null
    def projects = getRelatedIssues(approvalIssue, ["Project"])
    if (!projects.isEmpty()) {
        pl = projects[0].getCustomFieldValue(cFieldPl)
        topic = projects[0].getSummary()
    }
    
    def issueTypeName = null
    if(approvalIssue.getSummary().contains("MS1")){
        issueTypeName = "MS1 Approval"
    }
    else if(approvalIssue.getSummary().contains(">=MS3")){
        issueTypeName = ">=MS3 Approval"
    }
    else if(approvalIssue.getSummary().contains("Change")){
        issueTypeName = "Change Request"
    }

    agendaTable += "|${pl}|${topic}|${issueTypeName}|${String.join(", ", usernames) ?: " "}|${String.join(", ", usernames2) ?: " "}| |${duration}| | |\n"
}

issue.setCustomFieldValue(cFieldAgenda, agendaTable)
def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

ComponentAccessor.getIssueManager().updateIssue(user, issue, EventDispatchOption.ISSUE_UPDATED, false); // false = do not send e-mail notifications

def getRelatedIssues(issueToCheck, issueTypes) {
    def result = []
    List<IssueLink> allOutIssueLink = ComponentAccessor.getIssueLinkManager().getOutwardLinks(issueToCheck.getId());
     for (Iterator<IssueLink> outIterator = allOutIssueLink.iterator(); outIterator.hasNext();) {
     IssueLink issueLink = (IssueLink) outIterator.next();
     ;
         log.warn('Linktype: ' + issueLink.getIssueLinkType().getName())
         if (issueLink.getIssueLinkType().getName() == 'Relates') {
            def linkedIssue = issueLink.getDestinationObject()
             log.warn('Issue: ' + linkedIssue.getKey())
            String type = linkedIssue.getIssueType().getName();
              log.warn('Issuetype :' + type)
             if (type in issueTypes) {
                 result.add(linkedIssue)
             }
         }
     }
        List<IssueLink> allInIssueLink = ComponentAccessor.getIssueLinkManager().getInwardLinks(issueToCheck.getId());
     for (Iterator<IssueLink> inIterator = allInIssueLink.iterator(); inIterator.hasNext();) {
     IssueLink issueLink = (IssueLink) inIterator.next();
     ;
         log.warn('Linktype: ' + issueLink.getIssueLinkType().getName())
         if (issueLink.getIssueLinkType().getName() == 'Relates') {
            def linkedIssue = issueLink.getSourceObject()
             log.warn('Issue: ' + linkedIssue.getKey())
            String type = linkedIssue.getIssueType().getName();
              log.warn('Issuetype :' + type)
             if (type in issueTypes) {
                 result.add(linkedIssue)
             }
         }
     }
    return result;
}
