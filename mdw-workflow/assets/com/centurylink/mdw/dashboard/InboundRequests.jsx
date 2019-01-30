import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import statuses from '../react/statuses';
import DashboardChart from './DashboardChart.jsx';

class InboundRequests extends Component {

  constructor(...args) {
    super(...args);
    this.handleOverviewDataClick = this.handleOverviewDataClick.bind(this);
  }

  handleOverviewDataClick(breakdown, selection, filters) {
    var reqFilter = sessionStorage.getItem('workflow_requestFilter');
    reqFilter = reqFilter ? JSON.parse(reqFilter) : {};
    reqFilter.type = 'inboundRequests';
    if (breakdown === 'Throughput' || breakdown == 'Completion Time') {
      reqFilter.path = selection.id;
      reqFilter.status = filters.Status ? filters.Status : '[Active]';
      sessionStorage.setItem('workflow_requestFilter', JSON.stringify(reqFilter));
      location = this.context.hubRoot + '/#/workflow/requests';
      }
    else {
      // TODO request list status filter
    }
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
          instancesParam: 'statusCodes',
          colors: selected => selected.map(sel => statuses.request[sel.id].color)
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
        Status: Object.keys(statuses.request).map(status => status + ' - ' + statuses.request[status].message)
      }
    };

    return (
      <DashboardChart title="Inbound Requests"
        breakdownConfig={breakdownConfig}
        onOverviewDataClick={this.handleOverviewDataClick}
        list="#/workflow/requests" />
    );
  }
}

InboundRequests.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default InboundRequests;
