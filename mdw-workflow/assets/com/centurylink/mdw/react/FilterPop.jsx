import React, {Component} from '../node/node_modules/react';
import {Popover, Button} from '../node/node_modules/react-bootstrap/lib';
import Dropdown from './Dropdown.jsx';
import DatePicker from './DatePicker.jsx';

class FilterPop extends Component {

  constructor(...args) {
    super(...args);
    this.handleChange = this.handleChange.bind(this);
    this.handleReset = this.handleReset.bind(this);
  }

  handleChange(key, value, isText) {
    if (this.props.onFilterChange) {
      var filters = Object.assign({}, this.props.filters);
      filters[key] = value;
      this.props.onFilterChange(filters, isText);
    }
  }

  handleReset() {
    if (this.props.onFilterReset) {
      this.props.onFilterReset();
    }
    document.body.click();
  }

  render() {
    const {filters, filterOptions, onFilterChange, onFilterReset, ...popProps} = this.props; // eslint-disable-line no-unused-vars
    return (
      <Popover {...popProps} id="filter-pop">
        <div style={{width:'175px'}}>
          {
            Object.keys(filters).map(key => {
              const id = key.replace(/\s+/g, '-').toLowerCase();
              const value = filters[key];
              const isCb = typeof(value) === 'boolean';
              const isDate = value instanceof Date;
              const isDropdown = this.props.filterOptions && this.props.filterOptions[key];
              return (
                <div key={key} className={isDate ? '' : 'mdw-vsm-indent'}
                  style={{display:isCb ? 'flex' : 'block', marginTop:isCb ? '5px' : '3px'}}>
                  {!isCb &&
                    <label className="mdw-label">{key + ':'}</label>
                  }
                  {isDate &&
                    <div className="mdw-flex-item">
                      <DatePicker id={id}
                        date={value}
                        onChange={date => this.handleChange(key, date)} />
                    </div>
                  }
                  {isDropdown &&
                    <Dropdown id={id}
                      items={this.props.filterOptions[key]}
                      selected={filters[key]}
                      onSelect={sel => this.handleChange(key, sel)} />
                  }
                  {isCb &&
                    <span>
                      <input type="checkbox" id={id}
                        style={{marginTop:'3px',fontSize:'24px'}}
                        checked={filters[key]}
                        onChange={event => {this.handleChange(key, event.target.checked);document.body.click();}} />
                      <label className="mdw-label" style={{display:'inline',marginLeft:'5px'}}>{key}</label>
                    </span>
                  }
                  {!isDate && !isDropdown && !isCb &&
                    <input type="text" id={id} style={{width:'100%'}}
                      value={filters[key]}
                      onChange={event => this.handleChange(key, event.target.value, true)}
                      onKeyDown={event => {if (event.key === 'Enter') {document.body.click();}} } />
                  }
                </div>
              );
            })
          }
          <div className="mdw-vmed-indent">
            <Button bsStyle="primary" className="mdw-btn"
              onClick={this.handleReset}>
              Reset
            </Button>
            {this.props.onClose &&
              <button type="button" className="btn mdw-btn mdw-cancel-btn mdw-float-right"
                onClick={this.props.onClose}>
                Close
              </button>
            }
          </div>
        </div>
      </Popover>
    );
  }
}

export default FilterPop;
