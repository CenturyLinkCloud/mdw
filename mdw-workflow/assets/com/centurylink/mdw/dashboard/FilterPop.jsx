import React, {Component} from '../node/node_modules/react';
import {Popover, Button} from '../node/node_modules/react-bootstrap';
import Dropdown from '../react/Dropdown.jsx';
import DatePicker from '../react/DatePicker.jsx';

class FilterPop extends Component {

  constructor(...args) {
    super(...args);
    this.handleChange = this.handleChange.bind(this);
    this.handleReset = this.handleReset.bind(this);
  }

  handleChange(key, value) {
    if (this.props.onFilterChange) {
      var filters = Object.assign({}, this.props.filters);
      filters[key] = value;
      this.props.onFilterChange(filters);
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
        <div style={{width:'150px'}}>
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
                  <label className="mdw-label">{key + ':'}</label>
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
                    <input type="checkbox" id={id}
                      style={{marginTop:'3px',marginLeft:'6px',fontSize:'24px'}}
                      checked={filters[key]}
                      onChange={event => this.handleChange(key, event.target.checked)} />
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
          </div>
        </div>
      </Popover>
    );
  }
}

export default FilterPop;
