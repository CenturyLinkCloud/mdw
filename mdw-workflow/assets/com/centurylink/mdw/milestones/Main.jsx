import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {
  BrowserRouter as Router, Route
} from '../node/node_modules/react-router-dom';
import Nav from './Nav.jsx';
import DefNav from './DefNav.jsx';
import Data from './Data.js';
import Milestone from './Milestone.jsx';
import Timeline from './Timeline.jsx';

class Main extends Component {
  constructor(...args) {
    super(...args);
    this.state = { 
      milestone: {}, 
      data: {} 
    };
  }

  isDef() {
    return location.hash.startsWith('#/milestones/definitions');
  }
  
  componentDidMount() {
    if (!this.isDef()) {
      const masterRequestId = location.hash.substring(13);
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
          milestone: milestone.milestone,
          data: new Data(milestone)
        });
      });
    }
  }
  
  render() {
    var hub = this.getChildContext().hubRoot + '/';
    return (
      <Router>
        <div style={{display:'flex'}}>
          <div className="mdw-sidebar">
            {this.isDef() &&
              <DefNav />
            }
            {!this.isDef() &&
              <Nav milestone={this.state.milestone} />
            }
          </div>
          <div className="mdw-panel-full-width">
            <div className="panel panel-default mdw-panel">
              <Route exact path={hub}
                render={(props) => <Milestone {...props} milestone={this.state.milestone} data={this.state.data}/>} />
              <Route exact path={hub + 'milestones/:masterRequestId'} 
                render={(props) => <Milestone {...props} milestone={this.state.milestone} data={this.state.data}/>} />
              <Route exact path={hub + 'milestones/:masterRequestId/timeline'} 
                render={(props) => <Timeline {...props} milestone={this.state.milestone} data={this.state.data}/>} />
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