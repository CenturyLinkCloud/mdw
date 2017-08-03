import React, {Component} from '../node/node_modules/react';

class Toolbox extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { task: {} };
  }  

  render() {
    return (
      <div>
        toolbox
      </div>
    );
  }
}

export default Toolbox;  