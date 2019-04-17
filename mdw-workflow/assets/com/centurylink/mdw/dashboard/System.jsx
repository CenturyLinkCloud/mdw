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
          fill: true,
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
          tops: '/System/metrics/HeapMemory/summary',
          data: '/System/metrics/HeapMemory',
          websocketUrl: webSocketUrl,
          colors: ['#3366CC','#22AA99'],
          fill: true,
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
        },
        {
          name: 'Thread Pool',
          summaryTitle: 'Rolling Average',
          tops: '/System/metrics/ThreadPool/summary',
          data: '/System/metrics/ThreadPool',
          websocketUrl: webSocketUrl,
          colors: ['#3366CC','#DC3912'],
          fill: [true,false,false],
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
        },
        {
          name: 'DB Connections',
          summaryTitle: 'Rolling Average',
          tops: '/System/metrics/DbConnections/summary',
          data: '/System/metrics/DbConnections',
          websocketUrl: webSocketUrl,
          colors: ['#3366CC','#22AA99'],
          fill: [true,true,false],
          stacked: [true,true,false],
          chartOptions: { 
            animation: false,
            scales: {
              yAxes: [{
                ticks: {
                  beginAtZero: true,
                  max: undefined,
                  callback: function(value) {if (value % 1 === 0) return value;}
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
