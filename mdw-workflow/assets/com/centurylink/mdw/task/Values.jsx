import React, {Component} from '../node/node_modules/react';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import Value from '../react/Value.jsx';

class Values extends Component {
    
  constructor(...args) {
    super(...args);
    this.showLines = 2;
    this.maxLines = 8;
    
    this.state = { values: [] };
  }  

  componentDidMount() {
    fetch(new Request('/mdw/services/Tasks/' + this.props.task.id + '/values', {
      method: 'GET',
      headers: { Accept: 'application/json'}
    }))
    .then(response => {
      return response.json();
    })
    .then(vals => {
      var values = [];
      Object.keys(vals).forEach(key => {
        var val = vals[key];
        val.name = key
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
          val.value = new Date(val.value);
        }
        if (val.display && this.props.task.editable)
          val.editable = val.display !== 'ReadOnly';
        else
          val.editable = this.props.task.editable;
        
        values.push(val);
      });
      values.sort((val1, val2) => {
        var diff = val1.sequence - val2.sequence;
        if (diff === 0) {
          var label1 = val1.label ? val1.label : val1.name;
          var label2 = val2.label ? val2.label : val2.name;
          return label1.toLowerCase().localeCompare(label2.toLowerCase());
        }
        else {
          return diff;
        }
      });      
      this.setState({
        values: values
      });
    });    
  }
  
  render() {
    return (
      <form name="valuesForm" className="form-horizontal" role="form">
        {this.state.values.map(value => {
          return <Value value={value} key={value.name} />
        })}
        {this.props.task.editable && this.state.values.length > 0 &&
          <div className="form-group">
            <label className="control-label col-xs-2" />
            <div className="col-xs-4">
              <Button className="mdw-btn" bsStyle='primary'>
                <Glyphicon glyph="floppy-disk" />{' Save'}
              </Button>
            </div>
          </div>
        }
      </form>
    );
  }
}

export default Values;  