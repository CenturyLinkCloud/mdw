import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import statuses from '../react/statuses';
import DashboardChart from './DashboardChart.jsx';

class Tasks extends Component {

  constructor(...args) {
    super(...args);
  }

  render() {

    const breakdownConfig = {
       breakdowns: [
         {
           name: 'Throughput',
           selectField: 'id',
           selectLabel: 'Tasks',
           tops: '/Tasks/tops?by=throughput',
           data: '/Tasks/breakdown?by=throughput',
           instancesParam: 'taskIds'
         },
         {
           name: 'Status',
           selectField: 'name',
           selectLabel: 'Statuses',
           tops: '/Tasks/tops?by=status',
           data: '/Tasks/breakdown?by=status',
           instancesParam: 'statuses',
           colors: selected => selected.map(sel => statuses.task[sel.name].color)
         },
         {
          name: 'Workgroup',
          selectField: 'name',
          selectLabel: 'Workgroups',
          tops: '/Tasks/tops?by=workgroup',
          data: '/Tasks/breakdown?by=workgroup',
          instancesParam: 'workgroups'
        },
        {
          name: 'Assignee',
          selectField: 'id',
          selectLabel: 'Assignees',
          tops: '/Tasks/tops?by=assignee',
          data: '/Tasks/breakdown?by=assignee',
          instancesParam: 'assignees'
        },
        {
          name: 'Completion Time',
          selectField: 'id',
          selectLabel: 'Tasks',
          tops: '/Tasks/tops?by=completionTime',
          data: '/Tasks/breakdown?by=completionTime',
          instancesParam: 'taskIds',
          summaryChart: 'bar',
          units: 'sec'
        },
        {
           name: 'Total Throughput',
           data: '/Tasks/breakdown?by=total'
         }
       ],
       filters: {
         Ending: new Date(),
         Status: '',
         Master: false
       },
       filterOptions: {
         Status: statuses.task
       }
     };
 
     return (
       <DashboardChart title="Tasks"
         breakdownConfig={breakdownConfig}
         list="#/tasks" />
     );
   }
 }

Tasks.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default Tasks;
