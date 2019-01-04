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

    const breakdownConfig = {
      instanceCounts: '/services/Processes/instanceCounts',
      breakdowns: [
        {
          name: 'Master',
          selectField: 'id',
          selectLabel: 'Master Processes',
          throughput: '/services/Processes/topThroughput?master=true', // returns top asset InstanceCounts
          instancesParam: 'processIds'  // service parameter for selected instances
        },
        {
          name: 'Process',
          selectField: 'id',
          selectLabel: 'Processes',
          throughput: '/services/Processes/topThroughput',
          instancesParam: 'processIds'
        },
        {
          name: 'Status',
          selectField: 'status',
          selectLabel: 'Statuses',
          throughput: ['Pending', 'In Progress', 'Failed', 'Completed', 'Canceled', 'Waiting'],
          instancesParam: 'statuses'
        }
      ]
    };

    return (
      <DashboardChart title="Processes"
        breakdownConfig={breakdownConfig} list="#/workflow/processes" />
    );
  }
}

Processes.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default Processes;
