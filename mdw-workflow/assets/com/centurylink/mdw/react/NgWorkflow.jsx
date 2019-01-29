import React, {Component} from '../node/node_modules/react/react';
import implementors from './implementors';
import process from './process';

/**
 * This presumes the Angular context has been initialized, and so should only be used by
 * components that are displayed in the context of the Angular application (eg Task.jsx, Run.jsx).
 */
class NgWorkflow extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { process: {} };
  }
  
  componentDidMount() {
    process.get(this.props.serviceBase, this.props.assetPath, this.props.instanceId, this.props.masterRequestId,  proc => {
      implementors.get(this.props.serviceBase, impls => {
        var canvas = document.getElementById('mdw-canvas');
        if (canvas) {
          var diagram = new ($mdwUi.Diagram)(canvas, null, proc, impls, this.props.hubBase, this.props.editable, proc.instance, this.props.activity);
          if (this.props.containerId)
            diagram.containerId = this.props.containerId;
          this.setState({
            process: process,
            implementors: this.implementors,
            diagram: diagram
          });
          diagram.draw(this.props.animate);
        }
      });
    });
  }

  render() {
    return (
      <div className="mdw-workflow">
        <div>
          <canvas id="mdw-canvas" className="mdw-canvas"></canvas> 
        </div>
      </div>  
     );
  }
}

export default NgWorkflow;  