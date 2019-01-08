import React, {Component} from '../node/node_modules/react';
import {Popover, Button} from '../node/node_modules/react-bootstrap';
import Dropdown from '../react/Dropdown.jsx';
import DatePicker from '../react/DatePicker.jsx';

class FilterPop extends Component {

  constructor(...args) {
    super(...args);
    this.state = {filters: this.props.filters};
    this.handleEndDateChange = this.handleEndDateChange.bind(this);
    this.handleStatusChange = this.handleStatusChange.bind(this);
    this.handleReset = this.handleReset.bind(this);
  }

  handleEndDateChange(endDate) {
    this.setState({
      filters: {
        ending: endDate,
        status: this.state.filters.status
      }
    }, () => {
      if (this.props.onChange) {
        this.props.onChange(this.state.filters);
      }
    });
  }

  handleStatusChange(status) {
    this.setState({
      filters: {
        ending: this.state.filters.ending,
        status: status
      }
    }, () => {
      if (this.props.onChange) {
        this.props.onChange(this.state.filters);
      }
    });
  }

  handleReset() {
    this.setState({
      filters: {
        ending: new Date(),
        status: null
      }
    }, () => {
      if (this.props.onChange) {
        this.props.onChange(this.state.filters);
      }
    });
    document.body.click();
  }

  render() {
    const {filters, statuses, ...popProps} = this.props; // eslint-disable-line no-unused-vars
    return (
      <Popover {...popProps} id="filter-pop">
        <div style={{width:'150px'}}>
          <div>
            <label className="mdw-label">Ending:</label>
            <div className="mdw-flex-item">
              <DatePicker id="end-date-picker"
                date={this.state.filters.ending}
                onChange={this.handleEndDateChange} />
            </div>
          </div>
          {this.props.statuses &&
            <div className="mdw-vsm-indent">
              <label className="mdw-label">Status:</label>
              <Dropdown id="status-dropdown"
                items={statuses}
                selected={this.state.filters.status}
                onSelect={this.handleStatusChange} />
            </div>
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
