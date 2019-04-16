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
          name: 'CPU Usage',
          units: 'Percent',
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
        },
        {
          name: 'Heap Memory',
          units: 'MB',
          summaryTitle: 'Rolling Average',
          tops: '/System/metrics/Memory/summary',
          data: '/System/metrics/Memory',
          websocketUrl: webSocketUrl,
          colors: ['#3366CC','#22AA99'],
          fill: 'origin',
          chartOptions: { 
            animation: false,
            scales: {
              yAxes: [{
                ticks: {
                  beginAtZero: true,
                  max: undefined // must unset options from above
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
