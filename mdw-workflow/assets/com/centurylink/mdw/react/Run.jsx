import React, {Component} from '../node/node_modules/react';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import values from './values';
import Value from './Value.jsx';
import Workflow from './Workflow.jsx';

class Run extends Component {
  constructor(...args) {
    super(...args);
    this.state = { 
        assetPath: window.location.hash.substring(15),
        masterRequestId: '',
        values: [] 
    };
    this.handleChange = this.handleChange.bind(this);
    this.handleClick = this.handleClick.bind(this);
    values.showLines = 12;
    values.maxLines = 20;
  }
  
  componentDidMount() {
    const path = '/services/Processes/run/' + this.state.assetPath;
    fetch(new Request($mdwServicesRoot + path, {
      method: 'GET',
      headers: {Accept: 'application/json'},
      credentials: 'same-origin'
    }))
    .then(response => {
      return response.json();
    })
    .then(run => {
      const vals = values.toArray(run.values);
      if (vals) {
        // populated remembered values from localStorage
        const storVals = localStorage.getItem(path + '-values');
        if (storVals) {
          const vs = JSON.parse(storVals);
          vals.forEach(val => {
            const v = vs[val.name];
            if (v)
              val.value = v;
          });
        }
      }
      
      this.setState({
        assetPath: this.state.assetPath,
        masterRequestId: run.masterRequestId,
        values: vals
      });
    });
  }
  
  handleChange(event, newValue) {
    if (event.currentTarget.name === 'masterRequestId') {
      this.setState({
        assetPath: this.state.assetPath,
        masterRequestId: event.currentTarget.value,
        values: this.state.values
      });
    }
    else {
      this.setState({
        assetPath: this.state.assetPath,
        masterRequestId: this.state.masterRequestId,
        values: values.update(this.state.values.slice(), event, newValue)
      });
    }
  }
  
  handleClick(event) {
    if (event.currentTarget.name === 'run') {
      if ($mdwWebSocketUrl) {
        var state = this.state; // for access in listeners
        const socket = new WebSocket($mdwWebSocketUrl);
        socket.addEventListener('open', function(event) { // eslint-disable-line no-unused-vars
          socket.send(state.masterRequestId);
        });
        socket.addEventListener('message', function(event) {
          var message = JSON.parse(event.data);
          if (message.subtype === 'm') {
            console.log("RECEIVED: " + JSON.stringify(message, null, 2)); // eslint-disable-line no-console
            this.setState({
              assetPath: state.assetPath,
              masterRequestId: state.masterRequestId,
              values: [],
              instanceId: state.instanceId,
              invoked: true,
            });
          }
        });
        var run = {
          masterRequestId: this.state.masterRequestId,
          values: values.toObject(this.state.values)
        };
        // store values in localStorage
        const path = '/services/Processes/run/' + this.state.assetPath;
        localStorage.setItem(path + '-values', JSON.stringify(run.values));
        let ok;
        fetch(new Request($mdwServicesRoot + path, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(run),
          credentials: 'same-origin'
        }))
        .then(response => {
          ok = response.ok; 
          return response.json();
        })
        .then(json => {
          if (ok) {
            $mdwUi.clearMessage();
            if (json.instanceId) {
              location = $mdwHubRoot + '/#/workflow/processes/' + json.instanceId;
            }
            else {
              this.setState({
                assetPath: this.state.assetPath,
                masterRequestId: this.state.masterRequestId,
                values: this.state.values,
                instanceId: json.instanceId
              });
            }
          }
          else {
            $mdwUi.showMessage(json.status.message);
          }
        });
      }
    }
  }
  
  render() {
    const slash = this.state.assetPath.indexOf('/');
    const name = slash > 0 ? this.state.assetPath.substring(slash + 1) : this.state.assetPath;
    const pkg = slash > 0 ? this.state.assetPath.substring(0, slash) : ''; 
    return (
      <div>
        <div className="col-md-2 mdw-sidebar">
        <ul className="nav mdw-nav">
          <li className="mdw-active">
            <a href="">Run</a>
          </li>
          <li>
            <a href={'#/workflow/definitions/' + this.state.assetPath}>Definition</a>
          </li>
        </ul>
        <ul className="nav mdw-nav">
          <li>
            <a href="#/workflow/processes">Process List</a>
          </li>
          <li>
            <a href="#/workflow/definitions">Definitions</a>
          </li>
        </ul>
      </div>
      <div className="col-md-10">
        <div className="panel panel-default mdw-panel">
          <div className="panel-heading mdw-heading">
            <div className="mdw-heading-label">
              <a href={'#/packages/' + pkg}>{pkg}</a>
              {' / '}
              <a href={'#/workflow/definitions/' + this.state.assetPath}>{name}</a>
            </div>
            <div className="mdw-heading-actions">
              { !this.state.invoked &&
                <Button name="run" className="mdw-btn mdw-action-btn" bsStyle="primary" onClick={this.handleClick}>
                  <Glyphicon glyph="play" />{' Run'}
                </Button>
              }
            </div>
          </div>
          <div className="mdw-section">
            { !this.state.invoked &&
              <form name="valuesForm" className="form-horizontal" role="form">
                <div className="form-group">
                  <label className="control-label col-xs-2 mdw-required">
                    Master Request ID
                  </label>
                  <div className="col-xs-4">
                    <input type="text" className="form-control" 
                      id="masterRequestId" name="masterRequestId" value={this.state.masterRequestId} 
                      onChange={this.handleChange} />
                  </div>
                </div>
                { this.state.values.map(value => {
                    return (
                      <Value value={value} key={value.name} 
                        editable={true} onChange={this.handleChange} />
                    );
                  })
                }
              </form>
            }
            { this.state.invoked &&
              <div id="mdw-workflow" className="mdw-workflow">
                <Workflow assetPath={this.state.masterRequestId} 
                  containerId='mdw-workflow' instanceId={this.state.instanceId} masterRequestId={this.state.masterRequestId}
                  hubBase={$mdwHubRoot} serviceBase={$mdwServicesRoot + '/services'} />
              </div>
            }
          </div>
        </div>
      </div>
    </div>
    );
  }  
}

export default Run;