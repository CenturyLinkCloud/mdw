import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import {Link} from '../node/node_modules/react-router-dom';
import UserDate from '../react/UserDate.jsx';

class TaskItem extends Component {
    
  constructor(...args) {
    super(...args);
  }  
  handleClick(task) {
      //this.props.updateTask(task);
      this.props.refreshTask(task.id);
  }

  render() {
    var task = this.props.task;
    return (
      <div className="mdw-flex-item">
        <div className="mdw-flex-item-left">
          <div>
            <input type="checkbox" value={false} />
            <div>
              <Link to={this.context.hubRoot + '/#/tasks/' + task.id} onClick={() => this.handleClick(this.props.task)}
                className="mdw-item-link">
                {task.name} {task.id}
              </Link>
              <div className="mdw-item-sub" style={{height:'16px'}}>
                {task.masterRequestId &&
                  <span>
                    <label>Master request:</label> 
                    <a className="mdw-link"
                        href={this.context.hubRoot + '/#/workflow/masterRequests/' + task.masterRequestId}>
                      {task.masterRequestId}
                    </a>
                  </span>
                }
              </div>
              <div className="mdw-item-sub">
                <UserDate label="Created" date={task.start} />
                {task.due &&
                  <span>{',   '}
                    <UserDate label="Due" date={task.due} alert={!task.end} 
                      editable={false} notLabel="No Due Date" />
                  </span>
                }
                {task.end &&
                  <span>{',   '}
                    <UserDate label={task.status} date={task.end} />
                  </span>
                }
              </div>
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
              <img src={this.context.hubRoot + '/images/user.png'} alt="user"/>
              {' '}<a href={'#/users/' + task.assigneeId}>{task.assignee}</a>
            </div>
          }
        </div>
      </div>
    );
  }
}

TaskItem.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default TaskItem;