Feature: MDWTestMainProcess
  Workflow process for acknowledging and processing orders.

  Scenario: Call Single Subprocess
    Given process input values:
      | processCase  | SingleSubProcess     |
      | N 			 | 7 					|
    When the "com.centurylink.mdw.tests.workflow/MDWTestMainProcess" workflow is invoked
    And I wait 180 seconds  
    Then the results should match "MDWTestMainProcess,MDWTestSubProcess"
