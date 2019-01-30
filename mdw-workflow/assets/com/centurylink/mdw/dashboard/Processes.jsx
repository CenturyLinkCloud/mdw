import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import statuses from '../react/statuses';
import constants from '../react/constants';
import DashboardChart from './DashboardChart.jsx';

class Processes extends Component {

  constructor(...args) {
    super(...args);
    this.handleOverviewDataClick = this.handleOverviewDataClick.bind(this);
    this.handleMainDataClick = this.handleMainDataClick.bind(this); 
  }

  handleOverviewDataClick(breakdown, selection, filters) {
    var procFilter = sessionStorage.getItem('processFilter');
    procFilter = procFilter ? JSON.parse(procFilter) : {};
    if (breakdown === 'Throughput' || breakdown == 'Completion Time') {
      procFilter.processId = selection.id;
      var procSpec = selection.name;
      sessionStorage.setItem('processSpec', procSpec);
      procFilter.status = filters.Status ? filters.Status : '[Any]';
    }
    else {
      sessionStorage.removeItem('processSpec');
      if (breakdown === 'Status') {
        procFilter.status = selection.name;
      }
    }
    procFilter.master = filters.Master ? filters.Master : false;
    const start = filters.Starting;
    procFilter.startDate = start.getFullYear().toString() + '-' + constants.months[start.getMonth()] + '-' + start.getDate();
    sessionStorage.setItem('processFilter', JSON.stringify(procFilter));
    location = this.context.hubRoot + '/#/workflow/processes';
  }

  handleMainDataClick(breakdown, selection, filters) { // eslint-disable-line no-unused-vars

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
          instancesParam: 'statuses',
          colors: selected => selected.map(sel => statuses.process[sel.name].color)
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
        Status: Object.keys(statuses.process)
      }
    };

    return (
      <DashboardChart title="Processes"
        breakdownConfig={breakdownConfig}
        onOverviewDataClick={this.handleOverviewDataClick}
        list="#/workflow/processes" />
    );
  }
}

Processes.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default Processes;
