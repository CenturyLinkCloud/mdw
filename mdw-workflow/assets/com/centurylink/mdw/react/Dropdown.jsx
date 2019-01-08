import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Popover, Dropdown as BsDropdown, MenuItem, OverlayTrigger} from '../node/node_modules/react-bootstrap';

class Dropdown extends Component {

  constructor(...args) {
    super(...args);
    this.state = { selected: this.props.selected || ''};
    this.handleSelect = this.handleSelect.bind(this);
  }

  handleSelect(eventKey) {
    this.setState({ selected: eventKey });
    if (this.props.onSelect) {
      this.props.onSelect(eventKey, this.props.id);
    }
  }

  render() {
    const menu = (
      <Popover id={this.props.id}>
        <BsDropdown.Menu style={{display:'block',top:'-13px',left:'-60px'}}>
          {
            this.props.items.map((item, i) => {
              return (
                <MenuItem key={i} eventKey={item}
                  active={this.state.selected === item}
                  onClick={() => this.handleSelect(item)}>
                  {item}
                </MenuItem>
              );
            })
          }
        </BsDropdown.Menu>
      </Popover>
    );

    return (
      <OverlayTrigger id={this.props.id + '-trigger'}
        trigger="click" placement="bottom" overlay={menu} rootClose>
        <div className="mdw-inner-addon mdw-right-addon">
          <i className="glyphicon glyphicon-chevron-down"></i>
          <input id={this.props.id} type="text" className="form-control mdw-inline mdw-dropfilter"
            value={this.state.selected} readOnly />
        </div>
      </OverlayTrigger>
    );
  }
}

Dropdown.propTypes = {
  id: PropTypes.string.isRequired,
  items: PropTypes.arrayOf(PropTypes.string),
  selected: PropTypes.string,
  onSelect: PropTypes.func
};

export default Dropdown;
