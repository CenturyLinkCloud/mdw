import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import PanelHeader from '../react/PanelHeader.jsx';
import HeaderLabel from '../react/HeaderLabel.jsx';
import HeaderDropdown from '../react/HeaderDropdown.jsx';
import HeaderButtons from '../react/HeaderButtons.jsx';
import HeaderPopButton from '../react/HeaderPopButton.jsx';
import SelectPop from './SelectPop.jsx';

class ChartHeader extends Component {

  constructor(...args) {
    super(...args);
    this.state = {
      timespan: 'Week',
      breakdown: this.props.breakdownConfig.breakdowns[0].name,
      tops: [],
      selected: []
    };
    this.getBreakdown = this.getBreakdown.bind(this);
    this.handleDropdownSelect = this.handleDropdownSelect.bind(this);
  }

  // selected breakdown object from breakdownConfig
  getBreakdown() {
    return this.props.breakdownConfig.breakdowns.find(bd => bd.name === this.state.breakdown);
  }

  // returns an ISO string
  getStart() {
    // TODO handle end filter
    var spanMs = 24 * 3600 * 1000; // one day
    if (this.state.timespan === 'Week') {
      spanMs = spanMs * 6;
    }
    else if (this.state.timespan === 'Month') {
      spanMs = spanMs * 29;
    }
    var startDate = new Date(Date.now() - spanMs);
    startDate.setMinutes(0);
    startDate.setSeconds(0);
    startDate.setMilliseconds(0);
    if (this.state.timespan === 'Week' || this.state.timespan === 'Month') {
      startDate.setHours(0);
    }
    return startDate.toISOString();
  }

  componentDidMount() {
    const breakdown = this.getBreakdown();
    if (breakdown) {
      var topsUrl = this.context.serviceRoot + breakdown.throughput + '&max=50&startDt=' + this.getStart();
      fetch(new Request(topsUrl, {
        method: 'GET',
        headers: { Accept: 'application/json'},
        credentials: 'same-origin'
      }))
      .then(response => {
        return response.json();
      })
      .then(topsJson => {
        // todo: unselect no longer present
        this.setState({
          timespan: this.state.timespan,
          breakdown: this.state.breakdown,
          tops: topsJson,
          selected: this.state.selected
        });
      });
    }
  }

  handleDropdownSelect(eventKey, dropdownId) {
    if (dropdownId === 'timespan-dropdown') {
      this.setState({
        timespan: eventKey,
        breakdown: this.state.breakdown,
        tops: [],
        selected: []
      });
    }
    else if (dropdownId === 'breakdown-dropdown') {
      this.setState({
        timespan: this.state.timespan,
        breakdown: eventKey,
        tops: [],
        selected: []
      });
    }
    // TODO: render chart
  }

  render() {
    const breakdown = this.getBreakdown();

    return (
      <PanelHeader>
        <HeaderLabel title={this.props.title + ' for the:'} />
        <HeaderDropdown id="timespan-dropdown"
          items={['Week','Month']}
          selected={this.state.timespan}
          onSelect={this.handleDropdownSelect} />

        <HeaderLabel title="by:"  style={{marginLeft:'10px'}}/>
        <HeaderDropdown id="breakdown-dropdown"
          items={this.props.breakdownConfig.breakdowns.map(bd => bd.name)}
          selected={this.state.breakdown}
          onSelect={this.handleDropdownSelect} />

        <HeaderButtons>
          <HeaderPopButton label="Select" glyph="ok"
            popover={<SelectPop label={breakdown.selectLabel} tops={this.state.tops} selected={this.state.selected} />} />
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
