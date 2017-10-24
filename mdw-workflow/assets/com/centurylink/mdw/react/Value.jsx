import React, {Component} from '../node/node_modules/react';
import {Glyphicon} from '../node/node_modules/react-bootstrap';
var classNames = require('../node/node_modules/classnames');
var DatePicker = require('../node/node_modules/react-bootstrap-date-picker');

class Value extends Component {
    
  constructor(...args) {
    super(...args);
    this.showCalendar = this.showCalendar.bind(this);
    this.handleDateChange = this.handleDateChange.bind(this);
  }
  
  showCalendar() {
    this.ignore = true;
    var elem = document.getElementById(this.props.value.name);
    elem.previousElementSibling.previousElementSibling.focus();    
  }
  
  handleDateChange(isoString) {
    if (this.ignore) {
      this.ignore = false;
      return;
    }
    if (this.props.onChange) {
      this.props.onChange({currentTarget:{name: this.props.value.name}}, isoString);
    }
  }
  
  render() {
    var value = this.props.value;
    var editable = this.props.editable && value.display !== 'ReadOnly';

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
              readOnly={!editable} value={value.value} onChange={this.props.onChange} />
          }
          {value.type === 'java.lang.Boolean' &&
            <input type="checkbox" className="checkbox mdw-boolean-input" 
              id={value.name} name={value.name} 
              readOnly={!editable} checked={value.value === 'true'} onChange={this.props.onChange} />
          }
          {value.type === 'java.util.Date' &&
            <DatePicker
              id={value.name} name={value.name}
              clearButtonElement={<Glyphicon glyph="calendar" />}
              onClear={this.showCalendar}
              disabled={!editable} value={value.value} onChange={this.handleDateChange} />
          }
          {value.type !== 'java.util.Date' && value.type !== 'java.lang.Boolean' &&
              !value.isDocument && 
            <input type="text" className="form-control" 
              id={value.name} name={value.name} value={value.value} 
              readOnly={!editable} onChange={this.props.onChange} />
          }
        </div>
      </div>
    );
  }
}

export default Value;   