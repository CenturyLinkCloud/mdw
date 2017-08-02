import React, {Component} from '../node/node_modules/react';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
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
          <a href={'/mdw/tasks/' + this.props.task.id} className="mdw-id">
            {this.props.task.id}
          </a>
          {this.props.task.dirty && <span className="mdw-dirty">*</span>}
        </div>
        <div className="mdw-heading-actions">
          <Action task={this.props.task} />
        </div>
      </div>
    );
  }
}

export default Heading; 