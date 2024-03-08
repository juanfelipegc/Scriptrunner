import com.atlassian.jira.issue.index.IssueIndexingService
import com.atlassian.jira.issue.Issue
import com.onresolve.scriptrunner.db.DatabaseUtil
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.bc.user.search.UserSearchService
import java.sql.Timestamp
import java.text.SimpleDateFormat
import com.atlassian.jira.issue.security.IssueSecuritySchemeManager

//Variables and context
def issue = ComponentAccessor.getIssueManager().getIssueObject(event.getIssue().getId())
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def options = null
def userSearchService = ComponentAccessor.getComponent(UserSearchService.class)
def projectNameField = customFieldManager.getCustomFieldObject (16807)
def cField = customFieldManager.getCustomFieldObject (18600)
def cFieldPLNew = customFieldManager.getCustomFieldObject(20012)
def cFieldBl = customFieldManager.getCustomFieldObject(18505)
def cFieldSegmentNew = customFieldManager.getCustomFieldObject(20013)
def cFieldPrjTypeNew = customFieldManager.getCustomFieldObject(20014)
def cFieldPrjCode = customFieldManager.getCustomFieldObject(18512)
def cFieldPrjCategoryNew = customFieldManager.getCustomFieldObject(20015)
def cFieldDivision = customFieldManager.getCustomFieldObject(18601)
def cFieldPrjStatus = customFieldManager.getCustomFieldObject(18602)
def cFieldPrjApprovalStatus = customFieldManager.getCustomFieldObject(18603)
def cFieldPrjClass = customFieldManager.getCustomFieldObject(18606)
def cFieldLastMilestone = customFieldManager.getCustomFieldObject(18607)
def cFieldNextMilestone = customFieldManager.getCustomFieldObject(18608)
def cFieldFolderProject = customFieldManager.getCustomFieldObject(18609)
def cFieldFolderProjectName = customFieldManager.getCustomFieldObject(19200)
def cFieldPjo = customFieldManager.getCustomFieldObject(18500)
def cFieldPjm = customFieldManager.getCustomFieldObject(18501)
def cFieldProgramNew = customFieldManager.getCustomFieldObject(20200)

def projectName = projectNameField.getValue(issue)
log.warn("Project name at the beggining "+projectName.toString())
def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
def sendEmailWithChange = false

//Methods
def getUserByMail(UserSearchService userSearchService, String email) {
    def users = userSearchService.findUsersByEmail(email)
    for (user in users) {
        return user
    }
    return null;
}


def formatDate(Timestamp timestamp) {
    if (!timestamp) {
        return "No Date"
    }
    def formatter = new SimpleDateFormat("yyyy-MM-dd")
    return formatter.format(timestamp)
}

