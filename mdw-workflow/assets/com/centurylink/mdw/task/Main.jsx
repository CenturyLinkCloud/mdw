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
  
  // supports updating dueDate and workgroups (TODO: priority)
  updateTask(updates) {
    var updatedTask = Object.assign(this.state.task, updates);
    // save
    var ok = false;
    fetch(new Request(this.getChildContext().serviceRoot + '/Tasks/' + this.state.task.id, {
      method: 'PUT',
      headers: { Accept: 'application/json'},
      body: JSON.stringify(updatedTask)
    }))
    .then(response => {
      ok = response.ok;
      return response.json();
    })
    .then(json => {
      if (ok) {
        $mdwUi.clearMessage();
      }
      else {
        $mdwUi.showMessage(json.status.message);
      }
    });
    // waiting for server response could be confusing
    this.setState({
      task: updatedTask
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
      var assignedToMe = $mdwUi.authUser.cuid === task.assigneeId;
      var inFinalState = task.status === 'Completed' || task.status === 'Cancelled' || task.status === 'Canceled';
      task.actionable = !inFinalState;
      task.editable = assignedToMe && !inFinalState;
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