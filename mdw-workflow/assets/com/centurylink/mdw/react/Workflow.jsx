import React, {Component} from '../node/node_modules/react';
import Toolbox from './Toolbox.jsx';
import Inspector from './Inspector.jsx'; // eslint-disable-line
import implementors from './implementors';
import process from './process';

class Workflow extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { process: {} };
  }
  
  componentDidMount() {
    process.get(this.props.serviceBase, this.props.assetPath, this.props.instanceId, proc => {
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
          if (this.props.editable) {
            var toolbox = Toolbox.getToolbox();
            toolbox.init(this.implementors, this.props.hubBase);
          }
        }
      });
    });
  }

  // TODO: insert Inspector and Toolbox components
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

export default Workflow;  