import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import statuses from '../react/statuses';
import DashboardChart from './DashboardChart.jsx';

class OutboundRequests extends Component {

  constructor(...args) {
    super(...args);
    this.handleOverviewDataClick = this.handleOverviewDataClick.bind(this);
  }

  handleOverviewDataClick(breakdown, selection, filters) {
    var reqFilter = sessionStorage.getItem('workflow_requestFilter');
    reqFilter = reqFilter ? JSON.parse(reqFilter) : {};
    reqFilter.type = 'outboundRequests';
    if (breakdown === 'Throughput' || breakdown == 'Completion Time') {
      reqFilter.path = selection.id;
      reqFilter.status = filters.Status ? filters.Status : '[Active]';
      sessionStorage.setItem('workflow_requestFilter', JSON.stringify(reqFilter));
      location = this.context.hubRoot + '/#/workflow/requests';
      }
    else {
      // TODO request list status
    }
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
          instancesParam: 'statusCodes',
          colors: selected => selected.map(sel => statuses.request[sel.id].color)
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
        Status: Object.keys(statuses.request).map(status => status + ' - ' + statuses.request[status].message)
      }
    };

    return (
      <DashboardChart title="Outbound Requests"
        breakdownConfig={breakdownConfig}
        onOverviewDataClick={this.handleOverviewDataClick}
        list="#/workflow/requests" />
    );
  }
}

OutboundRequests.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default OutboundRequests;
