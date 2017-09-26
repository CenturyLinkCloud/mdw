import React, {Component} from '../node/node_modules/react';

class Subtasks extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { task: {} };
  }  

  render() {
    return (
      <div>
        subtasks
      </div>
    );
  }
}

export default Subtasks;  