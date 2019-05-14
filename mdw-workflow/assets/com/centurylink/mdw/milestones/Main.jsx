import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {
  BrowserRouter as Router, Route
} from '../node/node_modules/react-router-dom';
import Nav from './Nav.jsx';
import Milestone from './Milestone.jsx';
import Gantt from './Gantt.jsx';

class Main extends Component {
  constructor(...args) {
    super(...args);
    this.state = { milestone: {} };
  }
  
  componentDidMount() {
    $mdwUi.clearMessage();
    const masterRequestId = window.location.hash.substring(13);
    const url = this.getChildContext().serviceRoot + '/com/centurylink/mdw/milestones/' + masterRequestId;
    fetch(new Request(url, {
      method: 'GET',
      headers: { Accept: 'application/json'},
      credentials: 'same-origin'
    }))
    .then(response => {
      return response.json();
    })
    .then(milestone => {
      this.setState({
        milestone: milestone
      });
    });
  }
  
  render() {
    var hub = this.getChildContext().hubRoot + '/';
    return (
      <Router>
        <div>
          <div className="col-md-2 mdw-sidebar">
            <Nav milestone={this.state.milestone} />
          </div>
          <div className="col-md-10">
            <div className="panel panel-default mdw-panel">
              <Route exact path={hub}
                render={(props) => <Milestone {...props} milestone={this.state.milestone} />} />
              <Route exact path={hub + 'milestones/:masterRequestId'} 
                render={(props) => <Milestone {...props} milestone={this.state.milestone} />} />
              <Route exact path={hub + 'milestones/:masterRequestId/gantt'} 
                render={(props) => <Gantt {...props} milestone={this.state.milestone} />} />
            </div>
          </div>
        </div>
      </Router>
    );
  }

  getChildContext() {
    return {
      hubRoot: $mdwHubRoot,
      serviceRoot: $mdwServicesRoot + '/api'
    };
  }
}

Main.childContextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default Main;