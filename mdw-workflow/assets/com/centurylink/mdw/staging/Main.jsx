import React, {Component} from '../node/node_modules/react';
import {
  BrowserRouter as Router, Route
} from '../node/node_modules/react-router-dom';
import MdwContext from '../react/MdwContext';
import Nav from './Nav.jsx';
import Stage from './Stage.jsx';
import StagedAsset from './StagedAsset.jsx';

class Main extends Component {
  constructor(...args) {
    super(...args);
    this.state = { authUser: { id: '', name: '', roles: [], workgroups: [] } };
  }

  componentDidMount() {
    fetch(new Request($mdwServicesRoot + '/api/AuthenticatedUser', {
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
    const mdwContext = {
      hubRoot: $mdwHubRoot,
      serviceRoot: $mdwServicesRoot + '/api',
      authUser: this.state.authUser
    };
    const hub = mdwContext.hubRoot + '/';
    return (
      <MdwContext.Provider value={mdwContext}>
        <Router>
          <div style={{display:'flex'}}>
            <div className="mdw-sidebar">
              <Nav />
            </div>              
            <div className="mdw-panel-full-width">
              <div className="panel panel-default mdw-panel">
                <Route exact path={hub}
                  render={(props) => <Stage {...props}/>} />
                <Route exact path={hub + 'staging'}
                  render={(props) => <Stage {...props}/>} />
                <Route exact path={hub + 'staging/:cuid'}
                  render={(props) => <Stage {...props}/>} />
                <Route exact path={hub + 'staging/:cuid/assets/:package/:asset'}
                  render={(props) => <StagedAsset {...props}/>} />
              </div>
            </div>
          </div>
        </Router>
      </MdwContext.Provider>
    );
  }
}

export default Main;