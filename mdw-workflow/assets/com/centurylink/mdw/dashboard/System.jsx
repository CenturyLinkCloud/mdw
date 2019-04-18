import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import DashboardChart from './DashboardChart.jsx';

class System extends Component {

  constructor(...args) {
    super(...args);
  }

  getChartOptions(max) {
    return {
      animation: false,
      scales: {
        xAxes: [{
          scaleLabel: {
            display: true,
            labelString: 'Server time: ${hostname}'
          }
        }],
        yAxes: [{
          ticks: {
            beginAtZero: true,
            max: max,
            callback: function(value) {if (value % 1 === 0) return value;}
          }
        }]
      }
    };
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
          chartOptions: this.getChartOptions(100)
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
          chartOptions: this.getChartOptions()
        },
        {
          name: 'Thread Pool',
          summaryTitle: 'Rolling Average',
          tops: '/System/metrics/ThreadPool/summary',
          data: '/System/metrics/ThreadPool',
          websocketUrl: webSocketUrl,
          colors: ['#3366CC','#DC3912'],
          fill: [true,false,false],
          chartOptions: this.getChartOptions()
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
          chartOptions: this.getChartOptions()
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
