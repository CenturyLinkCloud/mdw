Feature: MDWTestSuppsEmbedded
  Workflow process for testing event wait notification after process started.

  Scenario: Event Wait Finish After Start
    Given process input values:
      | testCase  | correct-waiting-on-wait  |
    When the "MDWTestSuppsEmbedded" workflow is invoked
    And I wait 10 seconds
    And I send "REST" message "<Signal>${masterRequestId}</Signal>"
    And I wait 30 seconds  
    Then the results should match "MDWTestSuppsEmbedded"
	
