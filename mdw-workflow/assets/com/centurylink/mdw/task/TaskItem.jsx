import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import {Link} from '../node/node_modules/react-router-dom';
import UserDate from '../react/UserDate.jsx';

class TaskItem extends Component {
    
  constructor(...args) {
    super(...args);
  }  

  render() {
    var task = this.props.task;
    return (
      <div className="mdw-flex-item">
        <div className="mdw-flex-item-left">
          <div>
            <input type="checkbox" value={false} />
            <div>
              <Link to={this.context.hubRoot + '/tasks/' + this.props.task.id} 
                className="mdw-item-link">
                {task.name} {task.id}
              </Link>
              <div className="mdw-item-sub" style={{height:'16px'}}>
                {task.masterRequestId &&
                  <span>
                    <label>Master request:</label> 
                    <a className="mdw-link"
                      href="#/workflow/masterRequests/{{item.masterRequestId}}">
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