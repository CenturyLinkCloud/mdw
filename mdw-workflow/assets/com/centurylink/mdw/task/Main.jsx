import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {
  BrowserRouter as Router, Switch, Route, IndexRoute
} from '../node/node_modules/react-router-dom';
import Nav from './Nav.jsx';
import Heading from './Heading.jsx';
import Task from './Task.jsx';
import Discussion from './Discussion.jsx';
import Subtasks from './Subtasks.jsx';
import History from './History.jsx';

class Main extends Component {
  constructor(...args) {
    super(...args);
    this.state = { task: {} };
  }
  
  componentDidMount() {
    var path = window.location.hash.substring(8);
    console.log('retrieving task: ' + path);
    fetch(new Request('/mdw/services/Tasks/' + path, {
      method: 'GET',
      headers: { Accept: 'application/json'}
    }))
    .then(response => {
      return response.json();
    })
    .then(task => {
      task.actionable = true; // TODO assigned to user and not in final state
      task.editable = true; // TODO assigned to user and not in final state
      this.setState({
        task: task
      });
    });
  }
  
  render() {
    var hub = this.getChildContext().hubRoot + '/';
    return (
      <Router>
        <div>
          <div className="col-md-2 mdw-sidebar">
            <Nav task={this.state.task} />
          </div>
          <div className="col-md-10">
            <div className="panel panel-default mdw-panel">
              <Heading task={this.state.task} />
              <div className="mdw-section">
                <Route exact path={hub} 
                  render={(props) => <Task {...props} task={this.state.task} />} />
                <Route exact path={hub + 'tasks/:id'} 
                  render={(props) => <Task {...props} task={this.state.task} />} />
                <Route path={hub + 'tasks/:id/discussion'} 
                  render={() => <Discussion task={this.state.task} />} />
                <Route path={hub + 'tasks/:id/subtasks'}
                  render={() => <Subtasks task={this.state.task} />} />
                <Route path={hub + 'tasks/:id/history'} 
                  render={() => <History task={this.state.task} />} />
              </div>
            </div>
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

Main.childContextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default Main; 