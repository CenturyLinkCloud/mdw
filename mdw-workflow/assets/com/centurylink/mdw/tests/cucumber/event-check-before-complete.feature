Feature: event-check-before-complete-correct
  Event arrives before wait gets created.

  Scenario: Event arrives before wait gets created
    Given I notify event "S-${masterRequestId}" with message "<EventCheck/>"
    And I wait 10 seconds
    When the "EventCheck" workflow is invoked
    And I await process completion  
    Then the results should match "EventCheck"

