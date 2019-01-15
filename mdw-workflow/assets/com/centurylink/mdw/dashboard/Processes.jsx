import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import DashboardChart from './DashboardChart.jsx';

class Processes extends Component {

  constructor(...args) {
    super(...args);
  }

  componentDidMount() {
  }

  render() {

    const statuses = [
      'Pending',
      'In Progress',
      'Failed',
      'Completed',
      'Canceled',
      'Waiting'
    ];

    const breakdownConfig = {
      breakdowns: [
        {
          name: 'Throughput',
          selectField: 'id',
          selectLabel: 'Processes',
          tops: '/Processes/tops?by=throughput',  // returns top assets
          data: '/Processes/breakdown?by=throughput',
          instancesParam: 'processIds'  // service param for selected instances
        },
        {
          name: 'Status',
          selectField: 'status',
          selectLabel: 'Statuses',
          tops: '/Processes/tops?by=status',
          data: '/Processes/breakdown?by=status',
          instancesParam: 'statuses'
        },
        {
          name: 'Completion Time',
          selectField: 'id',
          selectLabel: 'Processes',
          tops: '/Processes/tops?by=completionTime',
          data: '/Processes/breakdown?by=completionTime',
          instancesParam: 'processIds'
        },
        {
          name: 'Total',
          data: '/Processes/breakdown?by=total'
        }
      ]
    };

    return (
      <DashboardChart title="Processes"
        breakdownConfig={breakdownConfig}
        list="#/workflow/processes"
        statuses={statuses} />
    );
  }
}

Processes.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default Processes;
