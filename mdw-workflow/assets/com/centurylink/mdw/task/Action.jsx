import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Popover, OverlayTrigger, Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import {Link} from '../node/node_modules/react-router-dom';
import {AsyncTypeahead} from '../node/node_modules/react-bootstrap-typeahead';

class Action extends Component {

  constructor(...args) {
    super(...args);
    this.state = {actions: [], assignees: []};
    this.handleClick = this.handleClick.bind(this);
    this.handleChange = this.handleChange.bind(this);
    this.findAssignee = this.findAssignee.bind(this);
  }
  
  loadActions() {
    // id not available through props yet
    var id = this.props.task.id ? this.props.task.id : window.location.hash.substring(8);
    fetch(new Request(this.context.serviceRoot + '/Tasks/' + id + '/actions', {
      method: 'GET',
      headers: { Accept: 'application/json'}
    }))
    .then(response => {
      return response.json();
    })
    .then(actions => {
      this.setState({
        actions: actions
      });
    });    
  }
  
  componentDidMount() {
    this.loadActions();
  }
  
  handleClick(event) {
    if (event.currentTarget.name === 'assigneeInput') {
      event.preventDefault();
    }
    else if (event.currentTarget.localName === 'a' && event.currentTarget.name) {
      var action = this.state.actions.find(act => {
        return act.action === event.currentTarget.name; 
      });
      console.log('action: ' + JSON.stringify(action));
      if (action.action === 'Assign') {
        event.preventDefault();
      }
      else {
        var ok = false;
        var userAction = {
          action: action.action,
          user: 'dxoakes',
          taskInstanceId: this.props.task.id
        };
        fetch(new Request(this.context.serviceRoot + '/Tasks/' + this.props.task.id + '/' + action.action, {
          method: 'POST',
          headers: { Accept: 'application/json'},
          body: JSON.stringify(userAction)
        }))
        .then(response => {
          ok = response.ok;
          return response.json();
        })
        .then(json => {
          if (ok) {
            $mdwUi.clearMessage();
            this.loadActions();
            this.props.refreshTask(this.props.task.id);
          }
          else {
            $mdwUi.showMessage(json.status.message);
          }
        });    
        // close popovers
        this.refs.actionTrigger.hide();
        this.refs.assigneeTrigger.hide();
      }
    }
  }
  
  handleChange(event) {
    console.log("CHANGE...");
  }
  
  findAssignee(input) {
    console.log("FINDING::: " + input);
    fetch(new Request('/mdw/services/Tasks/assignees?find=' + input, {
      method: 'GET',
      headers: { Accept: 'application/json'}
    }))
    .then(response => {
      return response.json();
    })
    .then(json => {
      this.setState({
        assignees: json.users
      });
    });
  }
  
  popExit(event) {
    console.log("POP EXIT: " + event);
  }
  
  render() {
    const assigneePopover = (
      <Popover id='assignee-pop' style={{height:'200px', width:'200px'}}>
        <AsyncTypeahead onSearch={this.findAssignee} options={this.state.assignees} autoFocus
          className="form-control" labelKey="name" onChange={this.handleChange} bsSize="large"
          inputProps={{name:'assigneeInput', className: 'form-control'}} onClick={this.handleClick}/>        
      </Popover>
    );
    
    const actionPopover = (
      <Popover id='action-pop'>
        <ul className="dropdown-menu mdw-popover-menu">
          {this.state.actions.map(action => {
            return (
              <li key={action.action}>
                {action.action === 'Assign' &&
                  <OverlayTrigger ref="assigneeTrigger" trigger="click" 
                    placement="left" overlay={assigneePopover} rootClose={false} onExit={this.popExit} >
                    <Link to={this.context.hubRoot + '/tasks/' + this.props.task.id}
                      name={action.action} onClick={this.handleClick}>
                      {action.action}
                    </Link>
                  </OverlayTrigger>
                }
                {action.action !== 'Assign' &&
                  <Link to={this.context.hubRoot + '/tasks/' + this.props.task.id}
                    name={action.action} onClick={this.handleClick}>
                    {action.action}
                  </Link>
                }
              </li>
            );
          })}
        </ul>        
      </Popover>
    );
    
    return (
      <OverlayTrigger ref="actionTrigger" trigger="click" placement="left" 
        overlay={actionPopover} rootClose={true}>
        <Button className="mdw-btn" bsStyle="primary">
          <Glyphicon glyph="ok" />{" Action"}
        </Button>
      </OverlayTrigger>
    );
  }
}


Action.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string  
};

export default Action;
