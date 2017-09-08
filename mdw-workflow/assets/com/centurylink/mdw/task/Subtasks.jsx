import React, {Component} from '../node/node_modules/react';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import Heading from './Heading.jsx';
import TaskItem from './TaskItem.jsx';


class Subtasks extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { subtasks: [] };
    this.handleNewSubtask = this.handleNewSubtask.bind(this);    
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
  
  handleNewSubtask(event) {
      window.location.assign('/mdw/tasks/' + this.props.task.id +'/newSubtask');
    }
  
  render() {
    return (
      <div>
        <Heading task={this.props.task} refreshTask={this.props.refreshTask}>
          <Button className="mdw-btn mdw-action-btn" bsStyle='primary' onClick={this.handleNewSubtask}>
            <Glyphicon glyph="plus" />{' New'}
          </Button>
        </Heading>
        <div className="mdw-section">
          <ul className="mdw-checklist">
            {this.state.subtasks.map(subtask => {
              return (
                <li key={subtask.id}>
                  <TaskItem task={subtask} />
                </li>
              );
            })}
          </ul>
         </div>
       </div>
    );
  }
}

export default Subtasks;  