  // javascript for use by picklist component

  function shift(name, toPosition) 
  {
	var selectedItemsList = findElement(name + '_selectedItems');

	if (selectedItemsList.options.length <= 1)
		return ;

	if (toPosition == 'top' || toPosition == 'bottom') 
	{
		var selectedOptionsIndexs = new Array();
		var selectedOptionsIdx = 0;
		for ( var i = 0; i < selectedItemsList.options.length; i++) 
		{
			if (selectedItemsList.options[i].selected) 
				selectedOptionsIndexs[selectedOptionsIdx++] = i;
		}
		if (selectedOptionsIndexs.length > 0) 
			moveTopOrBottom(selectedItemsList, selectedOptionsIndexs, toPosition);
	}
	else
	{
		for ( var i = 0; i < selectedItemsList.options.length; i++) 
		{
			if (selectedItemsList.options[i].selected) 
			{
				if (toPosition == 'up' && i > 0) 
				{
					swapPositions(selectedItemsList, i, i - 1);
				}					
				else if (toPosition == 'down' && (i < selectedItemsList.options.length - 1))
				{
					swapPositions(selectedItemsList, i, i + 1);
				}					
			}
		}
	}
	updateHiddenValues(name);
	return false;
  }

  function swapPositions(selectedItemsList, currentPosition, swapPosition) 
  {
	var currentElement = selectedItemsList.options[currentPosition];
	var previousElementText = selectedItemsList.options[swapPosition].text;
	var previousElementValue = selectedItemsList.options[swapPosition].value;
	selectedItemsList.options[swapPosition].text = currentElement.text;
	selectedItemsList.options[swapPosition].value = currentElement.value;
	selectedItemsList.options[currentPosition].text = previousElementText;
	selectedItemsList.options[currentPosition].value = previousElementValue;
	selectedItemsList.options[currentPosition].selected = false;
  }

  function moveTopOrBottom(selectedItemsList, selectedOptionsIndexs, position) 
  {
	var selectedOptions = new Array(selectedItemsList.options.length);
	var arrayIndex = 0;
	for ( var index = 0; index < selectedOptionsIndexs.length; index++) 
	{
		if (position == 'bottom')
		{
			selectedOptions[selectedItemsList.options.length - (selectedOptionsIndexs.length-index)] = selectedItemsList.options[selectedOptionsIndexs[index]];
		}			
		else
		{
			selectedOptions[arrayIndex++] = selectedItemsList.options[selectedOptionsIndexs[index]];
		}
	}
	
	for ( var i = 0; i < selectedItemsList.options.length; i++) 
	{
		if (selectedOptionsIndexs.indexOf(i) < 0)
		selectedOptions[arrayIndex++] = selectedItemsList.options[i];
	}
		
	while (selectedItemsList.options.length > 0)
		selectedItemsList.remove(0);

	for ( var j = 0; j < selectedOptions.length; j++)
		addOption(selectedItemsList, selectedOptions[j]);
  }
  
  function moveRight()
  {
    move('availableItems', 'selectedItems');
    updateHidden();
  }
  
  function moveLeft()
  {
    move('selectedItems', 'availableItems');
    updateHidden();
  }
  
  function moveToRight(name)
  {
    move(name + '_availableItems', name + '_selectedItems');
    updateHiddenValues(name);
  }
  
  function moveToLeft(name)
  {
    move(name + '_selectedItems', name + '_availableItems');
    updateHiddenValues(name);
  }
  
  function updateHidden()
  {
    selectedSelect = findElement('selectedItems');
    
    if (selectedSelect)
    {
      hiddenInput = findElement('hiddenSelected');
      
      selectedValues = new Array(selectedSelect.options.length);
      for (var i = 0; i < selectedSelect.options.length; i++)
      {
        selectedValues[i] = selectedSelect.options[i].value;
      }
      
      hiddenInput.value = selectedValues.join();
      return true;
    }
    else
    {
      return false;
    }
  }
  
  function updateHiddenValues(name)
  {
    selectedSelect = findElement(name + '_selectedItems');
    
    if (selectedSelect)
    {
      hiddenInput = findElement(name + '_hiddenSelected');
      
      selectedValues = new Array(selectedSelect.options.length);
      for (var i = 0; i < selectedSelect.options.length; i++)
      {
        selectedValues[i] = selectedSelect.options[i].value;
      }
      
      hiddenInput.value = selectedValues.join();
      return true;
    }
    else
    {
      return false;
    }
  }
  
  function setOriginal()
  {
    if (selectedValues)
    {
      originalSelectedValues = selectedValues;
    }
  }
  
  function resetToOriginal()
  {
    selectedValues = originalSelectedValues;
    
    selectedSelect = findElement('selectedItems');
    availableSelect = findElement('availableItems');
    
    var allOptions = new Array(selectedSelect.options.length + availableSelect.options.length);
    for (var i = 0; i < selectedSelect.options.length; i++)
    {
      allOptions[i] = selectedSelect.options[i];
    }
    for (var i = selectedSelect.length; i < allOptions.length; i++)
    {
      allOptions[i] = availableSelect.options[i - selectedSelect.length];
    }
    
    // remove all
    while (selectedSelect.options.length > 0)
      selectedSelect.remove(0);
    while (availableSelect.options.length > 0)
      availableSelect.remove(0);
    
    for (var i = 0; i < selectedValues.length; i++)
    {
      for (var j = 0; j < allOptions.length; j++)
      {
        if (selectedValues[i] == allOptions[j].value)
          addOption(selectedSelect, allOptions[j]);
      }
    }
    
    for (var i = 0; i < allOptions.length; i++)
    {
      var sel = false;
      for (var j = 0; j < selectedValues.length; j++)
      {
        if (allOptions[i].value == selectedValues[j])
        {
          sel = true;
          break;
        }
      }
      if (!sel)
      {
        addOption(availableSelect, allOptions[i]);
      }
    }
    
    updateHidden();
  }
  
  function move(from, to)
  {
    fromSelect = findElement(from);
    toSelect = findElement(to);
    for (var i = 0; i < fromSelect.options.length; i++)
    {
      if (fromSelect.options[i].selected)
      {
        newOpt = document.createElement('option');
        newOpt.text = fromSelect.options[i].text;
        newOpt.value = fromSelect.options[i].value;
        try
        {
          toSelect.add(newOpt);
        }
        catch (ex)
        {
          toSelect.add(newOpt, null);
        }
        fromSelect.remove(i);
        i = i - 1;
      }
    }      
  }
  
  function addOption(select, option)
  {
    newOpt = document.createElement('option');
    newOpt.text = option.text;
    newOpt.value = option.value;
    try
    {
      select.add(newOpt);
    }
    catch (ex)
    {
      select.add(newOpt, null);
    }
  }

  function findElement(name)
  {
    for (var i = 0; i < document.forms.length; i++)
    {
      elements = document.forms[i].elements;    
      for (var j = 0; j < elements.length; j++)
      {
        if (endsWith(elements[j].name, name))
          return elements[j];
      }
    }
      
    return null;
  }
  
  function deselect(name)
  {
    sel = findElement(name);
    for (var i = 0; i < sel.options.length; i++)
    {
      sel.options[i].selected = false;
    }
  }
  
  function endsWith(str, suffix)
  {
    return str.indexOf(suffix, str.length - suffix.length) !== -1;
  }   
  
