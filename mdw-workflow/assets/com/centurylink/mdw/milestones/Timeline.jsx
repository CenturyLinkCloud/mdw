import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Link} from '../node/node_modules/react-router-dom';

class Timeline extends Component {
    
  constructor(...args) {
    super(...args);
  }  

  componentDidMount() {
  }
  
  render() {
    const milestone = this.props.milestone;
    return (
      <div>
        <div className="panel-heading mdw-heading">
          <div className="mdw-heading-label">
            {'Timeline: '}
            <Link
              to={this.context.hubRoot + '/milestones/' + milestone.masterRequestId}>
              {milestone.label}
            </Link>
          </div>
        </div>
        <div className="mdw-section">
          Timeline
        </div>
      </div>
    );
  }
}

Timeline.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};
export default Timeline; 