import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import PanelHeader from '../react/PanelHeader.jsx';

class InboundRequests extends Component {

  constructor(...args) {
    super(...args);
  }

  componentDidMount() {
  }

  render() {
    return (
      <div>
        <PanelHeader title="Inbound Requests" />
        <div className="mdw-section">
          <div>INBOUND REQUESTS</div>
        </div>
      </div>
    );
  }
}

InboundRequests.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default InboundRequests;
