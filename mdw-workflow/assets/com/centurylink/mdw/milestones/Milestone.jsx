import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import Heading from './Heading.jsx';
import UserDate from '../react/UserDate.jsx';
import '../node/node_modules/style-loader!../node/node_modules/vis/dist/vis.css';
import Data from './Data.js';
import {Network,DataSet} from '../node/node_modules/vis/dist/vis';

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

    var container = document.getElementById('milestone-graph');
    if (container) {
      const data = new Data(this.props.milestone);
      var options = {
        height: (data.maxDepth * 100) + 'px',
        nodes: {
          shape: 'dot'
        },
        layout: {
          hierarchical: true
        }
      };
      const networkData = {nodes: data.items, edges: data.edges};
      const network = new Network(container, networkData, options);
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