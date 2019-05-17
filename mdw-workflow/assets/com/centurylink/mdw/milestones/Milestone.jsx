import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import '../node/node_modules/style-loader!../node/node_modules/vis/dist/vis.css';
import {Network} from '../node/node_modules/vis/dist/vis';
import Heading from './Heading.jsx';
import options from './options.js';

class Milestone extends Component {
    
  constructor(...args) {
    super(...args);
  }  

  drawGraph() {
    const container = document.getElementById('milestone-graph');
    if (container) {
      const graphOptions = Object.assign({}, {height: (this.props.data.maxDepth * 100) + 'px'}, options.graph);
      const graphData = {
        nodes: this.props.data.items, 
        edges: this.props.data.edges
      };
      const network = new Network(container, graphData, graphOptions);
    }
  }

  componentDidMount() {
    this.drawGraph();
  }

  render() {
    this.drawGraph();
    const milestone = this.props.milestone;
    return (
      <div>
        <Heading milestone={this.props.milestone} />
        <div className="mdw-section">
          <div id="milestone-graph">
          </div>
        </div>
      </div>
    );
  }
}

Milestone.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};
export default Milestone; 