import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import statuses from './statuses';
import DashboardChart from './DashboardChart.jsx';

class Processes extends Component {

  constructor(...args) {
    super(...args);
  }

  render() {

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
          selectField: 'name',
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
          instancesParam: 'processIds',
          summaryChart: 'bar',
          units: 'ms'
        },
        {
          name: 'Total Throughput',
          data: '/Processes/breakdown?by=total'
        }
      ],
      filters: {
        Ending: new Date(),
        Status: '',
        Master: false
      },
      filterOptions: {
        Status: statuses.process
      }
    };

    return (
      <DashboardChart title="Processes"
        breakdownConfig={breakdownConfig}
        list="#/workflow/processes" />
    );
  }
}

Processes.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default Processes;
