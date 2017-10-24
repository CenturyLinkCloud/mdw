import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import Heading from './Heading.jsx';
import Value from '../react/Value.jsx';

class Values extends Component {
    
  constructor(...args) {
    super(...args);
    this.showLines = 2;
    this.maxLines = 8;
    this.state = { values: [] };
    this.handleChange = this.handleChange.bind(this);
    this.handleSave = this.handleSave.bind(this);    
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
      this.initValues(vals);
    });
  }
  
  initValues(vals) {
    var values = [];
    Object.keys(vals).forEach(key => {
      var val = vals[key];
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
      
      values.push(val);
    });
    values.sort((val1, val2) => {
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
    this.setState({
      values: values
    });
  }
  
  handleChange(event, newValue) {
    var values = this.state.values.slice();
    var value = values.find(val => {
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
    this.setState({
      values: values
    });
  }
  
  handleSave() {
    var vals = {};
    this.state.values.forEach(value => {
      if (value.value !== '' && value.display !== 'ReadOnly') {
        vals[value.name] = value.value;
      }
    });
    // save
    var ok = false;
    fetch(new Request(this.context.serviceRoot + '/Tasks/' + this.props.task.id + '/values', {
      method: 'PUT',
      headers: { Accept: 'application/json'},
      body: JSON.stringify(vals)
    }))
    .then(response => {
      ok = response.ok;
      return response.json();
    })
    .then(json => {
      if (ok) {
        $mdwUi.showMessage('Values saved');
        setTimeout(() => {
          $mdwUi.clearMessage();
        }, 1500);
      }
      else {
        $mdwUi.showMessage(json.status.message);
      }
    });
  }
  
  render() {
    return (
      <div>
        <Heading task={this.props.task} refreshTask={this.props.refreshTask} />
        <div className="mdw-section">
          <form name="valuesForm" className="form-horizontal" role="form">
            {this.state.values.map(value => {
              return (
                <Value value={value} key={value.name} 
                  editable={this.props.task.editable} onChange={this.handleChange} />
              );
            })}
            {this.props.task.editable && this.state.values.length > 0 &&
              <div className="form-group">
                <label className="control-label col-xs-2" />
                <div className="col-xs-4">
                  <Button className="mdw-btn" bsStyle='primary' onClick={this.handleSave}>
                    <Glyphicon glyph="floppy-disk" />{' Save'}
                  </Button>
                </div>
              </div>
            }
          </form>
        </div>
      </div>
    );
  }
}

Values.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string  
};

export default Values;