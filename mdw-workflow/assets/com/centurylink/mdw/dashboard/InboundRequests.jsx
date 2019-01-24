import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import statuses from './statuses';
import DashboardChart from './DashboardChart.jsx';

class InboundRequests extends Component {

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
          tops: '/Requests/tops?direction=in&by=throughput',
          data: '/Requests/breakdown?direction=in&by=throughput',
          instancesParam: 'requestPaths'
        },
        {
          name: 'Status',
          selectField: 'id',
          selectLabel: 'Statuses',
          tops: '/Requests/tops?direction=in&by=status',
          data: '/Requests/breakdown?direction=in&by=status',
          instancesParam: 'statusCodes'
        },
        {
          name: 'Completion Time',
          selectField: 'name',
          selectLabel: 'Request Paths',
          tops: '/Requests/tops?direction=in&by=completionTime',
          data: '/Requests/breakdown?direction=in&by=completionTime',
          instancesParam: 'requestPaths',
          summaryChart: 'bar',
          units: 'ms'
        },
        {
          name: 'Total Throughput',
          data: '/Requests/breakdown?direction=in&by=total'
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
      <DashboardChart title="Inbound Requests"
        breakdownConfig={breakdownConfig}
        list="#/workflow/requests" />
    );
  }
}

InboundRequests.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default InboundRequests;
