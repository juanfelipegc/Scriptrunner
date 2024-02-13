//Imports
import static com.atlassian.jira.issue.IssueFieldConstants.COMPONENTS
import java.util.regex.*
import com.atlassian.jira.issue.customfields.option.Option
import com.atlassian.jira.bc.project.component.ProjectComponent
import com.atlassian.jira.bc.project.component.ProjectComponentManager
import com.atlassian.jira.component.ComponentAccessor

//Context variables
def optionsManager = ComponentAccessor.getOptionsManager()
def projectComponentManager = ComponentAccessor.getComponent(ProjectComponentManager)
long projectId = getIssueContext().getProjectObject().getId()
def field = getFieldById(COMPONENTS)


//Data structures
Collection<ProjectComponent> projectComponents = projectComponentManager.findAllForProject(projectId)
List<String> filteredComponents = []
List<String> components = []
log.warn(projectComponents)

//This function filters the input to check if the input contains the regular expression and if it does returns the same input (should work as a filter)
def useRegex(String input) {
    if (input != null) {
    // Compile regular expression
    final Pattern pattern = Pattern.compile(/Risk Topic:\\*/, Pattern.CASE_INSENSITIVE);
    // Match regex against input
    final Matcher matcher = pattern.matcher(input);
    // Return results that match the regex...
    return matcher.find()
    }
}

//Creates a list of component's names
for(p in projectComponents){
    components.add(p.getName())
    log.warn("All descriptions" + p.getDescription())
}


//Iterates over the current prject's components and uses the RegEx function to filter them and place them as the new component list
if (getIssueContext().getIssueType().getName() == "Risk") {

    for (a in projectComponents){
        if(useRegex(a.getName())){
            filteredComponents.add(a.getName())
        }
    }
    field.setFieldOptions(projectComponents.findAll {useRegex(it.getDescription())})

} else {
    field.setFieldOptions(projectComponents)
}
