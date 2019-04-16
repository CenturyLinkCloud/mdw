import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import DashboardChart from './DashboardChart.jsx';

class System extends Component {

  constructor(...args) {
    super(...args);
  }

  render() {
    
    let webSocketUrl = $mdwWebSocketUrl;
    if (webSocketUrl === '${mdwWebSocketUrl}') {
      webSocketUrl = null;
    }

    const breakdownConfig = {
      breakdowns: [
        {
          name: 'CPU',
          units: 'Usage %',
          summaryTitle: 'Rolling Average',
          tops: '/System/metrics/CPU/summary',
          data: '/System/metrics/CPU',
          websocketUrl: webSocketUrl,
          colors: ['#3366CC','#FF9900'],
          fill: 'origin',
          stacked: true,
          chartOptions: { 
            animation: false,
            scales: {
              yAxes: [{
                ticks: {
                  beginAtZero: true,
                  max: 100
                }
              }]
            }
          }
        }
      ]
    };

    return (
      <DashboardChart
        breakdownConfig={breakdownConfig} />
    );
  }
}

System.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default System;
