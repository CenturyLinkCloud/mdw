import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import Heading from './Heading.jsx';
import UserDate from '../react/UserDate.jsx';

class Milestone extends Component {
    
  constructor(...args) {
    super(...args);
  }  

  componentDidMount() {
  }
  
  render() {
    // TODO: this refBase doesn't work for non-hash (react) locations
    const refBase = location.hash ? '#/' : location.origin + this.context.hubRoot + '/#/';
    const milestone = this.props.milestone.milestone ? this.props.milestone.milestone : {};
    return (
      <div>
        <Heading milestone={this.props.milestone} />
        <div className="mdw-section">
          <div className="mdw-flex-item">
            <div className="mdw-flex-item-left">
              <div>
                <div>
                  <div className="mdw-item-group">
                    <span>
                      <label>Master request:</label>
                      <a href={refBase + 'workflow/masterRequests/' + milestone.masterRequestId} 
                          className="mdw-link">{milestone.masterRequestId}
                      </a>
                    </span>
                  </div>
                  <div className="mdw-item-group">
                    <UserDate id="milestoneStart" label="Started" date={milestone.startDate} />
                    {milestone.due &&
                      <span>{',   '}
                        <UserDate id="milestoneDue" label="Due" date={milestone.due} alert={!milestone.endDate} />
                      </span>
                    }
                    {milestone.endDate && milestone.processInstance &&
                      <span>{',   '}
                        <UserDate label={milestone.processInstance.status} date={milestone.endDate} />
                      </span>
                    }
                  </div>
                </div>
              </div>
            </div>
            <div className="mdw-flex-item-right">
              {milestone.processInstance &&
                <div className="mdw-item-group">
                  {milestone.processInstance.status}
                </div>
            }
              {milestone.assignee &&
                <div>
                  <img src={this.context.hubRoot + '/images/user.png'} alt="user"/>
                  {' '}<a href={refBase + 'users/' + milestone.assigneeId}>{milestone.assignee}</a>
                </div>
              }
            </div>
          </div>
        </div>
      </div>
    );
  }
}

Milestone.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};
export default Milestone; 