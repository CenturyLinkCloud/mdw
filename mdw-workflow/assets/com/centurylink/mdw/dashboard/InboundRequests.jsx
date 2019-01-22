import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
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
          selectField: 'path',
          selectLabel: 'Request Paths',
          tops: '/Requests/tops?direction=inbound&by=throughput',
          data: '/Requests/breakdown?direction=inbound&by=throughput',
          instancesParam: 'requestPaths'
        },
        {
          name: 'Status',
          selectField: 'status',
          selectLabel: 'Statuses',
          tops: '/Requests/tops?direction=inbound&by=status',
          data: '/Requests/breakdown?direction=inbound&by=status',
           instancesParam: 'statusCodes'
        },
        {
          name: 'Completion Time',
          selectField: 'path',
          selectLabel: 'Request Paths',
          tops: '/Requests/tops?direction=inbound&by=completionTime',
          data: '/Requests/breakdown?direction=inbound&by=completionTime',
          instancesParam: 'requestPaths',
          summaryChart: 'bar',
          units: 'ms'
        },
        {
          name: 'Total Throughput',
          data: '/Requests/breakdown?direction=inbound&by=total'
        }
      ],
      filters: {
        Ending: new Date(),
        Status: '',
        Master: false,
        HealthCheck: false
      },
      filterOptions: {
        Status: [
          '200 - OK',
          '201 - Created',
          '202 - Accepted',
          '400 - Bad Request',
          '401 - Unauthorized',
          '403 - Forbidden',
          '404 - Not Found',
          '405 - Method Not Allowed',
          '409 - Conflict',
          '500 - Internal Server Error',
          '501 - Not Implemented',
          '502 - Bad Gateway',
          '503 - Service Unavailable'
        ]
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
