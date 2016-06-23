// excel download javascript
  
  function downloadExcelExport(url, listId, user, criteria, sort, ascending)
  {
    var excelDownloadForm = document.createElement("form");
    excelDownloadForm.setAttribute("id", "excelDownloadForm");
    excelDownloadForm.setAttribute("name", "excelDownloadForm");
    excelDownloadForm.setAttribute("style", "padding:0px; margin:0px");
    document.body.appendChild(excelDownloadForm);

    var excelExportDownloadExpr = document.createElement("input");
    excelExportDownloadExpr.setAttribute("type", "hidden");
    excelExportDownloadExpr.setAttribute("id", "excelExportDownloadExpr");
    excelExportDownloadExpr.setAttribute("name", "excelExportDownloadExpr");
    excelExportDownloadExpr.setAttribute("value", "document.forms[formName].action = url; document.forms[formName].submit();");
    document.getElementById("excelDownloadForm").appendChild(excelExportDownloadExpr);

    var listIdInput = document.createElement("input");
    listIdInput.setAttribute("type", "hidden");
    listIdInput.setAttribute("id", "excelExportListId");
    listIdInput.setAttribute("name", "excelExportListId");
    listIdInput.setAttribute("value", listId);
    document.getElementById("excelDownloadForm").appendChild(listIdInput);
  
    var userInput = document.createElement("input");
    userInput.setAttribute("type", "hidden");
    userInput.setAttribute("id", "user");
    userInput.setAttribute("name", "user");
    userInput.setAttribute("value", user);
    document.getElementById("excelDownloadForm").appendChild(userInput);

    var criteriaInput = document.createElement("input");
    criteriaInput.setAttribute("type", "hidden");
    criteriaInput.setAttribute("id", "filterCriteria");
    criteriaInput.setAttribute("name", "filterCriteria");
    criteriaInput.setAttribute("value", criteria);
    document.getElementById("excelDownloadForm").appendChild(criteriaInput);

    var sortInput = document.createElement("input");
    sortInput.setAttribute("type", "hidden");
    sortInput.setAttribute("id", "sort");
    sortInput.setAttribute("name", "sort");
    sortInput.setAttribute("value", sort);
    document.getElementById("excelDownloadForm").appendChild(sortInput);

    var ascendingInput = document.createElement("input");
    ascendingInput.setAttribute("type", "hidden");
    ascendingInput.setAttribute("id", "ascending");
    ascendingInput.setAttribute("name", "ascending");
    ascendingInput.setAttribute("value", ascending);
    document.getElementById("excelDownloadForm").appendChild(ascendingInput);

    // obfuscation to prevent portal URL rewriting
    formName = "excelDownloadForm";
    expression = document.getElementById('excelExportDownloadExpr');
    eval(expression.value);
  }
