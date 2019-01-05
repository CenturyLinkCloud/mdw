import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Dropdown, MenuItem, Glyphicon} from '../node/node_modules/react-bootstrap';

class HeaderDropdown extends Component {

  constructor(...args) {
    super(...args);
  }

  render() {
    return (
      <div className="mdw-heading-input">
        <Dropdown id="{this.props.id}" className="mdw-heading-dropdown">
          <Dropdown.Menu style={{marginTop:'-3px'}}>
            {
              this.props.items.map((item, i) => {
                return (
                  <MenuItem key={i} active={this.props.selected === item}>
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
  selected: PropTypes.string
};

export default HeaderDropdown;
