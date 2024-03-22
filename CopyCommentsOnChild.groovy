import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.comments.Comment
import com.atlassian.jira.issue.comments.CommentManager
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.issue.link.IssueLink
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue;

//Context and Variables

def linkManager = ComponentAccessor.getIssueLinkManager()
def commentManager = ComponentAccessor.getCommentManager()
def issue = event.issue as MutableIssue

def lastComment = event.getComment()
def commentBody = lastComment.getBody()
def author = lastComment.getAuthorApplicationUser()


def reference = null
def comments = commentManager.getComments(issue)
def oLinks = linkManager.getOutwardLinks(issue.getId())

//Takes the last comment added to the master and replicates it on all of its references.
linkManager.getOutwardLinks(issue.getId()).each {issueLink ->;

    reference = issueLink.getDestinationObject()
    //Checks if link is a Reference
    if (issueLink.issueLinkType.name == "Reference") {
        //Checks if the comment exists and has a value
        if(lastComment){
            //Creates the new comment within the reference
            commentManager.create(reference, author, commentBody ,true)
        }
        
    }
}
