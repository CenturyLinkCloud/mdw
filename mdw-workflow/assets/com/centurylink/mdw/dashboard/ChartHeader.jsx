import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {DropdownButton, MenuItem} from '../node/node_modules/react-bootstrap';

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
    return (
      <div className="panel-heading mdw-heading">
        <div className="mdw-heading-label">{this.props.title} for the:</div>
        <div className="mdw-heading-input">
          <DropdownButton id="timespanDropdown" title={this.state.timespan}>
            <MenuItem active={this.state.timespan === 'Week'}>Week</MenuItem>
            <MenuItem active={this.state.timespan === 'Month'}>Month</MenuItem>
          </DropdownButton>
        </div>
        <span className="mdw-heading-label mdw-med-indent">by:</span>
        <div className="mdw-heading-input">
          <DropdownButton id="breakdownDropdown" title={this.props.breakdown}>
            {
              this.props.breakdownConfig.breakdowns.map(breakdown => {
                return (
                  <MenuItem key={breakdown.name} active={this.state.breakdown === breakdown.name}>
                    {breakdown.name}
                  </MenuItem>
                );
              })
            }
          </DropdownButton>
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
