import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import '../node/node_modules/style-loader!../node/node_modules/vis/dist/vis.css';
import {Network} from '../node/node_modules/vis/dist/vis';
import Heading from './Heading.jsx';
import Data from './Data.js';
import options from './options.js';

class Milestone extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { data: {} };  // for def
  }  

  isDef() {
    return location.hash.startsWith('#/milestones/definitions');
  }

  drawGraph() {
    const container = document.getElementById('milestone-graph');
    if (container) {
      const data = this.isDef() ? this.state.data : this.props.data;
      const graphOptions = Object.assign({}, { height: (data.maxDepth * 100) + 'px' }, options.graph);
      const graphData = {
        nodes: data.items, 
        edges: data.edges
      };
      const network = new Network(container, graphData, graphOptions);
    }
  }

  componentDidMount() {
    const isDef = location.hash.startsWith('#/milestones/definitions');
    if (isDef) {
      const processId = '49349280'; // TODO: use asset path
      const url = this.context.serviceRoot + '/com/centurylink/mdw/milestones/definitions/' + processId;
      fetch(new Request(url, {
        method: 'GET',
        headers: { Accept: 'application/json'},
        credentials: 'same-origin'
      }))
      .then(response => {
        return response.json();
      })
      .then(milestone => {
        this.setState({
          milestone: milestone.milestone,
          data: new Data(milestone)
        }, this.drawGraph());
      });  
    }
    else {
      this.drawGraph();
    }
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