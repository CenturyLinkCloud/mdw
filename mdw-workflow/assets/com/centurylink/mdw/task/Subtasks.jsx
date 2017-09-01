import React, {Component} from '../node/node_modules/react';
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
      <ul className="mdw-checklist">
        {this.state.subtasks.map(subtask => {
          return (
            <li key={subtask.id}>
              <TaskItem task={subtask} />
            </li>
          );
        })}
      </ul>
    );
  }
}

export default Subtasks;  