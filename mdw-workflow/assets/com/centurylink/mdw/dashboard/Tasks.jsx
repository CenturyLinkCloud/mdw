import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import PanelHeader from '../react/PanelHeader.jsx';

class Tasks extends Component {

  constructor(...args) {
    super(...args);
  }

  componentDidMount() {
  }

  render() {
    return (
      <div>
        <PanelHeader title="Tasks" />
        <div className="mdw-section">
          <div>TASKS</div>
        </div>
      </div>
    );
  }
}

Tasks.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default Tasks;
