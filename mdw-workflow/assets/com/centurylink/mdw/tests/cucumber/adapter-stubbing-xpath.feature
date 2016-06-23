Feature: adapter-stubbing-xpath
  Echo stub response.

  Scenario: Stub
    Given process input values:
      | request  | ${file('GetEmployeeRequest.xml')}   |
    When the "com.centurylink.mdw.tests.stubbing/TestAdapterStub" workflow is invoked
    And I stub "GetEmployee" after delay of 5 seconds with a response "${file('GetEmployeeResponse.xml')}"
    And I await process completion  
    Then the results should match "TestAdapterStub"