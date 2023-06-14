import com.atlassian.jira.security.roles.ProjectRoleActor
import com.atlassian.jira.util.SimpleErrorCollection
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.bc.projectroles.ProjectRoleService
import com.atlassian.jira.component.ComponentAccessor

//This script transfers the users from a project role to another one for a list of projects

//Context variables
def projectManager = ComponentAccessor.getProjectManager()

//List the projects inside quotation marks and separated by commas.
def projects = projectManager.getProjectsByArgs(["HXP","",""])

def projectRoleService = ComponentAccessor.getComponent(ProjectRoleService)
def projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)

def projectEditorRole = projectRoleManager.getProjectRole("Editor")
def projectPEPRole = projectRoleManager.getProjectRole("PEP Editor")

def errorCollection = new SimpleErrorCollection()

def editors = []
Collection<String> pepEditors = []

for(project in projects){
    //Get the current editors as a list of users
    editors = projectRoleService.getProjectRoleActors(projectEditorRole, project, errorCollection).getUsers()

    //Iterate over the list of editors, check if they're active, if yes, add them to a new list
    for(actor in editors){
        if(actor.active){
            pepEditors.add(actor.getKey().toString())
        }
    }
    log.warn(pepEditors)

    //Set the new list as actors to the "Pep Editor" role
    projectRoleService.addActorsToProjectRole(pepEditors,projectPEPRole,project,ProjectRoleActor.USER_ROLE_ACTOR_TYPE, errorCollection)

    log.warn(errorCollection.errorMessages)

    //Removes the users from the "Editor" role
    //projectRoleService.removeActorsFromProjectRole(pepEditors,projectEditorRole, project,ProjectRoleActor.USER_ROLE_ACTOR_TYPE,errorCollection)

}
