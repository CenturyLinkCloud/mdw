import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import ChartHeader from './ChartHeader.jsx';

class DashboardChart extends Component {

  constructor(...args) {
    super(...args);
    this.state = {
      timespan: 'Week',
      breakdown: this.props.breakdownConfig.breakdowns[0].name,
      tops: [],
      selected: [],
      filters: {ending: new Date(), status: ''}
    };
    this.getBreakdown = this.getBreakdown.bind(this);
    this.getStart = this.getStart.bind(this);
    this.setTops = this.setTops.bind(this);
    this.retrieveTops = this.retrieveTops.bind(this);

    // constants (TODO: parameterize?)
    this.initialSelect = 5;
    this.maxTops = 50;
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

  componentDidMount() {
    this.retrieveTops().then(tops => {
      this.setTops(tops, tops.slice(0, this.initialSelect));
    });
  }

  // updates state and then returns a promise
  retrieveTops() {
    return new Promise(resolve => {
      const breakdown = this.getBreakdown();
      if (breakdown) {
        var topsUrl = this.context.serviceRoot + breakdown.throughput +
              '&max=' + this.maxTops + '&startDt=' + this.getStart();
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
          breakdown="Master"
          timespan="Week"
          list={this.props.list}
          tops={this.state.tops}
          selected={this.state.selected}
          filters={this.state.filters} />
        <div className="mdw-section">
            HERE IS A CHART
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
