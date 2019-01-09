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
          name: 'Master',
          selectField: 'id',
          selectLabel: 'Master Processes',
          throughput: '/Processes/topThroughput?master=true', // returns top asset InstanceCounts
          data: '/Processes/instanceCounts',
          instancesParam: 'processIds'  // service parameter for selected instances
        },
        {
          name: 'Process',
          selectField: 'id',
          selectLabel: 'Processes',
          throughput: '/Processes/topThroughput',
          data: '/Processes/instanceCounts',
          instancesParam: 'processIds'
        },
        {
          name: 'Status',
          selectField: 'status',
          selectLabel: 'Statuses',
          throughput: statuses,
          data: '/Processes/instanceCounts',
          instancesParam: 'statuses'
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
