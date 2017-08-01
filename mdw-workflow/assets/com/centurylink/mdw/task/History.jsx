 import React, {Component} from '../node/node_modules/react';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';

class History extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { task: {} };
  }  

  componentDidMount() {
  }
  
  render() {
    return (
      <div className="panel panel-default mdw-panel">
        <div className="mdw-section">
            task history
        </div>
      </div>
    );
  }
}

export default History; 