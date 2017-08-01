import React, {Component} from '../node/node_modules/react';
import {Popover, OverlayTrigger, Button, Glyphicon} from '../node/node_modules/react-bootstrap';

class Action extends Component {

  constructor(...args) {
    super(...args);
  }
  
  componentDidMount() {
    console.log('HASH: ' + window.location.hash);
  }
  
  render() {
    const popover = (
      <Popover id='action-pop'>
        <div>
          <label>Hello Action</label>
        </div>
      </Popover>
    );
      
    return (
      <OverlayTrigger trigger='click' placement='left' overlay={popover} rootClose={true}>
        <Button className="mdw-btn" bsStyle='primary'>
          <Glyphicon glyph='ok' />{' Action'}
        </Button>
      </OverlayTrigger>
    );
  }
}

export default Action;
