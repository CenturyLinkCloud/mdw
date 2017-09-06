import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import {Link} from '../node/node_modules/react-router-dom';
import Action from './Action.jsx';

class Heading extends Component {
    
  constructor(...args) {
    super(...args);
  }  

  render() {
    return (
      <div className="panel-heading mdw-heading">
        <div className="mdw-heading-label">
          {this.props.task.name}
          <Link to={this.context.hubRoot + '/tasks/' + this.props.task.id} className="mdw-id">
            {this.props.task.id}
          </Link>
          {this.props.task.dirty && <span className="mdw-dirty">*</span>}
        </div>
        <div className="mdw-heading-actions">
          {this.props.children}
          {this.props.task.actionable &&
            <Action task={this.props.task} refreshTask={this.props.refreshTask} />
          }
        </div>
      </div>
    );
  }
}

Heading.contextTypes = {
  hubRoot: PropTypes.string
};

export default Heading; 