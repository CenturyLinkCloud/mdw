import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import PanelHeading from '../react/PanelHeading.jsx';

class Requests extends Component {

  constructor(...args) {
    super(...args);
  }

  componentDidMount() {
  }

  render() {
    return (
      <div>
        <PanelHeading title="Requests" />
        <div className="mdw-section">
          <div>HELLO, REQUESTS</div>
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