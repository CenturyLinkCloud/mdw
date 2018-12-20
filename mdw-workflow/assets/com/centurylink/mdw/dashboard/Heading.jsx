import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';

class Heading extends Component {

  constructor(...args) {
    super(...args);
  }

  render() {
    return (
      <div className="panel-heading mdw-heading">
        <div className="mdw-heading-label">
          {this.props.title}
        </div>
      </div>
    );
  }
}

Heading.contextTypes = {
  hubRoot: PropTypes.string
};

export default Heading;
