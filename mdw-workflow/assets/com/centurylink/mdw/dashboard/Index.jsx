import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {
  BrowserRouter as Router, Route
} from '../node/node_modules/react-router-dom';
import Nav from './Nav.jsx';
import Processes from './Processes.jsx';
import Requests from './Requests.jsx';
import Something from './Something.jsx';

class Index extends Component {
  constructor(...args) {
    super(...args);
    this.state = { dashLinks: [] };
  }

  componentDidMount() {
    $mdwUi.clearMessage();
    fetch(new Request(this.getChildContext().hubRoot + '/js/nav.json', {
      method: 'GET',
      headers: { Accept: 'application/json'},
      credentials: 'same-origin'
    }))
    .then(response => {
      return response.json();
    })
    .then(navs => {
      const dashNav = navs.find(nav => nav.id === 'dashboardTab');
      this.setState({
        dashLinks: dashNav.navs
      });
    });
  }

  getFlatLinks() {
    var flat = [];
    this.state.dashLinks.forEach(dashSection => {
      flat = flat.concat(dashSection.links)
    });
    return flat;
  }

  render() {
    console.log('PROPS: ' + JSON.stringify(this.props, null, 2));

    var hub = this.getChildContext().hubRoot + '/';
    return (
      <Router basename={hub}>
        <div>
          <div className="col-md-2 mdw-sidebar">
            <Nav dashLinks={this.state.dashLinks} />
          </div>
          <div className="panel panel-default mdw-panel">
          {
            this.getFlatLinks().map((dashLink, index) => {
              return (
                <Route key={index} exact path={dashLink.href}
                  render={(props) => <Requests {...props} requests={this.state.requests} />} />
              );
            })
          }
          </div>
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
