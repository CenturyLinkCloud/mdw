import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import PanelHeader from '../react/PanelHeader.jsx';
import HeaderLabel from '../react/HeaderLabel.jsx';
import HeaderDropdown from '../react/HeaderDropdown.jsx';
import HeaderButtons from '../react/HeaderButtons.jsx';
import HeaderButton from '../react/HeaderButton.jsx';
import HeaderPopButton from '../react/HeaderPopButton.jsx';
import SelectPop from './SelectPop.jsx';
import FilterPop from './FilterPop.jsx';

class ChartHeader extends Component {

  constructor(...args) {
    super(...args);
    this.getBreakdown = this.getBreakdown.bind(this);
    this.handleDropdownSelect = this.handleDropdownSelect.bind(this);
    this.handleFilterChange = this.handleFilterChange.bind(this);
  }

  // selected breakdown object from breakdownConfig
  getBreakdown() {
    return this.props.breakdownConfig.breakdowns.find(bd => bd.name === this.props.breakdown);
  }

  handleDropdownSelect(eventKey, dropdownId) { // eslint-disable-line no-unused-vars
    // if (dropdownId === 'timespan-dropdown') {
    //   this.setState({
    //     timespan: eventKey,
    //     breakdown: this.state.breakdown,
    //     tops: [],
    //     selected: [],
    //     filters: this.state.filters
    //   });
    // }
    // else if (dropdownId === 'breakdown-dropdown') {
    //   this.setState({
    //     timespan: this.state.timespan,
    //     breakdown: eventKey,
    //     tops: [],
    //     selected: [],
    //     filters: this.state.filters
    //   });
    // }
  }

  handleFilterChange(filters) { // eslint-disable-line no-unused-vars
    // this.setState({
    //   timespan: this.state.timespan,
    //   breakdown: this.state.breakdown,
    //   tops: [],
    //   selected: [],
    //   filters: filters
    // });
  }

  render() {
    const breakdown = this.getBreakdown();
    return (
      <PanelHeader>
        <HeaderLabel title={this.props.title + ' for the:'} />
        <HeaderDropdown id="timespan-dropdown"
          items={['Day','Week','Month']}
          selected={this.props.timespan}
          onSelect={this.handleDropdownSelect} />

        <HeaderLabel title="by:"  style={{marginLeft:'10px'}}/>
        <HeaderDropdown id="breakdown-dropdown"
          items={this.props.breakdownConfig.breakdowns.map(bd => bd.name)}
          selected={this.props.breakdown}
          onSelect={this.handleDropdownSelect} />

        <HeaderButtons>
          <HeaderPopButton label="Select" glyph="ok"
            popover={<SelectPop label={breakdown.selectLabel} tops={this.props.tops} selected={this.props.selected} />} />
          <HeaderPopButton label="Filters" glyph="filter" rootClose={false}
            popover={<FilterPop filters={this.props.filters} statuses={this.props.statuses} onChange={this.handleFilterChange} />} />
          <HeaderButton label="Export" glyph="download-alt" />
          <HeaderButton label="List" glyph="menu-hamburger"
            onClick={() => location = this.context.hubRoot + '/' + this.props.list} />
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
