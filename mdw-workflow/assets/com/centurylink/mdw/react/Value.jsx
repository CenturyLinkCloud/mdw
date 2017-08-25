import React, {Component} from '../node/node_modules/react';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
var classNames = require('../node/node_modules/classnames');
var DatePicker = require('../node/node_modules/react-bootstrap-date-picker');

class Value extends Component {
    
  constructor(...args) {
    super(...args);
    this.showCalendar = this.showCalendar.bind(this);
    this.handleChange = this.handleChange.bind(this);
  }
  
  handleChange(event) {
  }
  
  showCalendar() {
    var elem = document.getElementById(this.props.value.name);
    elem.previousElementSibling.previousElementSibling.focus();    
  }
  
  render() {    
    var value = this.props.value;
    return (
      <div className={classNames('form-group', {'has-error': value.error})}>
        <label className={classNames('control-label', 'col-xs-2', {
            'mdw-required': value.display && value.display.required})} >
          {value.label ? value.label : value.name}
        </label>
        <div className={value.isDocument ? 'col-md-10' : 'col-xs-4'}>
          {value.type === 'java.lang.Exception' &&
            <textarea className="form-control mdw-document-input"  
              id={value.name} name={value.name} rows={value.showLines}
              readOnly={true} value={$mdwUi.util.asException(value)} />
          }
          {value.isDocument && value.type !== 'java.lang.Exception' &&
            <textarea className="form-control mdw-document-input" 
              id={value.name} name={value.name} rows={value.showLines} 
              readOnly={!value.editable} value={value.value} onChange={this.handleChange} />
          }
          {value.type === 'java.lang.Boolean' &&
            <input type="checkbox" className="checkbox mdw-boolean-input" 
              id={value.name} name={value.name} 
              readOnly={!value.editable} value={value.value} onChange={this.handleChange} />
          }
          {value.type === 'java.util.Date' &&
            <DatePicker
              id={value.name} name={value.name}
              clearButtonElement={<Glyphicon glyph="calendar" />}
              onClear={this.showCalendar}
              disabled={!value.editable} value={value.value} onChange={this.handleChange} />
          }
          {value.type !== 'java.util.Date' && value.type !== 'java.lang.Boolean' &&
              !value.isDocument && 
            <input type="text" className="form-control" 
              id={value.name} name={value.name} value={value.value} 
              readOnly={!value.editable} onChange={this.handleChange} />
          }
        </div>
      </div>
    );
  }
}

export default Value;   