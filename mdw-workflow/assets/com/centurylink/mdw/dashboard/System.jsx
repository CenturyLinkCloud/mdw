import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import DashboardChart from './DashboardChart.jsx';

class System extends Component {

  constructor(...args) {
    super(...args);
  }

  render() {
    
    const breakdownConfig = {
      breakdowns: [
        {
          name: 'CPU',
          selectField: 'name',
          selectLabel: 'CPU Usage',
          tops: '/System/metrics/CPU/summary',
          data: '/System/metrics/CPU',
          colors: ['#3366CC','#FF9900','#109618']
        }
      ]
    };

    return (
      <DashboardChart title="System"
        breakdownConfig={breakdownConfig} />
    );
  }
}

System.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default System;
