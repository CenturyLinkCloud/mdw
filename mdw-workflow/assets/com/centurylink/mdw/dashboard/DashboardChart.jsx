import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import ChartHeader from './ChartHeader.jsx';

class DashboardChart extends Component {

  constructor(...args) {
    super(...args);
    // constants (TODO: parameterize?)
    this.initialSelect = 5;
    this.maxTops = 50;
    this.defaultTimespan = 'Week';

    this.state = {
      timespan: this.defaultTimespan,
      breakdown: this.props.breakdownConfig.breakdowns[0].name,
      tops: [],
      selected: [],
      filters: {ending: this.getDefaultEnd(), status: ''}
    };
    this.getBreakdown = this.getBreakdown.bind(this);
    this.getDefaultEnd = this.getDefaultEnd.bind(this);
    this.getStart = this.getStart.bind(this);
    this.setTops = this.setTops.bind(this);
    this.setSelected = this.setSelected.bind(this);
    this.handleTimespanChange = this.handleTimespanChange.bind(this);
    this.handleBreakdownChange = this.handleBreakdownChange.bind(this);
    this.handleTopSelect = this.handleTopSelect.bind(this);
    this.handleSelectCancel = this.handleSelectCancel.bind(this);
    this.handleSelectApply = this.handleSelectApply.bind(this);
    this.handleFilterChange = this.handleFilterChange.bind(this);
    this.handleFilterReset = this.handleFilterReset.bind(this);
    this.retrieveTops = this.retrieveTops.bind(this);
  }

  // selected breakdown object from breakdownConfig
  getBreakdown() {
    return this.props.breakdownConfig.breakdowns.find(bd => bd.name === this.state.breakdown);
  }

  getDefaultEnd() {
    var timespan = this.defaultTimespan;
    if (this.state && this.state.timespan) {
      timespan = this.state.timespan;
    }
    var end = new Date();
    end.setMinutes(0);
    end.setSeconds(0);
    end.setMilliseconds(0);
    if (timespan === 'Week' || timespan === 'Month') {
      end.setHours(0);
    }
    return end;
  }

  getStart() {
    var spanMs = 24 * 3600 * 1000; // one day
    if (this.state.timespan === 'Week') {
      spanMs = spanMs * 6;
    }
    else if (this.state.timespan === 'Month') {
      spanMs = spanMs * 29;
    }
    return new Date(this.state.filters.ending.getTime() - spanMs);
  }

  setTops(tops, selected) {
    return new Promise(resolve => {
      this.setState({
        timespan: this.state.timespan,
        breakdown: this.state.breakdown,
        tops: tops,
        selected: selected ? selected : this.state.selected,
        filters: this.state.filters
      }, () => {
        resolve();
      });
    });
  }

  setSelected(selected) {
    return new Promise(resolve => {
      this.setState({
        timespan: this.state.timespan,
        breakdown: this.state.breakdown,
        tops: this.state.tops,
        selected: selected,
        filters: this.state.filters
      }, () => {
        resolve();
      });
    });
  }

  handleTimespanChange(timespan) {
    this.setState({
      timespan: timespan,
      breakdown: this.state.breakdown,
      tops: this.state.tops,
      selected: this.state.selected,
      filters: this.state.filters
    });
  }

  handleBreakdownChange(breakdown) {
    this.setState({
      timespan: this.state.timespan,
      breakdown: breakdown,
      tops: this.state.tops,
      selected: this.state.selected,
      filters: this.state.filters
    });
  }

  // does not redraw charts
  handleTopSelect(top, isSelected) {
    var selected = this.state.selected.slice(0);
    if (isSelected) {
      selected.push(top);
    }
    else {
      const idx = selected.findIndex(t => t.id === top.id);
      selected.splice(idx, 1);
    }
    this.setSelected(selected);
  }

  handleSelectCancel() {
    if (this.previousSelected) {
      this.setSelected(this.previousSelected);
    }
  }

  handleSelectApply() {
    this.previousSelected = this.state.selected;
  }

  handleFilterChange(filters) {
    this.setState({
      timespan: this.state.timespan,
      breakdown: this.state.breakdown,
      tops: this.state.tops,
      selected: this.state.selected,
      filters: filters
    });
  }

  handleFilterReset() {
    this.handleFilterChange({
      ending: this.getDefaultEnd(),
      status: ''
    });
  }

  componentDidMount() {
    this.retrieveTops()
    .then(tops => {
      this.setTops(tops, tops.slice(0, this.initialSelect));
    })
    .then(() => {
      this.previousSelected = this.state.selected;
    });
  }

  redraw() {

  }

  // updates state and then returns a promise
  retrieveTops() {
    return new Promise(resolve => {
      const breakdown = this.getBreakdown();
      if (breakdown) {
        var topsUrl = this.context.serviceRoot + breakdown.throughput;
        topsUrl += (breakdown.throughput.indexOf('?') >= 0 ? '&' : '?');
        topsUrl += 'max=' + this.maxTops + '&startDt=' + this.getStart().toISOString();
        if (this.state.filters.status) {
          topsUrl += '&status=' + this.state.filters.status;
        }
        fetch(new Request(topsUrl, {
          method: 'GET',
          headers: { Accept: 'application/json'},
          credentials: 'same-origin'
        }))
        .then(response => {
          return response.json();
        })
        .then(tops => {
          resolve(tops);
        });
      }
      else {
        resolve([]);
      }
    });
  }

  render() {
    return (
      <div>
        <ChartHeader title={this.props.title}
          breakdownConfig={this.props.breakdownConfig}
          statuses={this.props.statuses}
          breakdown={this.state.breakdown}
          timespan={this.state.timespan}
          list={this.props.list}
          tops={this.state.tops}
          selected={this.state.selected}
          filters={this.state.filters}
          onTimespanChange={this.handleTimespanChange}
          onBreakdownChange={this.handleBreakdownChange}
          onSelect={this.handleTopSelect}
          onSelectCancel={this.handleSelectCancel}
          onSelectApply={this.handleSelectApply}
          onFilterChange={this.handleFilterChange}
          onFilterReset={this.handleFilterReset} />
        <div className="mdw-section">
            CHART GOES HERE
        </div>
      </div>
    );
  }
}

DashboardChart.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default DashboardChart;
