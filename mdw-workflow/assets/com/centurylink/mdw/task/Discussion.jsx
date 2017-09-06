import React, {Component} from '../node/node_modules/react';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import Heading from './Heading.jsx';

class Discussion extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { task: {} };
  }  

  render() {
    return (
      <div>
        <Heading task={this.props.task}>
          <Button className="mdw-btn mdw-action-btn" bsStyle='primary'>
            <Glyphicon glyph="plus" />{' New'}
          </Button>
        </Heading>
        <div className="mdw-section">
          TODO: task discussion
        </div>
      </div>
    );
  }
}

export default Discussion;  