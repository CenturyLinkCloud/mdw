import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import Select from '../node/node_modules/react-select';
import '../node/node_modules/style-loader!../react/react-select.css';
import Workflow from '../react/Workflow.jsx';
import UserDate from '../react/UserDate.jsx';

class Task extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = {workgroupOptions: []};
    this.handleClick = this.handleClick.bind(this);
    this.handleWorkgroupSelectChange = this.handleWorkgroupSelectChange.bind(this);
  }  

  componentDidMount() {
    fetch(new Request('/mdw/services/Workgroups', {
      method: 'GET',
      headers: { Accept: 'application/json'}
    }))
    .then(response => {
      return response.json();
    })
    .then(json => {
      this.setState({
        workgroupOptions: json.workgroups.map(group => {
          return { value: group.name, label: group.name };
        }) 
      });
    });    
  }
  
  handleClick(event) {
    if (event.currentTarget.type === 'button') {
      if (event.currentTarget.value === 'save') {
        console.log('save task');
      }
    }
  }
  
  handleWorkgroupSelectChange(values) {
    var groups = [];
    values.split(',').forEach(val => {
      groups.push(val);
    });
    this.props.updateTask({workgroups: groups});
  }
  
  render() {
    var task = this.props.task;
    return (
      <div>
        <div className="mdw-flex-item">
          <div className="mdw-flex-item-left">
            <div>
              <div>
                <div className="mdw-item-group">
                  {task.masterRequestId && 
                    <span>
                      <label>Master request:</label>
                      <a href={'#/workflow/masterRequests/' + task.masterRequestId} 
                          className="mdw-link">{task.masterRequestId}
                      </a>
                    </span>
                  }
                </div>
                <div className="mdw-item-group">
                  <UserDate label="Created" date={task.start} />
                  {task.due &&
                    <span>{',   '}
                      <UserDate label="Due" date={task.due} alert={true} 
                        editable={task.editable} notLabel="No Due Date" />
                    </span>
                  }
                  {task.end &&
                    <UserDate label={task.status} date={task.end} />
                  }
                </div>
                {task.workgroups &&
                  <div className="mdw-item-group">
                    <label>Workgroups:</label>
                    <div className="mdw-item-select">
                      <Select multi simpleValue value={task.workgroups} 
                        placeholder="Select workgroup(s)" 
                        options={this.state.workgroupOptions} 
                        onChange={this.handleWorkgroupSelectChange} />
                    </div>
                  </div>
                }
                {task.secondaryOwnerType == 'TASK_INSTANCE' &&
                  <div className="mdw-item-group">
                    Master Task: 
                    <a href={'#/tasks/' + task.secondaryOwnerId}>{task.secondaryOwnerId}</a>
                  </div>
                }
              </div>
            </div>
          </div>
          <div className="mdw-flex-item-right">
            <div className="mdw-item-group">
              {task.status != 'Open' &&
                <span>{task.status}</span>
              }
            </div>
            {task.assignee &&
              <div>
                <img src={this.context.hubRoot + '/images/user.png'} alt="user"/> {task.assignee}
              </div>
            }
            {task.template &&
              <div className="mdw-item-group" style={{marginTop:'10px'}}>
                <Glyphicon glyph="file" style={{marginRight:'4px'}}/>
                <a href={'#/asset/' + task.template}>Task Template</a>
              </div>
            }
          </div>
        </div>
        <div className="mdw-task-process">
          <span className="mdw-task-package">{task.packageName}/</span>
          <a href={'#/workflow/processes/' + task.ownerId}>
            {task.processName} {task.ownerId}
          </a>
        </div>
        <div id="mdw-task-workflow" className="mdw-task-workflow">
          {task.ownerType == 'PROCESS_INSTANCE' && task.ownerId &&
            <Workflow instanceId={task.ownerId} animate={true} activity={task.activityInstanceId}
              containerId='mdw-task-workflow' hubBase={this.context.hubRoot} serviceBase={this.context.serviceRoot} />
          }
        </div>
      </div>
    );
  }
}

Task.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};
export default Task; 