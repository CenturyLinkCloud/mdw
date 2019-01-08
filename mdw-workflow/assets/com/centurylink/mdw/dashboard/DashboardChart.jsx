import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import ChartHeader from './ChartHeader.jsx';

class DashboardChart extends Component {

  constructor(...args) {
    super(...args);
  }

  componentDidMount() {
  }

  render() {
    return (
      <div>
        <ChartHeader title={this.props.title}
          breakdownConfig={this.props.breakdownConfig}
          statuses={this.props.statuses}
          breakdown="Master"
          timespan="Week"
          list={this.props.list} />
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
