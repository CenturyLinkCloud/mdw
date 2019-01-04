import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Button, Dropdown, Glyphicon, MenuItem, OverlayTrigger, Popover} from '../node/node_modules/react-bootstrap';

class ChartHeading extends Component {

  constructor(...args) {
    super(...args);
    this.state = {
      timespan: 'Week',
      breakdown: this.props.breakdownConfig.breakdowns[0].name
    };
  }

  componentDidMount() {
  }

  render() {

    const selectPopover = (
      <Popover id="select-popover">
        <strong>Holy guacamole!</strong> Check this info.
      </Popover>
    );

    return (
      <div className="panel-heading mdw-heading">
        <div className="mdw-heading-label">{this.props.title} for the:</div>
        <div className="mdw-heading-input">
          <Dropdown id="timespan-dropdown" className="mdw-heading-dropdown">
            <Dropdown.Menu style={{marginTop:'-3px'}}>
              <MenuItem active={this.state.timespan === 'Week'}>Week</MenuItem>
              <MenuItem active={this.state.timespan === 'Month'}>Month</MenuItem>
            </Dropdown.Menu>
            <Dropdown.Toggle noCaret={true} style={{padding:'5px',width:'140px',textAlign:'left'}}>
              <span style={{position:'relative',top:'-3px'}}>{this.state.timespan}</span>
              <Glyphicon glyph="chevron-down" style={{color:'#9e9e9e',float:'right'}} />
            </Dropdown.Toggle>
          </Dropdown>
        </div>

        <span className="mdw-heading-label mdw-med-indent">by:</span>
        {
        // <div className="mdw-heading-input">
        //   <DropdownButton id="breakdownDropdown" title={this.props.breakdown}>
        //     {
        //       this.props.breakdownConfig.breakdowns.map(breakdown => {
        //         return (
        //           <MenuItem key={breakdown.name} active={this.state.breakdown === breakdown.name}>
        //             {breakdown.name}
        //           </MenuItem>
        //         );
        //       })
        //     }
        //   </DropdownButton>
        // </div>
        }

        <div className="mdw-buttons">
          <OverlayTrigger trigger="click" placement="left" overlay={selectPopover} rootClose>
            <Button bsStyle="primary" className="mdw-btn">
              <Glyphicon glyph="ok" />
              <span style={{marginLeft:'4px'}}>Select</span>
            </Button>
          </OverlayTrigger>
        </div>

      </div>
    );
  }
}

ChartHeading.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default ChartHeading;
