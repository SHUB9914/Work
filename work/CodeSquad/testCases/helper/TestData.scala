package helper

import model.Collaborator.Collaborator
import model.DashboardReports.DashboardDetails
import util.Project

object TestData {

  val PROJECT_NAME = "test"
  val PROJECT_NAME2 = "test2"
  val KNOLDUS_PROJECT_NAME = "knoldus"
  val USERNAME = "testUser"
  val TEST_USERNAME = USERNAME
  val PASSWORD = "test123"
  val TRUE = "true"
  val FALSE = "false"
  val LINK_ID = "123"

  val TEST_PROJECT = Project(PROJECT_NAME, "warning", "scapegoatWarning", "scapegoatError", "scapegoatInfo", "sCoverage", "cpd", "loc", "date", "diff", "color",
    "scapegoatHtmlReport", "scalastyleXmlReport")

  val DASHBOARD_DETAILS = DashboardDetails(PROJECT_NAME, "moduleName", "scalaStyleWarning", "scapegoatWarning", "scapegoatInfo", "scapegoatError", "scoverage", "cpd",
    "loc", "modifiedDate")

  val COLLABORATOR = Collaborator(USERNAME, PROJECT_NAME, "N")

  val NO_OF_COLLABORATORS_FOR_FREE_ACC = 5

  val NO_OF_PROJECTS_FOR_FREE_USER = 2

}
