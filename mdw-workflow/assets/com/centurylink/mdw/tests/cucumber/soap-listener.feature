Feature: Call a Service Process and verify the response
  Send SOAP message using a XML file

  Scenario: Read XML file and send a SOAP message
    When I send "SOAP" message "${asset('GetEmployeeSoap.xml').text}"
    Then the response should match "${asset('GetEmployeeResponseSoap.xml').text}"