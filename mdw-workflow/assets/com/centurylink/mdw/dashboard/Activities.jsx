import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import statuses from '../react/statuses';
import constants from '../react/constants';
import DashboardChart from './DashboardChart.jsx';

class Activities extends Component {

  constructor(...args) {
    super(...args);
    this.handleOverviewDataClick = this.handleOverviewDataClick.bind(this);
  }

  // TODO populate activity name filter
  handleOverviewDataClick(breakdown, selection, filters) {
    const activityFilter = { descending: true };
    if (breakdown === 'Status') {
      activityFilter.status = selection.name;
      delete activityFilter.activity;
      sessionStorage.removeItem('activitySpec');
    }
    else {
      activityFilter.status = filters.Status;
      if (!activityFilter.status) {
        if (breakdown === 'Stuck Count') {
          activityFilter.status = '[Stuck]';
        }
        else if (breakdown === 'Completion Time'){
          activityFilter.status = 'Completed';
        }
        else {
          activityFilter.status = '[Any]';
        }
      }
      activityFilter.activity = encodeURIComponent(selection.id);
      sessionStorage.setItem('activitySpec', selection.processName + ' v' +
          selection.version + ' ' + selection.activityName + ' (' + selection.definitionId + ')');
    }
    const start = filters.Starting;
    activityFilter.startDate = start.getFullYear().toString() + '-' + constants.months[start.getMonth()] + '-' + start.getDate();
    sessionStorage.setItem('activityFilter', JSON.stringify(activityFilter));
    location = this.context.hubRoot + '/#/workflow/activities';
  }

  render() {

    const breakdownConfig = {
      breakdowns: [
        {
          name: 'Stuck Count',
          selectField: 'id',
          selectLabel: 'Activities',
          tops: '/Activities/tops?by=throughput',
          data: '/Activities/breakdown?by=throughput',
          instancesParam: 'activityIds'
        },
        {
          name: 'Status',
          selectField: 'name',
          selectLabel: 'Statuses',
          tops: '/Activities/tops?by=status',
          data: '/Activities/breakdown?by=status',
          instancesParam: 'statuses',
          colors: selected => selected.map(sel => statuses.activity[sel.name].color)
        },
        {
          name: 'Completion Time',
          selectField: 'id',
          selectLabel: 'Activities',
          tops: '/Activities/tops?by=completionTime',
          data: '/Activities/breakdown?by=completionTime',
          instancesParam: 'activityIds',
          summaryChart: 'bar',
          summaryTitle: 'Completed Activities',
          summaryChartOptions: {scales: {yAxes: [{ticks: {beginAtZero: true}}]}},
          units: filters => filters['Completion Times In']
        }
      ],
      filters: {
        Ending: new Date(),
        Status: '',
        'Exclude Long-Running': false,
        'Completion Times In': 'Seconds'
      },
      filterOptions: {
        Status: Object.keys(statuses.activity),
        'Completion Times In': ['Milliseconds', 'Seconds', 'Minutes', 'Hours', 'Days']
      }
    };

    return (
      <DashboardChart title="Activities"
        breakdownConfig={breakdownConfig}
        onOverviewDataClick={this.handleOverviewDataClick}
        list="#/workflow/activities" />
    );
  }
}

Activities.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default Activities;
