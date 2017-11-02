import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import values from '../react/values';
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
  }
  
  componentDidMount() {
    fetch(new Request($mdwServicesRoot + '/services/Processes/run/' + this.state.assetPath, {
      method: 'GET',
      headers: {Accept: 'application/json'}
    }))
    .then(response => {
      return response.json();
    })
    .then(run => {
      this.setState({
        assetPath: this.state.assetPath,
        masterRequestId: run.masterRequestId,
        values: values.toArray(run.values)
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
                <Button className="mdw-btn mdw-action-btn" bsStyle="primary">
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
                  containerId='mdw-workflow' 
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

Run.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string  
};

export default Run;