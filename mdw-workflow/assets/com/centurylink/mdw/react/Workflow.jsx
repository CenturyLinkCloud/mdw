import React, {Component} from '../node/node_modules/react/react';
import implementors from './implementors';
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
    this.state = { implementors: {} };
    this.drawDiagram = this.drawDiagram.bind(this);
  }
  
  componentDidMount() {    
    implementors.get(this.props.serviceBase, impls => {
      this.Shape = ShapeFactory($mdwUi.DC);
      this.Label = LabelFactory($mdwUi.DC, this.Shape);
      this.Step = StepFactory($mdwUi.DC, this.Shape);
      this.Link = LinkFactory($mdwUi.DC, this.Label);
      this.Subflow = SubflowFactory($mdwUi.DC, this.Shape, this.Step, this.Link);
      this.Note = NoteFactory($mdwUi.DC, this.Shape);
      this.Marquee = MarqueeFactory($mdwUi.DC, this.Shape);
      this.Selection = SelectionFactory();
      this.Diagram = DiagramFactory($mdwUi.DC, this.Shape, this.Label, this.Step, this.Link, this.Subflow, 
            this.Note, this.Marquee, this.Selection);

      this.setState({implementors: impls});
    });
  }

  drawDiagram() {
    var canvas = document.getElementById('mdw-canvas');
    if (canvas && this.state.implementors && this.Diagram) {
      var diagram = new (this.Diagram)(canvas, null, this.props.process, this.state.implementors, this.props.hubBase, 
            this.props.editable, this.props.instance, this.props.activity, false, this.props.data);
      if (this.props.containerId) {
        diagram.containerId = this.props.containerId;
      }
      diagram.draw(this.props.animate);
    }
  }

  // TODO: Inspector
  render() {
    this.drawDiagram();
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