import React, {Component} from '../node/node_modules/react';
import {
  HashRouter as Router, Switch, Route
} from '../node/node_modules/react-router-dom';
import Nav from './Nav.jsx';
import Heading from './Heading.jsx';
import Task from './Task.jsx';
import History from './History.jsx';

class Main extends Component {
  constructor(...args) {
    super(...args);
    this.state = { task: {} };
  }
  
  componentDidMount() {
    var path = window.location.hash.substring(8);
    fetch(new Request('/mdw/services/Tasks/' + path, {
      method: 'GET',
      headers: { Accept: 'application/json'}
    }))
    .then(response => {
      return response.json();
    })
    .then(json => {
      this.setState({
        task: json
      });
    });
  }
  
  render() {
    return (
      <Router>
        <div>
          <div className="col-md-2 mdw-sidebar">
            <Nav />
          </div>
          <div className="col-md-10">
            <div className="panel panel-default mdw-panel">
              <Heading task={this.state.task} />
              <div className="mdw-section">
                <Route exact path="/tasks/:id" 
                  render={() => <Task task={this.state.task} />} />
                <Route path="/tasks/:id/history" 
                  render={() => <History task={this.state.task} />} />
              </div>
            </div>
          </div>
        </div>
      </Router>
    );
  }
}

export default Main; 