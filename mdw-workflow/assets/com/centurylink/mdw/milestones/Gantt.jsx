import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import Heading from './Heading.jsx';

class Gantt extends Component {
    
  constructor(...args) {
    super(...args);
  }  

  componentDidMount() {
  }
  
  render() {
    return (
      <div>
        <Heading task={this.props.milestone} />
        <div className="mdw-section">
          Gantt
        </div>
      </div>
    );
  }
}

Gantt.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};
export default Gantt; 