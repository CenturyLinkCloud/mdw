import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import PanelHeader from '../react/PanelHeader.jsx';

class Activities extends Component {

  constructor(...args) {
    super(...args);
  }

  componentDidMount() {
  }

  render() {
    return (
      <div>
        <PanelHeader title="Activities" />
        <div className="mdw-section">
          <div>ACTIVITIES</div>
        </div>
      </div>
    );
  }
}

Activities.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default Activities;
