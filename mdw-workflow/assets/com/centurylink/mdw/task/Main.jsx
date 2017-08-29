import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {
  BrowserRouter as Router, Switch, Route, IndexRoute
} from '../node/node_modules/react-router-dom';
import Nav from './Nav.jsx';
import Heading from './Heading.jsx';
import Task from './Task.jsx';
import Values from './Values.jsx';
import Discussion from './Discussion.jsx';
import Subtasks from './Subtasks.jsx';
import History from './History.jsx';

class Main extends Component {
  constructor(...args) {
    super(...args);
    this.state = { task: {} };
    this.updateTask = this.updateTask.bind(this);
    this.refreshTask = this.refreshTask.bind(this);
  }
  
  componentDidMount() {
    $mdwUi.clearMessage();
    this.refreshTask(window.location.hash.substring(8));
  }
  
  // supports updating dueDate and workgroups
  updateTask(updates) {
    // TODO: save state back to server
    this.setState({
      task: Object.assign(this.state.task, updates)
    });
  }
  
  refreshTask(id) {
    console.log('retrieving task: ' + id);
    fetch(new Request('/mdw/services/Tasks/' + id, {
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
              <Heading task={this.state.task} refreshTask={this.refreshTask} />
              <div className="mdw-section">
                <Route exact path={hub} 
                  render={(props) => <Task {...props} task={this.state.task} updateTask={this.updateTask} />} />
                <Route exact path={hub + 'tasks/:id'} 
                  render={(props) => <Task {...props} task={this.state.task} />} />
                <Route path={hub + 'tasks/:id/values'} 
                  render={() => <Values task={this.state.task} />} />
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