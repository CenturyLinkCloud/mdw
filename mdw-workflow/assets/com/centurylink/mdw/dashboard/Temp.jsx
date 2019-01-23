import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import PanelHeader from '../react/PanelHeader.jsx';
import {Bar} from '../node/node_modules/react-chartjs-2';

class Temp extends Component {

  constructor(...args) {
    super(...args);
  }

  render() {

    const data = {
      labels: ["Red", "Blue", "Yellow", "Green", "Purple", "Orange"],
      datasets: [{
          label: '# of Votes 1',
          data: [10, 19, 3, 5, 2, 3],
          backgroundColor: [
            'rgba(255, 99, 132, 0.2)',
            'rgba(255, 99, 132, 0.2)',
            'rgba(255, 99, 132, 0.2)',
            'rgba(255, 99, 132, 0.2)',
            'rgba(255, 99, 132, 0.2)',
            'rgba(255, 99, 132, 0.2)'
          ],
          borderWidth: 2,
          yAxisID: 'y-axis-bar'
        },
        {
          label: '# of Votes 2',
          data: [15, 19, 3, 5, 2, 3],
          backgroundColor: [
            'rgba(255, 159, 64, 0.2)',
            'rgba(255, 159, 64, 0.2)',
            'rgba(255, 159, 64, 0.2)',
            'rgba(255, 159, 64, 0.2)',
            'rgba(255, 159, 64, 0.2)',
            'rgba(255, 159, 64, 0.2)'
          ],
          borderWidth: 2,
          yAxisID: 'y-axis-bar'
        },
        {
          type: 'line',
          label: 'Other Data',
          fill: false,
          data: [100, 82, 35, 95, 32, 83],
          yAxisID: 'y-axis-line'
        }
      ]
    };

    const options = {
      scales: {
        yAxes: [
          {
            type: 'linear',
            stacked: true,
            display: true,
            position: 'left',
            id: 'y-axis-bar',
            gridLines: {
              display: false
            },
            labels: {
              show: true
            }
          },
          {
            type: 'linear',
            display: true,
            position: 'right',
            id: 'y-axis-line',
            gridLines: {
              display: false
            },
            labels: {
              show: true
            }
          }
        ],        
        xAxes: [{
          stacked: true,
          ticks: {
            beginAtZero: true
          }
        }]
  
      }
    };

    return (
      <div>
        <PanelHeader title="Temp" />
        <div className="mdw-section">
          <div>
            <Bar data={data} options={options} />
          </div>
        </div>
      </div>
    );
  }
}

Temp.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default Temp;
