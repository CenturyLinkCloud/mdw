import React, {Component} from '../node/node_modules/react';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';

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
                      Master request:
                      <a href={'#/workflow/masterRequests/' + task.masterRequestId} 
                          className="mdw-link2">{task.masterRequestId}
                      </a>
                    </span>
                  }
                </div>
                <div className="mdw-item">Created {task.created},
                  {task.due &&
                    <span> Due <i className="mdw-item-highlight">{task.due}</i></span>
                  }
                  {task.alert &&
                    <img className="mdw-item-alert" src="images/alert.png" alt="alert" />
                  }
                  {!task.due && task.editable &&
                    <span><i>No Due Date</i></span>
                  }
                  {task.editable &&
                    <button type="button" className="btn mdw-btn btn-default" onClick="{this.handleClick}"
                      style={{marginLeft: '5px'}}>
                      <i className="glyphicon glyphicon-calendar"></i>
                    </button>
                  }
                </div>
                {task.endDate &&
                  <div className="mdw-item-sub">{task.status} {task.endDate}</div>
                }
                {task.workgroups &&
                  <div className="mdw-item">
                    Workgroups:
                    {
                      task.workgroups.map(workgroup => {
                        return (
                          <a className="mdw-adjoin" href={'/#/groups/' + workgroup}>{workgroup}</a>
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
                <img src="images/user.png" alt="user"/> {task.assignee}
              </div>
            }
            {task.template &&
              <div className="mdw-item" style={{marginTop:'10px'}}>
                <a href={'#/asset/' + task.template}>Task Template</a>
              </div>
            }
          </div>
        </div>
      </div>
    );
  }
}

export default Task; 