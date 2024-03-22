//Imports
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.util.ImportUtils
import com.onresolve.scriptrunner.runner.util.UserMessageUtil
import com.atlassian.jira.issue.index.IssueIndexingService
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.issue.ModifiedValue
import org.h2.expression.Variable
import java.lang.Double
import com.onresolve.scriptrunner.canned.jira.utils.CannedScriptUtils
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.issue.customfields.option.Option
import com.atlassian.jira.project.ProjectManager
import java.util.regex.*

//Context
def issueManager = ComponentAccessor.getIssueManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueLinkManager = ComponentAccessor.getIssueLinkManager()
def projectManager = ComponentAccessor.getComponent(ProjectManager)
def commentManager = ComponentAccessor.getCommentManager()
def issue = issueManager.getIssueObject(event.issue.id)
def user = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser()
def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def searchService = ComponentAccessor.getComponent(SearchService)
IssueService issueService = ComponentAccessor.getIssueService()

//Custom fields
def assessCf = customFieldManager.getCustomFieldObject(14607)
def pSeverityCf = customFieldManager.getCustomFieldObject(14800)
def pProbabilityCf = customFieldManager.getCustomFieldObject(14801)
def financialImpactCf = customFieldManager.getCustomFieldObject(14608)
def topicCf = customFieldManager.getCustomFieldObject(14606)
def riskValueCf = customFieldManager.getCustomFieldObject(14612)
def riskLevelCf = customFieldManager.getCustomFieldObject(14611)
def waivedCf = customFieldManager.getCustomFieldObject(14615)
def othersCf = customFieldManager.getCustomFieldObject(14616)
def discRequiredCf = customFieldManager.getCustomFieldObject(14620)
def responseStrategyCf = customFieldManager.getCustomFieldObject(14614)
def qualitativeRatingCf = customFieldManager.getCustomFieldObject(14613)
def pTargetSevCf = customFieldManager.getCustomFieldObject(14802)
def pTargetProbCf = customFieldManager.getCustomFieldObject(14803)
def targetRiskLevelCf = customFieldManager.getCustomFieldObject(14619)
def potentialEffectCf = customFieldManager.getCustomFieldObject(14604)
def rootCauseCf = customFieldManager.getCustomFieldObject(14605)
def destinationProjectCf = customFieldManager.getCustomFieldObject(14602)
def transferredAssessmentCf = customFieldManager.getCustomFieldObject(14622)
def transferredSeverityCf = customFieldManager.getCustomFieldObject(14623)
def transferredProbabilityCf = customFieldManager.getCustomFieldObject(14624)
def detachedRiskCf = customFieldManager.getCustomFieldObject(14625)

// Custom fields from Assessment Issue Type
def riskSeverityCf = customFieldManager.getCustomFieldObject(14626)
def profitImpactCf = customFieldManager.getCustomFieldObject(14627)

//Variables
boolean exists = false
boolean inwardCondition = issueLinkManager.getInwardLinks(issue.id)*.issueLinkType.name.contains('Reference')
def oLinks = issueLinkManager.getOutwardLinks(issue.id)
def project = issue.getProjectObject().name
def projectKey = issue.getProjectObject().key as String
def linkBetweenIssues = CannedScriptUtils.getAllLinkTypesWithInwards(true).find { it.value == "references to" }?.key?.toString()
def pProbability = issue.getCustomFieldValue(pProbabilityCf)
def pSeverity = issue.getCustomFieldValue(pSeverityCf)
def pTargetSev = issue.getCustomFieldValue(pTargetSevCf)
def pTargetProb = issue.getCustomFieldValue(pTargetProbCf)
def transitionResult = null
def actionId = 0
def opt = null
def option = null
def count = 0
def references = null
def options = null
double riskValue = 0
double riskLevel = 0
def tRiskLevel = null
def qualitativeRating = null
def riskSeverity = null
def probability = null
def tSeverity = null
def tProbability = null
def reference = null
def financialImpact = null

