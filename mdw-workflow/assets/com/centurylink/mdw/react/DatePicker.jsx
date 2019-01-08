import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Glyphicon} from '../node/node_modules/react-bootstrap';
var RbsDatePicker = require('../node/node_modules/react-bootstrap-date-picker');

class DatePicker extends Component {

  constructor(...args) {
    super(...args);
    this.state = {date: this.props.date};
    this.showCalendar = this.showCalendar.bind(this);
    this.handleDateChange = this.handleDateChange.bind(this);
  }

  showCalendar() {
    this.ignore = true;
    var elem = document.getElementById(this.props.id);
    elem.previousElementSibling.previousElementSibling.focus();
  }

  handleDateChange(isoString) {
    if (this.ignore) {
      this.ignore = false;
      return;
    }
    this.setState({
      date: new Date(isoString)
    }, () => {
      if (this.props.onChange) {
        this.props.onChange(this.state.date);
      }
    });
  }

  render() {
    return (
      <RbsDatePicker id={this.props.id} style={{width:'110px'}}
        clearButtonElement={<Glyphicon glyph="calendar" />}
        calendarContainer={document.body}
        onClear={this.showCalendar}
        value={this.state.date ? this.state.date.toISOString() : null}
        onChange={this.handleDateChange} />
    );
  }
}

DatePicker.propTypes = {
  id: PropTypes.string.isRequired,
  date: PropTypes.object // Date
};

export default DatePicker;
