import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Link} from '../node/node_modules/react-router-dom';

class Heading extends Component {
    
  constructor(...args) {
    super(...args);
  }  

  render() {
    return (
      <div className="panel-heading mdw-heading">
        <div className="mdw-heading-label">
        {this.props.milestone.title}
          <Link className="mdw-id"
            to={this.context.hubRoot + '/milestones/' + this.props.milestone.masterRequestId}>
            {this.props.milestone.masterRequestId}
          </Link>
        </div>
      </div>
    );
  }
}

Heading.contextTypes = {
  hubRoot: PropTypes.string
};

export default Heading; 