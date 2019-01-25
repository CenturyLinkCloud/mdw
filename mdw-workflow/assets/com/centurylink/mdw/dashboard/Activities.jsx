import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import statuses from '../react/statuses';
import DashboardChart from './DashboardChart.jsx';

class Activities extends Component {

  constructor(...args) {
    super(...args);
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
          instancesParam: 'statuses'
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
        Status: statuses.activity
      }
    };

    return (
      <DashboardChart title="Stuck Activities"
        breakdownConfig={breakdownConfig}
        list="#/workflow/activities" />
    );
  }
}

Activities.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default Activities;
