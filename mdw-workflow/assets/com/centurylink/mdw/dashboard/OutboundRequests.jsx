import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import statuses from '../react/statuses';
import DashboardChart from './DashboardChart.jsx';

class OutboundRequests extends Component {

  constructor(...args) {
    super(...args);
  }

  render() {
    const breakdownConfig = {
      breakdowns: [
        {
          name: 'Throughput',
          selectField: 'name',
          selectLabel: 'Request Paths',
          tops: '/Requests/tops?direction=out&by=throughput',
          data: '/Requests/breakdown?direction=out&by=throughput',
          instancesParam: 'requestPaths'
        },
        {
          name: 'Status',
          selectField: 'id',
          selectLabel: 'Statuses',
          tops: '/Requests/tops?direction=out&by=status',
          data: '/Requests/breakdown?direction=out&by=status',
          instancesParam: 'statusCodes'
        },
        {
          name: 'Completion Time',
          selectField: 'name',
          selectLabel: 'Request Paths',
          tops: '/Requests/tops?direction=out&by=completionTime',
          data: '/Requests/breakdown?direction=out&by=completionTime',
          instancesParam: 'requestPaths',
          summaryChart: 'bar',
          units: 'ms'
        },
        {
          name: 'Total Throughput',
          data: '/Requests/breakdown?direction=out&by=total'
        }
      ],
      filters: {
        Ending: new Date(),
        Status: '',
        Master: false,
        HealthCheck: false
      },
      filterOptions: {
        Status: statuses.request
      }
    };

    return (
      <DashboardChart title="Outbound Requests"
        breakdownConfig={breakdownConfig}
        list="#/workflow/requests" />
    );
  }
}

OutboundRequests.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default OutboundRequests;
