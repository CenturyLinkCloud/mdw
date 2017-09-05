import React, {Component} from '../node/node_modules/react';
import ReactDOM from '../node/node_modules/react-dom';
import PropTypes from '../node/node_modules/prop-types';
import {Button, Glyphicon, Popover, OverlayTrigger} from '../node/node_modules/react-bootstrap';
var DatePicker = require('../node/node_modules/react-bootstrap-date-picker');

const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 
                'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const dayMs = 24 * 3600 * 1000;

class UserDate extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { task: {} };
    this.showCalendar = this.showCalendar.bind(this);
    this.expandCalendar = this.expandCalendar.bind(this);
    this.handleChange = this.handleChange.bind(this);
  }
  
  handleChange(isoString) {
    if (this.ignore) {
      this.ignore = false;
      return;
    }
    if (this.props.onChange) {
      this.props.onChange(new Date(isoString));
    }
    if (isoString != null) {
      this.refs.popoverTrigger.hide();
    }
  }
  
  expandCalendar(event) {
    event.srcElement.parentElement.parentElement.style.height = '280px';
  }
  
  showCalendar() {
    this.ignore = true;
    var datePicker = ReactDOM.findDOMNode(this.refs.datePicker);
    datePicker.firstElementChild.focus();
  }
  
  monthAndDay(date) {
    return months[date.getMonth()] + ' ' + date.getDate();
  }
  
  serviceDate(date) {
    return date.getFullYear() + '-' + months[date.getMonth()] + 
        '-' + this.padLeading(date.getDate(), 2, '0');
  }
    
  formatDateTime(date) {
    var ampm = 'am';
    var hours = date.getHours();
    if (hours > 11)
      ampm = 'pm';
    if (hours > 12)
      hours = hours - 12;
    return this.serviceDate(date) + ' ' + hours + ':' + 
        this.padLeading(date.getMinutes(), 2, '0') + ':' + 
        this.padLeading(date.getSeconds(), 2, '0') + ' ' + ampm;
  }

  padLeading(str, len, ch) {
    var ret = '' + str; // convert if nec.
    if (!ch)
      ch = ' ';
    while (ret.length < len)
      ret = ch + ret;
    return ret;
  }
  
  past(date) {
    var ret = months[date.getMonth()] + ' ' + date.getDate();
    var agoMs = Date.now() - date.getTime();
    if (agoMs < 60000) {
      var secs = Math.round(agoMs/1000);
      ret =  secs + (secs == 1 ? ' second ago' : ' seconds ago');
    }
    else if (agoMs < 3600000) {
      var mins = Math.round(agoMs/60000);
      ret = mins + (mins == 1 ? ' minute ago' : ' minutes ago');
    }
    else if (agoMs < 86400000) {
      var hrs = Math.round(agoMs/3600000);
      ret = hrs + (hrs == 1 ? ' hour ago' : ' hours ago');
    }
    else if (agoMs < 2592000000) {
      var days = Math.round(agoMs/86400000);
      ret = days + (days == 1 ? ' day ago' : ' days ago');
    }
    return ret;
  }
  
  future(date) {
    var ret = months[date.getMonth()] + ' ' + date.getDate();
    var inMs = date.getTime() - Date.now();
    if (inMs < 60000) {
      var secs = Math.round(inMs/1000);
      ret =  'in ' + secs + (secs == 1 ? ' second' : ' seconds');
    }
    else if (inMs < 3600000) {
      var mins = Math.round(inMs/60000);
      ret = 'in ' + mins + (mins == 1 ? ' minute' : ' minutes');
    }
    else if (inMs < 86400000) {
      var hrs = Math.round(inMs/3600000);
      ret = 'in ' + hrs + (hrs == 1 ? ' hour' : ' hours');
    }
    else if (inMs < 2592000000) {
      var days = Math.round(inMs/86400000);
      ret = 'in ' + days + (days == 1 ? ' day' : ' days');
    }
    return ret;
  }
  
  render() {
    var title = '', text = '';
    var date = this.props.date;
    if (typeof date === 'string')
      date = new Date(date);
    var past = false;
    if (date) {
      var title = this.formatDateTime(date);
      var text = title;
      if (date < Date.now()) {
        past = true;
        text = this.past(date)
      }
      else {
        text = this.future(date);
      }
    }

    const datePickerPopover = (
      <Popover id="date-picker-popover" placement="right" style={{width:'280px'}}>
        <div>
          <DatePicker
           ref="datePicker" clearButtonElement={<Glyphicon glyph="calendar" />}
           value={date ? date.toISOString() : null}
           onClear={this.showCalendar} onFocus={this.expandCalendar} onChange={this.handleChange} />
        </div>
      </Popover>
    );
    
    return (
      <span>
        {(date || !this.props.notLabel) &&
          <label title={title}>
            {this.props.label}:
          </label>
        }
        {date &&
          <span className="mdw-item-value">{text}</span>
        }
        {this.props.alert && past &&
          <img className="mdw-item-alert mdw-space" src={this.context.hubRoot + '/images/alert.png'} alt="alert" />
        }
        {!date && this.props.notLabel &&
          <span><i>{this.props.notLabel}</i></span>
        }
        {this.props.editable &&
          <OverlayTrigger ref="popoverTrigger" trigger="click" placement="right" 
            overlay={datePickerPopover} rootClose={true}>
            <Button type="button" className="mdw-btn" onClick={this.handleClick}
              style={{border: '1px solid #2e6da4', padding: '3px 5px 1px 6px', marginLeft: '8px'}}>
              <Glyphicon glyph="calendar" className="mdw-date-glyph" />
            </Button>
          </OverlayTrigger>
        }
      </span>
    );
  }
}

UserDate.contextTypes = {
  hubRoot: PropTypes.string
};

export default UserDate;  