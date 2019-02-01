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
    var activityFilter = sessionStorage.getItem('activityFilter');
    activityFilter = activityFilter ? JSON.parse(activityFilter) : {};
    if (breakdown === 'Status') {
      activityFilter.status = selection.name;
    }
    else if (filters.Status) {
      activityFilter.status = filters.Status;
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
          name: 'Throughput',
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
          name: 'Total Throughput',
          data: '/Activities/breakdown?by=total'
        }
      ],
      filters: {
        Ending: new Date(),
        Status: ''
      },
      filterOptions: {
        Status: Object.keys(statuses.activity)
      }
    };

    return (
      <DashboardChart title="Stuck Activities"
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
