import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import { BrowserRouter as Router} from '../node/node_modules/react-router-dom';
import Header from '../react/Header.jsx';
import Footer from '../react/Footer.jsx';
import Nav from './Nav.jsx';
// To add custom charts, override Routes.jsx and Index.jsx in custom UI package.
import Routes from './Routes.jsx';

class Index extends Component {

  constructor(...args) {
    super(...args);
    this.state = { authUser: {} };
    document.body.classList.add('mdw-body');
  }

  componentDidMount() {
    $mdwUi.clearMessage();
    fetch(new Request(this.getChildContext().serviceRoot + '/AuthenticatedUser', {
      method: 'GET',
      headers: { Accept: 'application/json'},
      credentials: 'same-origin'
    }))
    .then(response => {
      return response.json();
    })
    .then(authUser => {
      this.setState({
        authUser: authUser
      });
    });
  }

  render() {
    return (
      <div>
        <Header activeTab="dashboardTab" />
        { /* TODO: mdw-mobile-content instead of mdw-content if mobile */ }
        <div id="mdw-main" className="content container mdw-content">
          <Router>
            <div className="row">
              <div className="col-md-2 mdw-sidebar">
                <Nav />
              </div>
              <div className="col-md-10">
                <Routes />
              </div>
            </div>
          </Router>
        </div>
        <Footer />
      </div>
    );
  }

  getChildContext() {
    return {
      hubRoot: $mdwHubRoot,
      serviceRoot: $mdwServicesRoot + '/services',
      authUser: this.state.authUser
    };
  }
}

Index.childContextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string,
  authUser: PropTypes.object
};

export default Index;