//Mappings
probability = issue.getCustomFieldValue(pProbabilityCf)
tProbability = issue.getCustomFieldValue(pTargetProbCf)
tSeverity = pTargetSev.toString()
riskSeverity = pSeverity


//This is an adapted code for the Assessment - Risk listener which calculates new values for Risk Level, Qualitative Rating, and Target Risk Level. Also updates th references.
if (issue.issueType.name == 'Risk') {

    //Updates custom field values from the Master Risk to the References
    //Checks if current issue is not a reference
    if(!inwardCondition){
        log.warn("IS MASTER RISK (CALCULATION)")
        //Iterates over the issue links
        for(o in oLinks){
            //Finds the related transparent references
            if(o.getLinkTypeId() == 10701){
                log.warn("THE REFERENCES ARE: "+ oLinks)
                reference = issueManager.getIssueObject(o.getDestinationId())
                //Sets the new value for the reference's field
                reference.setSummary(issue.getSummary() + " [Referenced Risk]")
                reference.setDescription(issue.getDescription())
                reference.setCustomFieldValue(potentialEffectCf,issue.getCustomFieldValue(potentialEffectCf))
                reference.setCustomFieldValue(rootCauseCf,issue.getCustomFieldValue(rootCauseCf))
                reference.setReporter(issue.getReporter())
                
                reference.setCustomFieldValue(responseStrategyCf,issue.getCustomFieldValue(responseStrategyCf))
                reference.setCustomFieldValue(pTargetSevCf,issue.getCustomFieldValue(pTargetSevCf))
                reference.setCustomFieldValue(pTargetProbCf,issue.getCustomFieldValue(pTargetProbCf))

                reference.setCustomFieldValue(targetRiskLevelCf,issue.getCustomFieldValue(targetRiskLevelCf))

                //OPTIONAL FIELDS
                //Transfer Assessment Type
                if(reference.getCustomFieldValue(transferredAssessmentCf) != null){
                    reference.setCustomFieldValue(assessCf,issue.getCustomFieldValue(assessCf))

                }
                //Transfer Severity
                if(reference.getCustomFieldValue(transferredSeverityCf) != null){
                    reference.setCustomFieldValue(pSeverityCf,issue.getCustomFieldValue(pSeverityCf))
                    reference.setCustomFieldValue(pTargetSevCf,issue.getCustomFieldValue(pTargetSevCf))
                    
                }
                //Transfer Probability
                if(reference.getCustomFieldValue(transferredProbabilityCf) != null){
                    reference.setCustomFieldValue(pProbabilityCf,issue.getCustomFieldValue(pProbabilityCf))
                    reference.setCustomFieldValue(pTargetProbCf,issue.getCustomFieldValue(pTargetProbCf))
                    
                }

                //UPDATES
                issueManager.updateIssue(user, reference, EventDispatchOption.ISSUE_UPDATED, false)
                reIndexIssue(reference)
            } 
        } 
    }
    //Checks if current issue is a reference
    else if (inwardCondition){

        //Risk Detachment
        //Checks if value for Detach Risk custom field is "Yes"
        if(detachedRiskCf.getValue(issue).toString() == "Yes"){
            log.warn("Deleting link to Master")
            def linkToMaster = issueLinkManager.getInwardLinks(issue.id).get(0)
            log.warn("Link: "+linkToMaster)
            issueLinkManager.removeIssueLink(linkToMaster,user)
            return
        }

        //Update values after re-attachment
        def master = issueLinkManager.getInwardLinks(issue.id).find{it.getLinkTypeId() == 10701}.getSourceObject()
        def mIssue = issueManager.getIssueObject(master.getId())
        //Assessment
        if(transferredAssessmentCf.getValue(issue) != null){
            issue.setCustomFieldValue(assessCf, master.getCustomFieldValue(assessCf))
        }
        //Severity
        if(transferredSeverityCf.getValue(issue) != null){
            log.warn("SEVERITY now: "+ issue.getCustomFieldValue(pSeverityCf))
            issue.setCustomFieldValue(pSeverityCf, master.getCustomFieldValue(pSeverityCf))
            log.warn("SEVERITY after sync with MASTER: "+ issue.getCustomFieldValue(pSeverityCf))
            issue.setCustomFieldValue(pTargetSevCf,master.getCustomFieldValue(pTargetSevCf))
            riskSeverity = pSeverityCf.getValue(master)
        }
        //Probability
        if(transferredProbabilityCf.getValue(issue) != null){
            issue.setCustomFieldValue(pProbabilityCf, master.getCustomFieldValue(pProbabilityCf))
            issue.setCustomFieldValue(pTargetProbCf,master.getCustomFieldValue(pTargetProbCf))
            probability = pProbabilityCf.getValue(master)
        }

        issue.setSummary(master.getSummary()+" [Referenced Risk]")
        log.warn("UPDATES ISSUE")
        issueManager.updateIssue(user, issue, EventDispatchOption.ISSUE_UPDATED, false); // false = do not send e-mail notifications 
        issueManager.updateIssue(user, mIssue, EventDispatchOption.ISSUE_UPDATED, false); // false = do not send e-mail notifications 
        reIndexIssue(issue)
    }

    if (riskSeverity == null || riskSeverity == 0) {
        log.warn("ENTRA NULL SEVERITY")
        //If a value used to calculate any of these fields is equal to "None" (null), the fields should update and not show the last non-empty value
        riskValueCf.updateValue(null, event.issue, new ModifiedValue(issue.getCustomFieldValue(riskValueCf), riskValue), new DefaultIssueChangeHolder())
        riskLevelCf.updateValue(null, event.issue, new ModifiedValue(issue.getCustomFieldValue(riskLevelCf), riskLevel), new DefaultIssueChangeHolder())
        qualitativeRatingCf.updateValue(null, event.issue, new ModifiedValue(issue.getCustomFieldValue(qualitativeRatingCf), qualitativeRating), new DefaultIssueChangeHolder())
        financialImpactCf.updateValue(null, event.issue, new ModifiedValue(issue.getCustomFieldValue(financialImpactCf), financialImpact), new DefaultIssueChangeHolder())
        //issueManager.updateIssue(user, issue, EventDispatchOption.ISSUE_UPDATED, false); // false = do not send e-mail notifications
        return
    }

    reIndexIssue(issue)
    //Uses a JQL to get the corresponding Assessment
    log.warn("SEVERITY FOR GETTING ASSESSMENT: "+ riskSeverity.toInteger())
    log.warn("PROJECT: "+ projectKey )
    def jqlSearch = "project = '" + projectKey + "'" + " and issuetype = 'Assessment' and 'Risk Severity' = '" + riskSeverity.toInteger() + "'"
    def query = jqlQueryParser.parseQuery(jqlSearch)
    def results = searchService.search(user, query, PagerFilter.getUnlimitedFilter())
    log.warn("ASSESSMENTS: "+ results.getTotal())
    if (results.getTotal() == 0) {
        UserMessageUtil.error('No Assessment Issues have been created yet')
        return
    }

    def assessment = issueManager.getIssueObject(results.getResults().get(0).key)
    def profitImpact = assessment.getCustomFieldValue(profitImpactCf)
    if (!profitImpact) {
        return
    }

    probability = issue.getCustomFieldValue(pProbabilityCf)
    if (!probability) {
        log.warn("ENTER NULL PROBABILITY")
        //If a value used to calculate any of these fields is equal to "None" (null), the fields should update and not show the last non-empty value
        riskValueCf.updateValue(null, event.issue, new ModifiedValue(issue.getCustomFieldValue(riskValueCf), riskValue), new DefaultIssueChangeHolder())
        riskLevelCf.updateValue(null, event.issue, new ModifiedValue(issue.getCustomFieldValue(riskLevelCf), riskLevel), new DefaultIssueChangeHolder())
        qualitativeRatingCf.updateValue(null, event.issue, new ModifiedValue(issue.getCustomFieldValue(qualitativeRatingCf), qualitativeRating), new DefaultIssueChangeHolder())
        financialImpactCf.updateValue(null, event.issue, new ModifiedValue(issue.getCustomFieldValue(financialImpactCf), financialImpact), new DefaultIssueChangeHolder())
        //issueManager.updateIssue(user, issue, EventDispatchOption.ISSUE_UPDATED, false); // false = do not send e-mail notifications
        return
    }
    
    //When the values are not empty, it is possible to calculate and set the fields
    if(probability && riskSeverity && profitImpact){
        log.warn("ENTERS CALCULATIONS")
        riskValue = (probability.toString() as Double) * (profitImpact.toString() as Double)
        riskLevel = (probability.toString() as Double) * (riskSeverity.toString() as Double)
        //riskValueCf.updateValue(null, event.issue, new ModifiedValue(issue.getCustomFieldValue(riskValueCf), riskValue), new DefaultIssueChangeHolder())
        //riskLevelCf.updateValue(null, event.issue, new ModifiedValue(issue.getCustomFieldValue(riskLevelCf), riskLevel), new DefaultIssueChangeHolder())
        log.warn('Trying to update issue')
        issue.setCustomFieldValue(riskLevelCf, riskLevel)
        issue.setCustomFieldValue(riskValueCf, riskValue)
        issueManager.updateIssue(user, issue, EventDispatchOption.ISSUE_UPDATED, false)
        financialImpactCf.updateValue(null, event.issue, new ModifiedValue(issue.getCustomFieldValue(financialImpactCf), profitImpact.toString()), new DefaultIssueChangeHolder())

        //Even the ones that depend on original calculated values (E.g Qualitative Rating depends on Risk Level)
        if(riskLevel >= 50){
            qualitativeRating = "High"
            qualitativeRatingCf.updateValue(null, event.issue, new ModifiedValue(issue.getCustomFieldValue(qualitativeRatingCf), qualitativeRating), new DefaultIssueChangeHolder())

        }
        else if(riskLevel <= 25){
            qualitativeRating = "Low"
            qualitativeRatingCf.updateValue(null, event.issue, new ModifiedValue(issue.getCustomFieldValue(qualitativeRatingCf), qualitativeRating), new DefaultIssueChangeHolder())

        }
        else{
            qualitativeRating = "Medium"
            qualitativeRatingCf.updateValue(null, event.issue, new ModifiedValue(issue.getCustomFieldValue(qualitativeRatingCf), qualitativeRating), new DefaultIssueChangeHolder())

        }
    }

    //The calculation logic is the same as before
    if (tSeverity == null) {
        targetRiskLevelCf.updateValue(null, event.issue, new ModifiedValue(issue.getCustomFieldValue(targetRiskLevelCf), tRiskLevel), new DefaultIssueChangeHolder())
        return
    }
    if (!tProbability) {
        targetRiskLevelCf.updateValue(null, event.issue, new ModifiedValue(issue.getCustomFieldValue(targetRiskLevelCf), tRiskLevel), new DefaultIssueChangeHolder())
        return
    }
    
    tRiskLevel = (tProbability.toString() as Double) * (tSeverity.toString() as Double)
    issue.setCustomFieldValue(targetRiskLevelCf, tRiskLevel)
    issueManager.updateIssue(user, issue, EventDispatchOption.ISSUE_UPDATED, false)
    //targetRiskLevelCf.updateValue(null, event.issue, new ModifiedValue(issue.getCustomFieldValue(targetRiskLevelCf), tRiskLevel), new DefaultIssueChangeHolder())

    if(tRiskLevel > riskLevel){
        UserMessageUtil.error('TARGET RISK LEVEL VALUE IS HIGHER THAN RISK LEVEL')
        log.warn('TARGET RISK LEVEL VALUE IS HIGHER THAN RISK LEVEL')
    }

}

