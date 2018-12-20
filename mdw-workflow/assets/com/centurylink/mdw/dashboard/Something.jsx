import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import Heading from './Heading.jsx';

class Something extends Component {

  constructor(...args) {
    super(...args);
    console.log('PROPS: ' + JSON.stringify(this.props, null, 2));

  }

  componentDidMount() {
  }

  render() {
    return (
      <div>
        <Heading title="Something" />
        <div className="mdw-section">
          <div>HELLO, SOMETHING</div>
        </div>
      </div>
    );
  }
}

Something.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default Something;
