import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Dropdown, MenuItem, Glyphicon} from '../node/node_modules/react-bootstrap';

class AssetDropdown extends Component {

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
    var width = 140;
    var selName;
    if (this.props.selected) {
      selName = this.props.selected.substring(this.props.selected.lastIndexOf('/') + 1);
      const canvas = document.createElement('canvas');
      const context = canvas.getContext('2d');
      context.font = '14px "Helvetica Neue", Helvetica, Arial, sans-serif';
      const metrics = context.measureText(selName);
      const selWidth = metrics.width + 35;
      if (selWidth > width) {
        width = selWidth;
      }
    }

    const items = [];
    this.props.packages.forEach(pkg => {
      pkg.isPackage = true;
      items.push(pkg);
      if (pkg.assets) {
        pkg.assets.forEach(asset => {
          asset.package = pkg.name;
          items.push(asset);
        });
      }
    });

    return (

      <div className="mdw-heading-input">
        <Dropdown id="{this.props.id}" className="mdw-dropdown mdw-asset-dropdown"
          onSelect={this.handleSelect}>
          <Dropdown.Menu style={{marginTop:'-3px'}}>
            {
              items.map((item, i) => {
                return (
                  <MenuItem key={i} style={{marginLeft:item.isPackage?'0':'20px'}}
                    eventKey={item.isPackage ? item.name : item.package + '/' + item.name} 
                    disabled={item.isPackage} 
                    active={false}>
                    <span>
                      {item.isPackage &&
                        <Glyphicon glyph="folder-open" style={{left:'-7px'}} />                    
                      }
                    </span>
                    {item.name}
                  </MenuItem>
                );
              })
            }
          </Dropdown.Menu>
          <Dropdown.Toggle noCaret={true} style={{padding:'5px',width:width+'px',textAlign:'left'}}
            title={this.props.selected}>
            <span style={{position:'relative',top:'-3px'}}>{selName}</span>
            <Glyphicon glyph="chevron-down" style={{color:'#9e9e9e',float:'right'}} />
          </Dropdown.Toggle>
        </Dropdown>
      </div>
    );
  }
}

AssetDropdown.propTypes = {
  id: PropTypes.string.isRequired,
  packages: PropTypes.arrayOf(PropTypes.object).isRequired,
  selected: PropTypes.string, // asset path
  onSelect: PropTypes.func
};

export default AssetDropdown;
