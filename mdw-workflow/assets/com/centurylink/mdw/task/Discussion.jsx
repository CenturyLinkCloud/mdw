import React, {Component} from '../node/node_modules/react';

class Discussion extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { task: {} };
  }  

  render() {
    return (
      <div>
        task discussion
      </div>
    );
  }
}

export default Discussion;  