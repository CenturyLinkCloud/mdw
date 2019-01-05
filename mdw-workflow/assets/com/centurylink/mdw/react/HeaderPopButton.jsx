import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Button, OverlayTrigger, Glyphicon} from '../node/node_modules/react-bootstrap';

class HeaderPopButton extends Component {

  constructor(...args) {
    super(...args);
  }

  render() {
    let left = this.props.glyph ? '4px' : null;
    return (
      <OverlayTrigger trigger="click" placement="left" overlay={this.props.popover} rootClose>
        <Button bsStyle="primary" className="mdw-btn">
          {this.props.glyph &&
            <Glyphicon glyph={this.props.glyph} />
          }
          <span style={{marginLeft:left}}>{this.props.label}</span>
        </Button>
      </OverlayTrigger>
    );
  }
}

HeaderPopButton.propTypes = {
  label: PropTypes.string.isRequired,
  glyph: PropTypes.string
};

export default HeaderPopButton;
