import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import Heading from './Heading.jsx';

class Requests extends Component {

  constructor(...args) {
    super(...args);
  }

  componentDidMount() {
  }

  render() {
    return (
      <div>
        <Heading title="Processes" />
        <div className="mdw-section">
          <div>HELLO, PROCESS</div>
        </div>
      </div>
    );
  }
}

Requests.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default Requests;