//For getting field values from the DB
if(issue.getIssueType().getName() == "Project"){

    //set Assignee
    issue.setAssignee(issue.getReporter())
//get Data via PPS_Datamart DB-View, which is configured via ScriptRunner Resources
    def klusaProjectCfVal = issue.getCustomFieldValue(cField)
    if (klusaProjectCfVal != null) {
        def klusaProject = klusaProjectCfVal.toString()
        def projectDetails = DatabaseUtil.withSql('PPS_Datamart') { sql ->
            sql.firstRow('select * from dbo.vwJIRA_PRJ where DwhPrjId = ?', klusaProject)
        }

        if (projectDetails == null) {
            log.warn("Klusa project with DWH Id not found: " + klusaProject)
            
            return;
        }
        issue.setCustomFieldValue(cFieldBl, projectDetails.BL)
        issue.setCustomFieldValue(cFieldPrjCode, projectDetails."Prj Code")
        issue.setCustomFieldValue(cFieldDivision, projectDetails.DIV)
        issue.setCustomFieldValue(cFieldPrjStatus, projectDetails."Prj Status")
        issue.setCustomFieldValue(cFieldPrjApprovalStatus, projectDetails.AprSts)
        issue.setCustomFieldValue(cFieldPrjClass, projectDetails."Prj Class")
        issue.setCustomFieldValue(cFieldLastMilestone, String.format("%s (%s)", projectDetails."Last Rel. Milestone", formatDate(projectDetails."Last Re. Date")))
        issue.setCustomFieldValue(cFieldNextMilestone, String.format("%s (%s)", projectDetails."Next Plan Milestone", formatDate(projectDetails."Next Plan Date")))
        issue.setCustomFieldValue(cFieldFolderProject, projectDetails."Folder Project Code")
        issue.setCustomFieldValue(cFieldFolderProjectName, projectDetails."Folder Project Name")
        issue.setCustomFieldValue(cFieldPjo, getUserByMail(userSearchService, projectDetails."PJO EMail"))
        issue.setCustomFieldValue(cFieldPjm, getUserByMail(userSearchService, projectDetails."PJM EMail"))
        issue.setSummary(projectDetails."Prj Name")
        issue.setCustomFieldValue(projectNameField, projectDetails."Prj Name")
        setIssueSecurity(projectDetails.PL, issue)

        //PL Mapping
        options = ComponentAccessor.getOptionsManager().getOptions(cFieldPLNew.getRelevantConfig(issue))
        
        if(projectDetails.PL == "32"){
            issue.setCustomFieldValue(cFieldPLNew,options.get(0))
        }
        else if(projectDetails.PL == "38"){
            issue.setCustomFieldValue(cFieldPLNew,options.get(1))
        }
        else if(projectDetails.PL == "55"){
            issue.setCustomFieldValue(cFieldPLNew,options.get(2))
        }
        log.warn("PL VALUE: "+ issue.getCustomFieldValue(cFieldPLNew))

        //Program Mapping
        options = ComponentAccessor.getOptionsManager().getOptions(cFieldProgramNew.getRelevantConfig(issue))
        if(projectDetails.Program == "3DI"){
            issue.setCustomFieldValue(cFieldProgramNew, options.get(0))
        }
        else if(projectDetails.Program == "Antenna Devices"){
            issue.setCustomFieldValue(cFieldProgramNew, options.get(1))
        }
        else if(projectDetails.Program == "Customer Owned Technology"){
            issue.setCustomFieldValue(cFieldProgramNew, options.get(2))
        }
        else if(projectDetails.Program == "Enviromental Sensing"){
            issue.setCustomFieldValue(cFieldProgramNew, options.get(3))
        }
        else if(projectDetails.Program == "Hardware"){
            issue.setCustomFieldValue(cFieldProgramNew, options.get(4))
        }
        else if(projectDetails.Program == "HiRel"){
            issue.setCustomFieldValue(cFieldProgramNew, options.get(5))
        }
        else if(projectDetails.Program == "ISS"){
            issue.setCustomFieldValue(cFieldProgramNew, options.get(6))
        }
        else if(projectDetails.Program == "MICROPHONE"){
            issue.setCustomFieldValue(cFieldProgramNew, options.get(7))
        }
        else if(projectDetails.Program == "OPTO"){
            issue.setCustomFieldValue(cFieldProgramNew, options.get(8))
        }
        else if(projectDetails.Program == "PRESSURE SENSOR"){
            issue.setCustomFieldValue(cFieldProgramNew, options.get(9))
        }
        else if(projectDetails.Program == "Protection"){
            issue.setCustomFieldValue(cFieldProgramNew, options.get(10))
        }
        else if(projectDetails.Program == "RF Communications"){
            issue.setCustomFieldValue(cFieldProgramNew, options.get(11))
        }
        else if(projectDetails.Program == "Software"){
            issue.setCustomFieldValue(cFieldProgramNew, options.get(12))
        }
        else if(projectDetails.Program == "UE Sub 6GHz"){
            issue.setCustomFieldValue(cFieldProgramNew, options.get(13))
        }
        else if(projectDetails.Program == "Wireless Infrastructure"){
            issue.setCustomFieldValue(cFieldProgramNew, options.get(14))
        }

        //Segment Mapping
        options = ComponentAccessor.getOptionsManager().getOptions(cFieldSegmentNew.getRelevantConfig(issue))

        if(projectDetails.Segment == "3DI"){
            issue.setCustomFieldValue(cFieldSegmentNew, options.get(0))
        }
        else if(projectDetails.Segment == "HiRel"){
            issue.setCustomFieldValue(cFieldSegmentNew, options.get(1))
        }
        else if(projectDetails.Segment == "ISS"){
            issue.setCustomFieldValue(cFieldSegmentNew, options.get(2))
        }
        else if(projectDetails.Segment == "Radar Sensing"){
            issue.setCustomFieldValue(cFieldSegmentNew, options.get(3))
        }
        else if(projectDetails.Segment == "RF Communications"){
            issue.setCustomFieldValue(cFieldSegmentNew, options.get(4))
        }
        else if(projectDetails.Segment == "RF Discretes"){
            issue.setCustomFieldValue(cFieldSegmentNew, options.get(5))
        }
        else if(projectDetails.Segment == "Sensors"){
            issue.setCustomFieldValue(cFieldSegmentNew, options.get(6))
        }

        //Project Type Mapping
        options = ComponentAccessor.getOptionsManager().getOptions(cFieldPrjTypeNew.getRelevantConfig(issue))

        if(projectDetails."Prj Type" == "Design System"){
            issue.setCustomFieldValue(cFieldPrjTypeNew, options.get(0))
        }
        else if(projectDetails."Prj Type" == "Discrete Development"){
            issue.setCustomFieldValue(cFieldPrjTypeNew, options.get(1))
        }
        else if(projectDetails."Prj Type" == "Discrete Transfer"){
            issue.setCustomFieldValue(cFieldPrjTypeNew, options.get(2))
        }
        else if(projectDetails."Prj Type" == "Firmware Development"){
            issue.setCustomFieldValue(cFieldPrjTypeNew, options.get(3))
        }
        else if(projectDetails."Prj Type" == "Funding"){
            issue.setCustomFieldValue(cFieldPrjTypeNew, options.get(4))
        }
        else if(projectDetails."Prj Type" == "Innovation/Studies"){
            issue.setCustomFieldValue(cFieldPrjTypeNew, options.get(5))
        }
        else if(projectDetails."Prj Type" == "IP Component Development"){
            issue.setCustomFieldValue(cFieldPrjTypeNew, options.get(6))
        }
        else if(projectDetails."Prj Type" == "Multi Chip Development"){
            issue.setCustomFieldValue(cFieldPrjTypeNew, options.get(7))
        }
        else if(projectDetails."Prj Type" == "Multi Chip Development_PR"){
            issue.setCustomFieldValue(cFieldPrjTypeNew, options.get(8))
        }
        else if(projectDetails."Prj Type" == "Search field"){
            issue.setCustomFieldValue(cFieldPrjTypeNew, options.get(9))
        }
        else if(projectDetails."Prj Type" == "Single Chip Development"){
            issue.setCustomFieldValue(cFieldPrjTypeNew, options.get(10))
        }
        else if(projectDetails."Prj Type" == "Single Chip Development_PR"){
            issue.setCustomFieldValue(cFieldPrjTypeNew, options.get(11))
        }        
        else if(projectDetails."Prj Type" == "Software Agile Development"){
            issue.setCustomFieldValue(cFieldPrjTypeNew, options.get(12))
        }
        else if(projectDetails."Prj Type" == "Software Development"){
            issue.setCustomFieldValue(cFieldPrjTypeNew, options.get(13))
        }
        else if(projectDetails."Prj Type" == "System Development"){
            issue.setCustomFieldValue(cFieldPrjTypeNew, options.get(14))
        }
        else if(projectDetails."Prj Type" == "Wafer Technology Development"){
            issue.setCustomFieldValue(cFieldPrjTypeNew, options.get(15))
        }
        else if(projectDetails."Prj Type" == "Wafer Technology Transfer"){
            issue.setCustomFieldValue(cFieldPrjTypeNew, options.get(16))
        }

        //Project Category Mapping
        options = ComponentAccessor.getOptionsManager().getOptions(cFieldPrjCategoryNew.getRelevantConfig(issue))

        if(projectDetails."Prj Category" == "Autobahn"){
            issue.setCustomFieldValue(cFieldPrjCategoryNew, options.get(0))
        }
        else if(projectDetails."Prj Category" == "Expedition"){
            issue.setCustomFieldValue(cFieldPrjCategoryNew, options.get(1))
        }
        else if(projectDetails."Prj Category" == "Mission"){
            issue.setCustomFieldValue(cFieldPrjCategoryNew, options.get(2))
        }
        
    }
    else {
        issue.setSummary(projectName)
    }

}
log.warn("Project name at the end "+projectName.toString())

//Method for setting the sec level
    def setIssueSecurity(pl, Issue passedIssue) {
        def issueSecuritySchemeManager = ComponentAccessor.getComponent(IssueSecuritySchemeManager.class)
        def issueSecurityManager = ComponentAccessor.getIssueSecurityLevelManager()
        
        def levels = issueSecurityManager.getIssueSecurityLevels(issueSecuritySchemeManager.getSchemeIdFor(issue.getProjectObject()))
        
        for (level in levels) {
            if (level.getName() == pl) {
                issue.setSecurityLevelId(level.getId())
                return
            }
        }
        log.warn("No security level available for PL " + pl)
        
    }

def indexingService = ComponentAccessor.getComponent(IssueIndexingService.class)
ComponentAccessor.getIssueManager().updateIssue(user, issue, EventDispatchOption.DO_NOT_DISPATCH, false )
indexingService.reIndex(issue)
