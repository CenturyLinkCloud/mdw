import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import PanelHeader from '../react/PanelHeader.jsx';

class OutboundRequests extends Component {

  constructor(...args) {
    super(...args);
  }

  componentDidMount() {
  }

  render() {
    return (
      <div>
        <PanelHeader title="Outbound Requests" />
        <div className="mdw-section">
          <div>OUTBOUND REQUESTS</div>
        </div>
      </div>
    );
  }
}

OutboundRequests.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default OutboundRequests;
