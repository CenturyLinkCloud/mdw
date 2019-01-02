import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import Heading from './Heading.jsx';
import {Line} from '../node/node_modules/react-chartjs-2';

class Temp extends Component {

  constructor(...args) {
    super(...args);
  }

  componentDidMount() {
  }

  render() {

    const chartData = {
      labels: ['January', 'February', 'March', 'April', 'May', 'June', 'July'],
      datasets: [
        {
          label: 'Test dataset',
          fill: false,
          lineTension: 0.1,
          borderCapStyle: 'butt',
          pointBorderWidth: 1,
          pointHoverRadius: 5,
          pointRadius: 1,
          pointHitRadius: 10,
          data: [65, 59, 80, 81, 56, 55, 40]
        }
      ]
    };

    return (
      <div>
        <Heading title="Temp" />
        <div className="mdw-section">
          <div>
            <Line data={chartData} />
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
