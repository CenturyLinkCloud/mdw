import React, {Component} from '../node/node_modules/react';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';

class Task extends Component {
    
  constructor(...args) {
    super(...args);
    this.handleClick = this.handleClick.bind(this);
  }  

  handleClick(event) {
    if (event.currentTarget.type === 'button') {
      if (event.currentTarget.value === 'save') {
        console.log('save task');
      }
    }
  }
  
  render() {
    return (
      <div >
        hello task {this.props.task.id}
      </div>
    );
  }
}

export default Task; 