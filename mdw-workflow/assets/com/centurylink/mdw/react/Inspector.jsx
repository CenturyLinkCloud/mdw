import React, {Component} from '../node/node_modules/react';

class Inspector extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { task: {} };
  }  

  render() {
    return (
      <div>
        inspector
      </div>
    );
  }
}

export default Inspector;  