import React, {Component} from '../node/node_modules/react/react';
import implementors from './implementors';
import process from './process';
import ShapeFactory from './workflow/Shape';
import LabelFactory from './workflow/Label';
import StepFactory from './workflow/Step';
import LinkFactory from './workflow/Link';
import SubflowFactory from './workflow/Subflow';
import NoteFactory from './workflow/Note';
import MarqueeFactory from './workflow/Marquee';
import SelectionFactory from './workflow/Selection';
import DiagramFactory from './workflow/Diagram';

class Workflow extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { process: {} };
  }
  
  componentDidMount() {
    process.get(this.props.serviceBase, this.props.assetPath, this.props.instanceId, this.props.masterRequestId, proc => {
      implementors.get(this.props.serviceBase, impls => {
        var canvas = document.getElementById('mdw-canvas');
        if (canvas) {
          // TODO cache these factories
          var Shape = ShapeFactory($mdwUi.DC);
          var Label = LabelFactory($mdwUi.DC, Shape);
          const Step = StepFactory($mdwUi.DC, Shape);
          const Link = LinkFactory($mdwUi.DC, Label);
          const Subflow = SubflowFactory($mdwUi.DC, Shape, Step, Link);
          const Note = NoteFactory($mdwUi.DC, Shape);
          const Marquee = MarqueeFactory($mdwUi.DC, Shape);
          const Selection = SelectionFactory();
          const Diagram = DiagramFactory($mdwUi.DC, Shape, Label, Step, Link, Subflow, Note, Marquee, Selection);

          var diagram = new Diagram(canvas, null, proc, impls, this.props.hubBase, this.props.editable, proc.instance, this.props.activity);
          if (this.props.containerId)
            diagram.containerId = this.props.containerId;
          this.setState({
            process: process,
            implementors: this.implementors,
            diagram: diagram
          }, () => diagram.draw(this.props.animate));
        }
      });
    });
  }

  // TODO: Inspector
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