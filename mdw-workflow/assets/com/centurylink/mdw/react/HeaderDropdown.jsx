import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Dropdown, MenuItem, Glyphicon} from '../node/node_modules/react-bootstrap';

class HeaderDropdown extends Component {

  constructor(...args) {
    super(...args);
    this.handleSelect = this.handleSelect.bind(this);
  }

  handleSelect(eventKey) {
    if (this.props.onSelect) {
      this.props.onSelect(eventKey, this.props.id);
    }
  }

  render() {
    return (
      <div className="mdw-heading-input">
        <Dropdown id="{this.props.id}" className="mdw-dropdown"
          onSelect={this.handleSelect}>
          <Dropdown.Menu style={{marginTop:'-3px'}}>
            {
              this.props.items.map((item, i) => {
                return (
                  <MenuItem key={i} eventKey={item}
                    active={this.props.selected === item}>
                    {item}
                  </MenuItem>
                );
              })
            }
          </Dropdown.Menu>
          <Dropdown.Toggle noCaret={true} style={{padding:'5px',width:'140px',textAlign:'left'}}>
            <span style={{position:'relative',top:'-3px'}}>{this.props.selected}</span>
            <Glyphicon glyph="chevron-down" style={{color:'#9e9e9e',float:'right'}} />
          </Dropdown.Toggle>
        </Dropdown>
      </div>
    );
  }
}

HeaderDropdown.propTypes = {
  id: PropTypes.string.isRequired,
  items: PropTypes.arrayOf(PropTypes.string),
  selected: PropTypes.string,
  onSelect: PropTypes.func
};

export default HeaderDropdown;
