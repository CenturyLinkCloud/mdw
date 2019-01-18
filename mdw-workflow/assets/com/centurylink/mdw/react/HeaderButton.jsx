import React from '../node/node_modules/react';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';

function HeaderButton(props) {
  let left = props.glyph ? '4px' : null;
  return (
    <Button bsStyle="primary" className="mdw-btn" style={{marginLeft:'4px'}}
      onClick={props.onClick}>
      {props.glyph &&
        <Glyphicon glyph={props.glyph} />
      }
      <span style={{marginLeft:left}}>{props.label}</span>
    </Button>
  );
}

export default HeaderButton;
