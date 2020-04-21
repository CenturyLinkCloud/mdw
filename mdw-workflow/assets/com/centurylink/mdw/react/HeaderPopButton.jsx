import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Button, OverlayTrigger, Glyphicon} from '../node/node_modules/react-bootstrap';
import mobile from './mobile';

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
    const placement = mobile.isMobile() ? 'right' : 'left';
    return (
      <OverlayTrigger trigger="click"
        placement={this.props.placement || placement}
        overlay={this.props.popover}
        rootClose={rootClose}
        ref="overlayTriggerRef">
        <Button bsStyle="primary" className="mdw-btn" style={{marginLeft:'4px'}}
          title={this.props.title} disabled={this.props.disabled}>
          {this.props.glyph &&
            <Glyphicon glyph={this.props.glyph} />
          }
          {this.props.label &&
            <span style={{marginLeft:left}}>
              {this.props.label}
              {this.props.dirty &&
                <span style={{position:'relative',paddingRight:'6px'}}>
                  <span style={{fontWeight:'bold',fontSize:'16px',position:'absolute',left:'3px',top:'-4px'}}>
                    {' *'}
                  </span>
                </span>
              }
            </span>
          }
        </Button>
      </OverlayTrigger>
    );
  }
}

HeaderPopButton.propTypes = {
  label: PropTypes.string,
  glyph: PropTypes.string,
  rootClose: PropTypes.bool,
  dirty: PropTypes.bool,
  disabled: PropTypes.bool
};

export default HeaderPopButton;
