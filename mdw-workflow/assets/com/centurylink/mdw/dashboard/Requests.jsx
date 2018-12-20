import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import Heading from './Heading.jsx';

class Requests extends Component {

  constructor(...args) {
    super(...args);
    console.log('PROPS: ' + JSON.stringify(this.props, null, 2));
    
  }

  componentDidMount() {
  }

  render() {
    return (
      <div>
        <Heading title="Requests" />
        <div className="mdw-section">
          <div>HELLO, REQUEST</div>
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
