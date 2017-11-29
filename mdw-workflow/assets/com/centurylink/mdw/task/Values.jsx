import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import Heading from './Heading.jsx';
import values from '../react/values';
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
      headers: { Accept: 'application/json'},
      credentials: 'same-origin'
    }))
    .then(response => {
      return response.json();
    })
    .then(vals => {
      this.setState({
        values: values.toArray(vals)
      });
    });
  }
  
  handleChange(event, newValue) {
    this.setState({
      values: values.update(this.state.values.slice(), event, newValue)
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
      body: JSON.stringify(vals),
      credentials: 'same-origin'
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