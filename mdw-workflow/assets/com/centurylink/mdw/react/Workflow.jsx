import React, {Component} from '../node/node_modules/react';
import Toolbox from './Toolbox.jsx';
import Inspector from './Inspector.jsx';
import implementors from './implementors';

class Workflow extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { process: {} };
  }
  
  doRender(process, implementors, instanceId) {
    var canvas = document.getElementById('mdw-canvas');
    if (instanceId) {
      fetch(new Request(this.props.serviceBase + '/Processes/' + instanceId, {
        method: 'GET',
        headers: {Accept: 'application/json'}
      }))
      .then(response => {
        return response.json();
      })
      .then(instance => {
        var diagram = new (mdwUi.Diagram)(canvas, null, process, implementors, this.props.hubBase, this.props.editable, instance);
        this.setState({
          process: process,
          instance: instance,
          implementors: this.implementors,
          diagram: diagram
        })
        diagram.draw();
      });
    }
    else {
      var diagram = new (mdwUi.Diagram)(canvas, null, process, implementors, this.props.hubBase, this.props.editable);
      this.setState({
        process: process,
        implementors: this.implementors,
        diagram: diagram
      })
      diagram.draw();
      if (this.props.editable) {
        var toolbox = Toolbox.getToolbox();
        toolbox.init(this.implementors, this.props.hubBase);
      }
    }
  }
  
  componentDidMount() {
    var path = this.props.assetPath;
    var pkg = path.substring(0, path.lastIndexOf('/'));
    var workflowUrl = this.props.serviceBase + '/Workflow/' + path;
    if (this.props.editable)
      workflowUrl += '?forUpdate'; // TODO not honored
    console.log('retrieving process: ' + path);
    fetch(new Request(workflowUrl, {
      method: 'GET',
      headers: {Accept: 'application/json'}
    }))
    .then(response => {
      return response.json();
    })
    .then(process => {
      process.instanceId = this.props.instanceId;
      process.packageName = pkg;
      implementors.get(this.props.serviceBase, impls => {
        this.doRender(process, impls, this.props.instanceId);
      });
    });
  }
  
  render() {
    return (
      <div className="mdw-workflow">
        <div>
          <canvas id="mdw-canvas" className="mdw-canvas"></canvas> 
        </div>
        <Toolbox />
        <Inspector />
      </div>  
     );
  }
}

export default Workflow;  