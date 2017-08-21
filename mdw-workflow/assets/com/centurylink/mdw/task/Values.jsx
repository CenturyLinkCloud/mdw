import React, {Component} from '../node/node_modules/react';

class Values extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { task: {} };
  }  

  render() {
    return (
      <div>
        task values
      </div>
    );
  }
}

export default Values;  