import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import Workflow from '../react/Workflow.jsx';
import UserDate from './UserDate.jsx';

class Task extends Component {
    
  constructor(...args) {
    super(...args);
    this.handleClick = this.handleClick.bind(this);
  }  

  handleClick(event) {
    if (event.currentTarget.type === 'button') {
      if (event.currentTarget.value === 'save') {
        console.log('save task');
      }
    }
  }
  
  render() {
    var task = this.props.task;
    return (
      <div>
        <div className="mdw-flex-item">
          <div className="mdw-flex-item-left">
            <div>
              <div>
                <div className="mdw-item">
                  {task.masterRequestId && 
                    <span>
                      Master request:{' '}
                      <a href={'#/workflow/masterRequests/' + task.masterRequestId} 
                          className="mdw-link">{task.masterRequestId}
                      </a>
                    </span>
                  }
                </div>
                <div className="mdw-item">
                  <UserDate label="Created" date={task.start} />
                  {task.due &&
                    <span>{',  '}
                      <UserDate label="Due" date={task.due} alert={true} 
                        editable={task.editable} notLabel="No Due Date" />
                    </span>
                  }
                  {task.end &&
                    <UserDate label={task.status} date={task.end} />
                  }
                </div>
                {task.workgroups &&
                  <div className="mdw-item">
                    Workgroups:{' '}
                    {
                      task.workgroups.map((workgroup, i) => {
                        return (
                          <span key={workgroup}>
                            <a className="mdw-adjoin" href={'/#/groups/' + workgroup}>
                              {workgroup}
                            </a>
                            {i < task.workgroups.length - 1 && ', '}
                          </span>
                        );
                      })
                    }
                  </div>
                }
                {task.secondaryOwnerType == 'TASK_INSTANCE' &&
                  <div className="mdw-item">
                    Master Task: 
                    <a href={'#/tasks/' + task.secondaryOwnerId}>{task.secondaryOwnerId}</a>
                  </div>
                }
                {task.ownerType == 'PROCESS_INSTANCE' &&
                  <div className="mdw-item">
                    Process:{' '}
                    <span style={{fontWeight:'normal'}}>{task.packageName}/</span>
                    <a href={'#/workflow/processes/' + task.ownerId}>{task.processName}</a>
                    {' '}<a href={'#/workflow/processes/' + task.ownerId}>{task.ownerId}</a>
                  </div>
                }
              </div>
            </div>
          </div>
          <div className="mdw-flex-item-right">
            <div className="mdw-item">
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
              <div className="mdw-item" style={{marginTop:'10px'}}>
                <a href={'#/asset/' + task.template}>Task Template</a>
              </div>
            }
          </div>
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