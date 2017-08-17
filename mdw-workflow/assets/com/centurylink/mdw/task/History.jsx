import React, {Component} from '../node/node_modules/react';

class History extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { task: {} };
  }  

  render() {
    return (
      <div>
        task history
      </div>
    );
  }
}

export default History; 