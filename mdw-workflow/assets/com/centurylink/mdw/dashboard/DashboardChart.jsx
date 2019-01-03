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
          breakdown="Master"
          timespan="Week" />
        <div className="mdw-section">
            HERE'S A CHART
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
