import React, {Component} from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';

class Index extends Component {
  constructor(...args) {
    super(...args);
  }
  
  componentDidMount() {
  }
  
  render() {
    return (
      <div>
        HELLO
      </div>
    );
  }
  
  getChildContext() {
    return {
      hubRoot: $mdwHubRoot,
      serviceRoot: $mdwServicesRoot + '/services'
    };
  }
}

Index.childContextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default Index; 