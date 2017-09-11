import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import {Link} from '../node/node_modules/react-router-dom';
import Heading from './Heading.jsx';
import TaskItem from './TaskItem.jsx';

class Subtasks extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { subtasks: [] };
  }  

  componentDidMount() {
    fetch(new Request('/mdw/services/Tasks/' + this.props.task.id + '/subtasks', {
      method: 'GET',
      headers: { Accept: 'application/json'}
    }))
    .then(response => {
      return response.json();
    })
    .then(json => {
      var subtasks = json['subtasks'];
      this.setState({
        subtasks: subtasks
      });
    });
  }
  
  render() {
    return (
      <div>
        <Heading task={this.props.task} refreshTask={this.props.refreshTask}>
          <Link to={this.context.hubRoot + '/tasks/' + this.props.task.id + '/newSubtask'}
            className="btn mdw-btn btn-primary" style={{fontWeight:'normal',fontSize:'14px'}}>
            <Glyphicon glyph="plus" />{' New'}
          </Link>
        </Heading>
        <div className="mdw-section">
          <ul className="mdw-checklist">
            {this.state.subtasks.map(subtask => {
              return (
                <li key={subtask.id}>
                  <TaskItem task={subtask} updateTask={this.props.updateTask} refreshTask={this.props.refreshTask} />
                </li>
              );
            })}
          </ul>
         </div>
       </div>
    );
  }
}

Subtasks.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string  
};

export default Subtasks;  