else if (issue.issueType.name == 'Assessment') {
    
    if(issue.getCustomFieldValue(riskSeverityCf) == null){
    riskSeverity == "0"
    }
    else{
    riskSeverity = issue.getCustomFieldValue(riskSeverityCf)

    //Mapping is done inside the Assessment Issue Type as riskSeverity uses the value on "Risk Severity" custom field
    pSeverity = riskSeverity.getValue()
    }
    
    if (riskSeverity == null) {
        
        //If a value used to calculate any of these fields is equal to "None" (null), the fields should update and not show the last non-empty value
        riskValueCf.updateValue(null, event.issue, new ModifiedValue(issue.getCustomFieldValue(riskValueCf), riskValue), new DefaultIssueChangeHolder())
        return
    }

    //Uses a JQL to get the corresponding Risk
    def riskjqlSearch = "project = '" + projectKey + "'" + " and issuetype = 'Risk' and 'Severity' = '" + pSeverity.toString() + "'"
    def query = jqlQueryParser.parseQuery(riskjqlSearch)
    def results = searchService.search(user, query, PagerFilter.getUnlimitedFilter())
    if (results.getTotal() == 0) {
        log.warn("THERE ARE NO ISSUES WITH SEVERITY: " + pSeverity)
        //UserMessageUtil.error('THERE ARE NO ISSUES YET CREATED IN THIS PROJECT WITH THAT SEVERITY VALUE')
        return
    }
    
    def profitImpact = issue.getCustomFieldValue(profitImpactCf)
    if (!profitImpact) {
        //If a value used to calculate any of these fields is equal to "None" (null), the fields should update and not show the last non-empty value
        riskValueCf.updateValue(null, event.issue, new ModifiedValue(issue.getCustomFieldValue(riskValueCf), riskValue), new DefaultIssueChangeHolder())
        return
    }
    results.getResults().each{document -> 
        def issueKey = document.getKey()
        def riskIssue = issueManager.getIssueObject(issueKey)
        pProbability = riskIssue.getCustomFieldValue(pProbabilityCf)

        //Mapping added to check "Probability" custom field value on Risk Issue Type
        probability = pProbability

        if (!probability) {
            //If a value used to calculate any of these fields is equal to "None" (null), the fields should update and not show the last non-empty value
            riskValueCf.updateValue(null, document, new ModifiedValue(issue.getCustomFieldValue(riskValueCf), riskValue), new DefaultIssueChangeHolder())
            return
        }

        riskValue = (probability.toString() as Double) * (profitImpact.toString() as Double)
        riskValueCf.updateValue(null, document, new ModifiedValue(issue.getCustomFieldValue(riskValueCf), riskValue), new DefaultIssueChangeHolder())
        riskLevel = (probability.toString() as Double) * (pSeverity.toString() as Double)
        //riskLevelCf.updateValue(null, document, new ModifiedValue(issue.getCustomFieldValue(riskLevelCf), riskLevel), new DefaultIssueChangeHolder())
        issue.setCustomFieldValue(riskLevelCf, riskLevel)
        issueManager.updateIssue(user, issue, EventDispatchOption.ISSUE_UPDATED, false)  
        financialImpactCf.updateValue(null, document, new ModifiedValue(issue.getCustomFieldValue(financialImpactCf), profitImpact.toString()), new DefaultIssueChangeHolder())
    }
    reIndexIssue(issue)
}

def reIndexIssue(MutableIssue issue){
    //Re-index the issue after update
    boolean wasIndexing = ImportUtils.isIndexIssues()
    ImportUtils.setIndexIssues(true)
    ComponentAccessor.getComponent(IssueIndexingService.class).reIndex(issue)
    ImportUtils.setIndexIssues(wasIndexing)
}
