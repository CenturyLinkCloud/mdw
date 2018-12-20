import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import { BrowserRouter as Router} from '../node/node_modules/react-router-dom';
import Nav from './Nav.jsx';
// To add custom charts, override Routes.jsx and Index.jsx in custom UI package.
import Routes from './Routes.jsx';

class Index extends Component {
  constructor(...args) {
    super(...args);
  }

  render() {
    var hub = this.getChildContext().hubRoot + '/';
    return (
      <Router>
        <div>
          <div className="col-md-2 mdw-sidebar">
            <Nav />
          </div>
          <Routes />
        </div>
      </Router>
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
