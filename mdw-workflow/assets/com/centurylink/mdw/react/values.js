 // Initializes values for user-entry
const values = {
  toArray: function(valuesObj) {
    var vals = [];
    Object.keys(valuesObj).forEach(key => {
      var val = valuesObj[key];
      val.name = key;
      val.isDocument = val.type && $mdwUi.DOCUMENT_TYPES[val.type];
      if (val.isDocument) {
        val.showLines = this.showLines;
        if (val.value && val.value.lineCount) {
          var lineCount = val.value.lineCount();
          if (lineCount > this.maxLines)
            val.showLines = this.maxLines;
          else if (lineCount > this.showLines)
            val.showLines = lineCount;
        }
      }
      else if (val.type === 'java.util.Date' && val.value) {
        // TODO: option to specify date parse format
        val.value = new Date(val.value).toISOString();
      }
      if (val.value === null || typeof val.value === 'undefined')
        val.value = '';
      
      vals.push(val);
    });
    vals.sort((val1, val2) => {
      if (typeof val1.sequence === 'number') {
        if (typeof val2.sequence === 'number')
          return val1.sequence - val2.sequence;
        else
          return -1;
      }
      else if (typeof val2.sequence === 'number') {
        return 1;
      }
      else {
        var label1 = val1.label ? val1.label : val1.name;
        var label2 = val2.label ? val2.label : val2.name;
        return label1.toLowerCase().localeCompare(label2.toLowerCase());
      }
    });
    return vals;
  },
  // returns a new copy of the array with the newly-updated value
  update: function(valuesArr, event, newValue) {
    var value = valuesArr.find(val => {
      return val.name === event.currentTarget.name;
    });
    if (typeof newValue !== 'undefined') {
      value.value = newValue;
    }
    else if (event.currentTarget.type === 'checkbox') {
      value.value = '' + event.currentTarget.checked;
    }
    else {
      value.value = event.currentTarget.value;
    }
  }
};

export default values; 
