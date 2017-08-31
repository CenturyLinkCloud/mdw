import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Popover, OverlayTrigger, Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import RootCloseWrapper from '../node/node_modules/react-overlays/lib/RootCloseWrapper';
import {Link} from '../node/node_modules/react-router-dom';
import {AsyncTypeahead, Menu, MenuItem} from '../node/node_modules/react-bootstrap-typeahead';
import '../node/node_modules/style-loader!../react/typeahead.css';

class Action extends Component {

  constructor(...args) {
    super(...args);
    this.state = {actions: [], assignees: [], assigneePopOpen: false};
    this.handleActionClick = this.handleActionClick.bind(this);
    this.handleAssigneeChange = this.handleAssigneeChange.bind(this);
    this.findAssignee = this.findAssignee.bind(this);
    this.renderAssigneeMenu = this.renderAssigneeMenu.bind(this);
    this.assigneePopRootClose = this.assigneePopRootClose.bind(this);
    this.actionPopRootClose = this.actionPopRootClose.bind(this);
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
  
  performAction(action, assignee) {
    var ok = false;
    var userAction = {
      action: action,
      user: $mdwUi.authUser.cuid,
      taskInstanceId: this.props.task.id
    };
    if (assignee)
      userAction.assignee = assignee;
    
    fetch(new Request(this.context.serviceRoot + '/Tasks/' + this.props.task.id + '/' + action, {
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
  }
  
  handleActionClick(event) {
    if (event.currentTarget.localName === 'a' && event.currentTarget.name) {
      var action = this.state.actions.find(act => {
        return act.action === event.currentTarget.name; 
      });
      this.performAction(action.action);
      // close popover
      this.refs.actionTrigger.hide();
    }
    return false;
  }
  
  ignoreClick(event) {
    event.preventDefault();
    return false;
  }
  
  handleAssigneeChange(users) {
    var user = users.constructor === Array ? users[0] : users;
    this.performAction('Assign', user.cuid);
    this.assigneePopRootClose();
  }

  findAssignee(input) {
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
  
  renderAssigneeMenu(users, menuProps) {
    var linkTo = this.context.hubRoot + '/tasks/' + this.props.task.id;
    return (
      <Menu {...menuProps}>
        {users.map((user, index) => (
          <AssigneeMenuItem key={user.cuid} option={user} position={index} linkTo={linkTo}
            onChange={this.handleAssigneeChange}>
            {user.name}
          </AssigneeMenuItem>
        ))}
      </Menu>
    );
  }
  
  assigneePopRootClose() {
    var assigneeTrigger = this.refs.assigneeTrigger;
    if (assigneeTrigger.state.show)
      assigneeTrigger.hide();
    if (this.refs.actionTrigger.state.show)
      this.refs.actionTrigger.hide();
  }
  actionPopRootClose() {
    var actionTrigger = this.refs.actionTrigger;
    if (actionTrigger.state.show && !this.refs.assigneeTrigger.state.show) {
      actionTrigger.hide();
    }
  }
  
  render() {
    // custom RootCloseWrappers to prevent closing action-pop when assignee-pop is clicked
    const assigneePopover = (
      <Popover id="assignee-pop" style={{width:'250px'}} placement="left">
        <RootCloseWrapper onRootClose={this.assigneePopRootClose}>
          <div>
            <AsyncTypeahead ref="typeahead" onSearch={this.findAssignee} options={this.state.assignees} 
              autoFocus bodyContainer={true} labelKey="name" minLength={1} placeholder=' find user' 
              inputProps={{name:'assigneeInput'}} renderMenu={this.renderAssigneeMenu} onChange={this.handleAssigneeChange} />
          </div>
        </RootCloseWrapper>
      </Popover>
    );

    const actionPopover = (
        <Popover id="action-pop">
          <RootCloseWrapper onRootClose={this.actionPopRootClose}>
            <ul className="dropdown-menu mdw-popover-menu">
              {this.state.actions.map(action => {
                return (
                  <li key={action.action}>
                    {action.action === 'Assign' &&
                      <OverlayTrigger ref="assigneeTrigger" trigger="click" 
                        placement="left" overlay={assigneePopover} rootClose={false}>
                        <Link to={this.context.hubRoot + '/tasks/' + this.props.task.id}
                          name={action.action} onClick={this.ignoreClick}>
                          {action.action}
                        </Link>
                      </OverlayTrigger>
                    }
                    {action.action !== 'Assign' &&
                      <Link to={this.context.hubRoot + '/tasks/' + this.props.task.id}
                        name={action.action} onClick={this.handleActionClick}>
                        {action.action}
                      </Link>
                    }
                  </li>
                );
              })}
            </ul>        
          </RootCloseWrapper>
        </Popover>
    );

    return (
      <OverlayTrigger ref="actionTrigger" trigger="click" placement="left" 
        overlay={actionPopover} rootClose={false}>
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

class AssigneeMenuItem extends MenuItem {
  constructor(...args) {
    super(...args);
    this.handleClick = this.handleClick.bind(this);
  }
  
  handleClick(event) {
    this.props.onChange(this.props.option);
  }
  
  render() {
    return (
      <li>
        <Link to={this.props.linkTo} className="dropdown-item" 
          name={this.props.option.cuid} onClick={this.handleClick}> 
          {this.props.option.name}
        </Link>
      </li>
    );
  }    
}

export default Action;
