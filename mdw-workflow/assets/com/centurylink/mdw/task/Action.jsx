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
    this.action = null; // selected action
    this.state = {actions: [], assignees: [], comment: ''};
    this.findAction = this.findAction.bind(this);
    this.handleActionClick = this.handleActionClick.bind(this);
    this.handlePendingActionClick = this.handlePendingActionClick.bind(this);
    this.actionPopRootClose = this.actionPopRootClose.bind(this);
    this.handleAssignAction = this.handleAssignAction.bind(this);
    this.findAssignee = this.findAssignee.bind(this);
    this.renderAssigneeMenu = this.renderAssigneeMenu.bind(this);
    this.assigneePopRootClose = this.assigneePopRootClose.bind(this);
    this.handleCommentChange = this.handleCommentChange.bind(this);
    this.handleCommentedAction = this.handleCommentedAction.bind(this);
    this.commentPopRootClose = this.commentPopRootClose.bind(this);
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
      actions.forEach(action => {
        action.label = action.alias ? action.alias : action.action;
      });
      this.setState({
        actions: actions,
        assignees: [],
        comment: ''
      });
    });    
  }
  
  componentDidMount() {
    this.loadActions();
  }
  
  performAction(assignee, comment) {
    var ok = false;
    var userAction = {
      action: this.action.action,
      user: $mdwUi.authUser.cuid,
      taskInstanceId: this.props.task.id
    };
    if (assignee)
      userAction.assignee = assignee;
    if (comment)
      userAction.comment = comment;
    
    fetch(new Request(this.context.serviceRoot + '/Tasks/' + 
        this.props.task.id + '/' + this.action.action, {
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
  
  findAction(name) {
    return this.state.actions.find(act => {
      return act.action === name; 
    });
  }
  
  handleActionClick(event) {
    event.preventDefault();
    this.action = this.findAction(event.currentTarget.name);
    this.performAction();
    this.refs.actionTrigger.hide();
  }
  
  // actions that require assignee or comment
  handlePendingActionClick(event) {
    event.preventDefault();
    this.action = this.findAction(event.currentTarget.name);
  }
  
  actionPopRootClose() {
    var actionTrigger = this.refs.actionTrigger;
    var commentTrigger = this.action ? this.refs[this.action.action + '_commentTrigger'] : null;
    var assigneeTrigger = this.refs.assigneeTrigger;
    if (actionTrigger.state.show && !assigneeTrigger.state.show && 
        (!commentTrigger || !commentTrigger.state.show)) {
      actionTrigger.hide();
    }
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
          <AssigneeMenuItem key={user.cuid} option={user} position={index} 
            linkTo={linkTo} onChange={this.handleAssignAction}>
            {user.name}
          </AssigneeMenuItem>
        ))}
      </Menu>
    );
  }

  handleAssignAction(users) {
    var user = users.constructor === Array ? users[0] : users;
    this.performAction(user.cuid);
    this.assigneePopRootClose();
  }

  assigneePopRootClose() {
    this.hidePopover('assigneeTrigger');
    this.hidePopover('actionTrigger');
  }

  handleCommentChange(event) {
    var comment = event.currentTarget.value;
    this.setState( {
      actions: this.state.actions,
      assignees: [],
      comment: comment
    });
  }
  
  handleCommentedAction() {
    this.performAction(null, this.state.comment);
    this.hidePopover(this.action.action + '_commentTrigger');
    this.hidePopover('actionTrigger');
  }

  commentPopRootClose() {
    this.hidePopover(this.action.action + '_commentTrigger');
    this.hidePopover('actionTrigger');
  }

  hidePopover(trigger) {
    if (this.refs[trigger].state.show)
      this.refs[trigger].hide();
  }
  
  
  // custom RootCloseWrappers to prevent closing action-pop when assignee-pop is clicked
  render() {
    const assigneePopover = (
      <Popover id="assignee-pop" style={{width:'250px'}} placement="left">
        <RootCloseWrapper onRootClose={this.assigneePopRootClose}>
          <div>
            <AsyncTypeahead ref="typeahead" onSearch={this.findAssignee} autoFocus
              options={this.state.assignees} bodyContainer={true} labelKey="name" 
              minLength={1} placeholder=' find user' inputProps={{name:'assigneeInput'}} 
              renderMenu={this.renderAssigneeMenu} onChange={this.handleAssignAction} />
          </div>
        </RootCloseWrapper>
      </Popover>
    );

    const commentPopover = (
      <Popover id="comment-pop" style={{width:'350px'}} placement="left">
        <RootCloseWrapper onRootClose={this.assigneePopRootClose}>
          <div>
            <div>
              <textarea id="action-comment" className="form-control" rows={8} autoFocus
                placeholder={'enter reason'} style={{fontSize:'14px'}} onChange={this.handleCommentChange}
                value={this.state.comment} />
            </div>
            <div style={{marginTop:'6px'}}>
              <Button className="mdw-btn" onClick={this.handleCommentedAction}>
                {' Submit'}
              </Button>
            </div>
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
                        name={action.action} onClick={this.handlePendingActionClick}>
                        {action.label}
                      </Link>
                    </OverlayTrigger>
                  }
                  {action.action !== 'Assign' && action.requireComment &&
                    <OverlayTrigger ref={action.action + '_commentTrigger'} trigger="click" 
                      placement="left" overlay={commentPopover} rootClose={false}>
                      <Link to={this.context.hubRoot + '/tasks/' + this.props.task.id}
                        name={action.action} onClick={this.handlePendingActionClick}>
                        {action.label}
                      </Link>
                    </OverlayTrigger>
                  }
                  {action.action !== 'Assign' && !action.requireComment &&
                    <Link to={this.context.hubRoot + '/tasks/' + this.props.task.id}
                      name={action.action} onClick={this.handleActionClick}>
                      {action.label}
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
        <Button className="mdw-btn mdw-action-btn" bsStyle="primary">
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
  
  handleClick(event) { // eslint-disable-line no-unused-vars
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
