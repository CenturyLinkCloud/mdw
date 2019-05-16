import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import '../node/node_modules/style-loader!../node/node_modules/vis/dist/vis.css';
import {Network} from '../node/node_modules/vis/dist/vis';
import Heading from './Heading.jsx';
import UserDate from '../react/UserDate.jsx';
import options from './options.js';

class Milestone extends Component {
    
  constructor(...args) {
    super(...args);
  }  

  render() {
    // TODO: this refBase doesn't work for non-hash (react) locations
    const refBase = location.hash ? '#/' : location.origin + this.context.hubRoot + '/#/';
    const milestone = this.props.milestone;

    const container = document.getElementById('milestone-graph');
    if (container) {
      const graphOptions = Object.assign({}, {height: (this.props.data.maxDepth * 100) + 'px'}, options.graph);
      const graphData = {
        nodes: this.props.data.items, 
        edges: this.props.data.edges
      };
      const network = new Network(container, graphData, graphOptions);
    }

    return (
      <div>
        <Heading milestone={this.props.milestone} />
        <div className="mdw-section">
          <div className="mdw-flex-item">
            <div className="mdw-flex-item-left">
              <div>
                <div>
                  <div className="mdw-item-group" style={{lineHeight:'1.4'}}>
                    <span>
                      <label>Master request:</label>
                      <a href={refBase + 'workflow/masterRequests/' + milestone.masterRequestId} 
                          className="mdw-link">{milestone.masterRequestId}
                      </a>
                    </span>
                  </div>
                  {milestone.processInstance &&
                    <div className="mdw-item-group" >
                      <UserDate id="milestoneStart" label="Started" date={milestone.processInstance.start} />
                      {milestone.processInstance.due && 
                        <span>{',   '}
                          <UserDate id="milestoneDue" label="Due" date={milestone.processInstance.due} alert={!milestone.processInstance.end} />
                        </span>
                      }
                      {milestone.processInstance.end &&
                        <span>{',   '}
                          <UserDate label={milestone.processInstance.status} date={milestone.processInstance.end} />
                        </span>
                      }
                    </div>
                  }
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
        <div id="milestone-graph">
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