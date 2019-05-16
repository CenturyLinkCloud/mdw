import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import Heading from './Heading.jsx';

class Timeline extends Component {
    
  constructor(...args) {
    super(...args);
  }  

  componentDidMount() {
  }
  
  render() {
    return (
      <div>
        <Heading milestone={this.props.milestone} />
        <div className="mdw-section">
          Timeline
        </div>
      </div>
    );
  }
}

Timeline.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};
export default Timeline; 