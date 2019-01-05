import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Popover} from '../node/node_modules/react-bootstrap';
import PanelHeader from '../react/PanelHeader.jsx';
import HeaderLabel from '../react/HeaderLabel.jsx';
import HeaderDropdown from '../react/HeaderDropdown.jsx';
import HeaderButtons from '../react/HeaderButtons.jsx';
import HeaderPopButton from '../react/HeaderPopButton.jsx';

class ChartHeader extends Component {

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
      <PanelHeader>
        <HeaderLabel title={this.props.title + ' for the:'} />
        <HeaderDropdown id="timespan-dropdown"
          items={['Week','Month']}
          selected={this.state.timespan} />

        <HeaderLabel title="by:"  style={{marginLeft:'10px'}}/>
        <HeaderDropdown id="breakdown-dropdown"
          items={this.props.breakdownConfig.breakdowns.map(bd => bd.name)}
          selected={this.state.breakdown} />

        <HeaderButtons>
          <HeaderPopButton label="Select" glyph="ok"
            popover={selectPopover} />
        </HeaderButtons>

      </PanelHeader>
    );
  }
}

ChartHeader.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default ChartHeader;
