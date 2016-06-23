Feature: rest web service listener
  Send Rest message using a XML file

  Scenario: Read XML file and send a REST message
    When I send "${asset('GetEmployeeRequest.xml').text}"
    Then the response should match "${asset('GetEmployeeResponse.xml').text}"