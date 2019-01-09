import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Button, OverlayTrigger, Glyphicon} from '../node/node_modules/react-bootstrap';

class HeaderPopButton extends Component {

  constructor(...args) {
    super(...args);
    this.hide = this.hide.bind(this);
  }

  hide() {
    this.refs.overlayTriggerRef.hide();
  }

  render() {
    const left = this.props.glyph ? '4px' : null;
    const rootClose = this.props.rootClose !== false ? true : null;
    return (
      <OverlayTrigger trigger="click"
        placement="left"
        overlay={this.props.popover}
        rootClose={rootClose}
        ref="overlayTriggerRef">
        <Button bsStyle="primary" className="mdw-btn" style={{marginLeft:'4px'}}>
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
  glyph: PropTypes.string,
  rootClose: PropTypes.bool
};

export default HeaderPopButton;
