import React, {Component} from '../node/node_modules/react';
import UserDate from './UserDate.jsx';

class Subtask extends Component {
    
  constructor(...args) {
    super(...args);
    console.log("subTask ...");
  }
  
  handleChange(event) {
    if (event.currentTarget.type === 'button') {
      if (event.currentTarget.subTask === 'save') {
        console.log('save subTask');
      }
    }
  }
}

export default Subtask;   