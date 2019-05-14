import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Link} from '../node/node_modules/react-router-dom';

class Heading extends Component {
    
  constructor(...args) {
    super(...args);
  }  

  render() {
    const milestone = this.props.milestone.milestone ? this.props.milestone.milestone : {};
    return (
      <div className="panel-heading mdw-heading">
        <div className="mdw-heading-label">
          {milestone.title}
          <Link className="mdw-id"
            to={this.context.hubRoot + '/milestones/' + milestone.masterRequestId}>
            {milestone.masterRequestId}
          </Link>
        </div>
        <div className="mdw-heading-actions">
          {this.props.children}
        </div>
      </div>
    );
  }
}

Heading.contextTypes = {
  hubRoot: PropTypes.string
};

export default Heading; 