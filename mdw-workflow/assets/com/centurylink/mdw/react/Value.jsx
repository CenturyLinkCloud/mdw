import React, {Component} from '../node/node_modules/react';
import UserDate from './UserDate.jsx';

class Value extends Component {
    
  constructor(...args) {
    super(...args);
    console.log("VALUE VALUE VALUE");
  }
  
  handleChange(event) {
    if (event.currentTarget.type === 'button') {
      if (event.currentTarget.value === 'save') {
        console.log('save task');
      }
    }
  }
  
  render() {
    var value = this.props.value;
    
    return (
      <div className={'form-group' + (value.error ? ' has-error' : '')}>
        <label className={'control-label col-xs-2' + (value.display && value.display.required ? 'mdw-required' : '')} >
          {value.label ? value.label : value.name}
        </label>
        <div className={value.isDocument ? 'col-md-10' : 'col-xs-4'}>
          {value.isException &&
            <textarea className="form-control mdw-document-input"
              rows="{value.showLines}" id="{value.name}" name="{value.name}" readonly={true}>
              {$mdwUi.util.asException(value)}
            </textarea>
          }
          {value.isDocument && !value.isException &&
            <textarea className="form-control mdw-document-input" 
              rows="{value.showLines}" id="{value.name}" name="{value.name}" 
              readonly={!value.editable} onChange={this.handleChange}>
              {value.value}
            </textarea>
          }
          {value.type === 'java.lang.Boolean' &&
            <input type="checkbox" className="checkbox mdw-boolean-input" 
              id="{value.name}" name="{value.name}" value={value.value} 
              readonly={!value.editable} onChange={this.handleChange} />
          }
          {value.type === 'java.util.Date' &&
            <UserDate date={value.value} />
          }
          {!value.isDocument && value.type !== 'java.lang.Boolean' && value.type !== 'java.util.Date' &&
            <input type="text" className="form-control" 
            id="{value.name}" name="{value.name}" value={value.value} 
            readonly={!value.editable} onChange={this.handleChange} />
          }
        </div>
      </div>
    );
  }
}

export default Value;   